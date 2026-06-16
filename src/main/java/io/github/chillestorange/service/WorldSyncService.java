package io.github.chillestorange.service;

import io.github.chillestorange.client.ui.ClientToastHandler;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WorldSyncService {

    private static final AtomicBoolean SYNC_IN_PROGRESS = new AtomicBoolean(false);
    private static final long SYNC_TIMEOUT_MINUTES = 5;

    private WorldSyncService() {
    }

    private static Path syncExecutable() {
        return WorldSyncConfig.syncExecutableDirectory().resolve("File_accesser.exe");
    }

    public static void runSyncAsync(String threadName) {
        runSyncAsync(threadName, () -> {
        });
    }

    public static void runSyncAsync(String threadName, Runnable onSuccess) {
        if (!SYNC_IN_PROGRESS.compareAndSet(false, true)) {
            WorldSyncLogger.info("Already in progress, skipping request.");
            return;
        }

        Thread.ofVirtual().name(threadName).start(() -> {
            try {
                ClientToastHandler.showSyncStarted();
                WorldSyncLogger.info("Starting.");

                if (!synchronizeWorld()) {
                    WorldSyncLogger.error("Failed.");
                    ClientToastHandler.showMessage("Sync failed.");
                    return;
                }

                ClientToastHandler.showSyncFinished();
                WorldSyncLogger.info("Finished successfully.");

                onSuccess.run();
            } catch (Exception e) {
                WorldSyncLogger.error("Unexpected error.", e);
                ClientToastHandler.showMessage("Sync error - check logs.");
            } finally {
                SYNC_IN_PROGRESS.set(false);
            }
        });
    }

    public static boolean updateSingleplayerUuid(final Path levelDatPath, final UUID uuid) {
        try {
            CompoundTag root = NbtIo.readCompressed(levelDatPath, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompound("Data").orElseThrow(() -> new IllegalStateException("Missing Data tag"));

            data.storeNullable("singleplayer_uuid", UUIDUtil.CODEC, uuid);

            NbtIo.writeCompressed(root, levelDatPath);

            WorldSyncLogger.info("Updated singleplayer_uuid: path={}, uuid={}", levelDatPath, uuid);

            return true;
        } catch (Exception e) {
            WorldSyncLogger.error("Failed to update singleplayer_uuid: path=" + levelDatPath, e);
            return false;
        }
    }

    public static boolean synchronizeWorld() {
        Path executable = syncExecutable();
        try {
            if (!Files.isRegularFile(executable)) {
                WorldSyncLogger.error("Executable not found: path=" + executable);
                return false;
            }

            ProcessBuilder builder = new ProcessBuilder(executable.toString());
            builder.directory(WorldSyncConfig.syncExecutableDirectory().toFile());
            builder.redirectErrorStream(true);

            WorldSyncLogger.info("Launching executable: path=", executable);

            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    WorldSyncLogger.info("[EXE] " + line);
                }
            }

            boolean finished = process.waitFor(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                WorldSyncLogger.error("Timed out after " + SYNC_TIMEOUT_MINUTES + " minutes, process killed.");
                return false;
            }

            int exitCode = process.exitValue();
            WorldSyncLogger.info("Executable exited: code=", exitCode);

            return exitCode == 0;

        } catch (Exception e) {
            WorldSyncLogger.error("Failed to launch executable.", e);
            return false;
        }
    }
}