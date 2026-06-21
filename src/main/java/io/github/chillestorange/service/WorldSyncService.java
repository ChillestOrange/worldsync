package io.github.chillestorange.service;

import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.cloud.CloudItem;
import io.github.chillestorange.service.cloud.CloudStorageFactory;
import io.github.chillestorange.service.cloud.CloudStorageFactory.Credentials;
import io.github.chillestorange.service.cloud.CloudStorageFactory.ProviderType;
import io.github.chillestorange.service.cloud.CloudStorageProvider;
import io.github.chillestorange.service.sync.*;
import io.github.chillestorange.service.sync.SyncDiffEngine.FolderTask;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * This is the call site that replaces:
 * <p>
 * new ProcessBuilder(file_accesser.exe path...).start();
 * <p>
 * with:
 * <p>
 * WorldSyncService.initialize(...);          // once, at mod startup
 * WorldSyncService.runSyncCycle(worldPath);   // every trigger after that
 * <p>
 * wired into your AutosaveSyncListener / WorldSaveMixin / WorldJoinMixin
 * wherever the process used to get launched.
 */
public final class WorldSyncService {

    // Replaces lock.py's PID-file lock entirely. That existed because
    // file_accesser.exe could be launched as a brand-new OS process every
    // cycle; here everything runs in one JVM, so a flag is enough to stop two
    // sync cycles overlapping (e.g. an autosave-triggered sync racing a
    // world-join-triggered one).
    private static final AtomicBoolean SYNC_RUNNING = new AtomicBoolean(false);
    private static final ExecutorService SYNC_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "worldsync-cycle");
        t.setDaemon(true);
        return t;
    });
    private static volatile CloudStorageProvider provider;
    private static volatile HashCache hashCache;
    private static volatile String remoteFolderId;
    private static volatile Path configDir;
    private WorldSyncService() {
    }

    public static boolean isSyncing() {
        return SYNC_RUNNING.get();
    }

    /**
     * Call once at mod startup (e.g. from WorldSyncClient's initializer), not
     * on every sync cycle. Building the provider, authenticator, and hash
     * cache fresh every cycle was forced when this was a freshly-launched
     * process each time; now that it's one long-lived JVM, doing that every
     * cycle would just mean re-reading token/hash-cache JSON off disk and
     * spinning up duplicate HttpClient instances for no reason.
     */
    public static void initialize(
            ProviderType providerType, Credentials credentials, String remoteFolderId, Path configDir
    ) {
        HttpClient sharedHttpClient = HttpClient.newHttpClient();
        provider = CloudStorageFactory.create(providerType, credentials, sharedHttpClient);
        hashCache = new HashCache(configDir.resolve("sync_hash_cache.json"), provider::computeLocalFingerprint);
        WorldSyncService.remoteFolderId = remoteFolderId;
        WorldSyncService.configDir = configDir;
    }

    /**
     * Runs one full sync cycle for the given world. Hops onto a background
     * thread internally and never blocks the calling thread — safe to call
     * directly from a mixin callback on the client thread.
     */
    public static CompletableFuture<Void> runSyncCycle(Path worldPath) {
        return runSyncCycle(worldPath, () -> {}, error -> {});
    }

    public static CompletableFuture<Void> runSyncCycle(
            Path worldPath, Runnable onSuccess, Consumer<Throwable> onFailure) {

        if (provider == null) {
            throw new IllegalStateException("WorldSyncService.initialize(...) must be called before runSyncCycle(...)");
        }
        if (!SYNC_RUNNING.compareAndSet(false, true)) {
            WorldSyncLogger.info("Sync already running, skipping this trigger");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                doSync(worldPath);
                onSuccess.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                WorldSyncLogger.error("Sync cycle interrupted", e);
                onFailure.accept(e);
            } catch (Exception e) {
                WorldSyncLogger.error("Sync cycle failed", e);
                onFailure.accept(e);
            } finally {
                SYNC_RUNNING.set(false);
            }
        }, SYNC_EXECUTOR);
    }

    private static void doSync(Path worldPath) throws IOException, InterruptedException {
        // Check for level.dat specifically, not just the directory. On a world-join
        // trigger, Minecraft creates the world directory before writing level.dat,
        // so Files.exists(worldPath) can return true while level.dat doesn't exist
        // yet — which caused a NoSuchFileException when the old check just tested
        // the directory.
        boolean firstRun = !Files.exists(worldPath.resolve("level.dat"));
        SyncDirection direction;

        if (firstRun) {
            // The original had a real bug here: it called orchestrator.sync(direction="-1")
            // with a comment saying "force download everything", but "-1" is the no-op
            // direction in that codebase, so sync() returned immediately and first-time
            // download never actually happened. Fixed here by setting DOWNLOAD directly.
            WorldSyncLogger.info("World not found locally — downloading from cloud storage");
            Files.createDirectories(worldPath);
            direction = SyncDirection.DOWNLOAD;
        } else {
            Path remoteLevelDat = configDir.resolve("remote_files/level.dat");
            CloudItem remoteLevelDatItem = provider.findByNameInFolder("level.dat", remoteFolderId)
                    .orElseThrow(() -> new IOException("level.dat not found remotely in folder " + remoteFolderId));
            provider.downloadFile(remoteLevelDatItem.id(), remoteLevelDat);

            LevelSync.Summary local = LevelSync.read(worldPath.resolve("level.dat"));
            LevelSync.Summary remote = LevelSync.read(remoteLevelDat);

            WorldSyncLogger.debug("Level.dat comparison: local ticks={} remote ticks={}", local.time(), remote.time());

            direction = LevelSync.compare(local, remote);
        }

        if (direction == SyncDirection.NO_OP) {
            WorldSyncLogger.info("Worlds already in sync, nothing to do");
            return;
        }

        WorldSyncLogger.info("Starting sync — direction: " + direction);

        Map<String, List<CloudItem>> tree = provider.fetchTree(remoteFolderId);
        WorldSyncLogger.info("Remote tree fetched: " + tree.size() + " folders mapped");

        SyncDiffEngine diffEngine = new SyncDiffEngine();
        SyncDiffEngine.Result diff = diffEngine.buildChangeset(
                worldPath, remoteFolderId, tree, hashCache, direction);

        WorldSyncLogger.info(diff.toUpload().size() + " uploads, " + diff.toDownload().size()
                + " downloads, " + diff.folderTasks().size() + " folder(s) to create");

        // Folder creation happens synchronously here, before the transfer pool
        // starts — two threads racing to create the same folder on either side
        // causes intermittent errors.
        for (FolderTask task : diff.folderTasks()) {
            if (task instanceof FolderTask.CreateLocal(Path path)) {
                Files.createDirectories(path);
            } else if (task instanceof FolderTask.CreateRemote(Path localPath, String parentFolderId, String name)) {
                String newFolderId = provider.createFolder(parentFolderId, name,
                        Files.getLastModifiedTime(localPath).toInstant());

                // The diff couldn't see inside this folder while building the
                // changeset, since it didn't exist remotely yet. Now that it has a
                // real id, walk its local contents directly so anything inside
                // gets uploaded this same cycle instead of waiting one cycle late.
                diffEngine.discoverNewLocalFolderContents(provider, localPath, newFolderId, diff.toUpload());
            }
        }

        new FileTransferManager(provider).runTransfers(diff.toUpload(), diff.toDownload());

        hashCache.save();
        WorldSyncLogger.info("Sync cycle complete");
    }
}