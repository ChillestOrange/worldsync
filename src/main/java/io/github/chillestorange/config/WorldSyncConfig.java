package io.github.chillestorange.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.chillestorange.WorldSync;
import io.github.chillestorange.service.cloud.CloudStorageFactory.Credentials;
import io.github.chillestorange.service.cloud.CloudStorageFactory.ProviderType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class WorldSyncConfig {

    public static final ConfigClassHandler<WorldSyncConfig> HANDLER =
            ConfigClassHandler.createBuilder(WorldSyncConfig.class)
                    .id(Identifier.fromNamespaceAndPath(
                            WorldSync.MOD_ID,
                            "config"
                    ))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(
                                    FabricLoader.getInstance()
                                            .getConfigDir()
                                            .resolve(WorldSync.MOD_ID + ".json5")
                            )
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                            .setJson5(true)
                            .build())
                    .build();

    @SerialEntry(comment = "Name of the world to be synced.")
    public String targetWorld = "";

    @SerialEntry(comment = "[ Experimental ] Whether syncing should also run after each autosave.")
    public boolean autosaveSyncEnabled = false;

    @SerialEntry(comment = "[ Experimental ] Interval in ticks between autosave-triggered syncs. Default 6000 = 5 minutes.")
    public int autosaveIntervalTicks = 6000;

    @SerialEntry(comment = "Cloud provider to use for syncing. Currently only GOOGLE_DRIVE is supported.")
    public String cloudProvider = "GOOGLE_DRIVE";

    @SerialEntry(comment = "Google Drive folder ID to sync the world with. " +
            "Find it in your Drive URL: drive.google.com/drive/folders/<THIS_PART>")
    public String remoteFolderId = "";

    @SerialEntry(comment = "OAuth Client ID from your Google Cloud 'Desktop app' credential " +
            "(APIs & Services > Credentials). Required for Drive access.")
    public String clientId = "";

    @SerialEntry(comment = "OAuth Client Secret from the same Google Cloud credential. " +
            "For an installed-app OAuth flow this is not a true secret (Google's own docs " +
            "acknowledge this), but avoid sharing your config file publicly.")
    public String clientSecret = "";

    @SerialEntry(comment = "[ Advanced ] Number of files after which threaded execution is used.")
    public int threadThreshold = 5;

    @SerialEntry(comment = "[ Advanced ] Number of simultaneous file transfers during threaded execution.")
    public int maxWorkers = 12;

    @SerialEntry(comment = "[ Advanced ] Number of attempts per file before giving up.")
    public int maxRetries = 3;

    @SerialEntry(comment = "[ Advanced ] Delay between each retry (in ms).")
    public long retryDelay = 1500;

    // Accessors.
    public static String targetWorld() {
        return HANDLER.instance().targetWorld;
    }

    public static boolean autosaveSyncEnabled() {
        return HANDLER.instance().autosaveSyncEnabled;
    }

    public static int autosaveIntervalTicks() {
        return HANDLER.instance().autosaveIntervalTicks;
    }

    /**
     * Converts the cloudProvider string from config into a typed ProviderType.
     * Throws IllegalArgumentException with a clear message if the value in the
     * JSON doesn't match any known provider, rather than silently NPE-ing later
     * inside a sync cycle.
     */
    public static ProviderType providerType() {
        String raw = HANDLER.instance().cloudProvider.trim().toUpperCase();
        try {
            return ProviderType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown cloudProvider in worldsync.json5: '" + raw +
                            "'. Valid values: GOOGLE_DRIVE", e);
        }
    }

    public static String remoteFolderId() {
        return HANDLER.instance().remoteFolderId;
    }

    /**
     * Builds the credential object WorldSyncService.initialize() needs.
     * tokenStorePath is derived from configDir() so it doesn't need its
     * own config field — the user never needs to know where tokens live.
     */
    public static Credentials credentials() {
        return new Credentials.OAuthCredentials(
                HANDLER.instance().clientId,
                HANDLER.instance().clientSecret,
                configDir().resolve("drive_tokens.json")
        );
    }

    /**
     * Dedicated subdirectory under Fabric's config dir for WorldSync runtime
     * files (OAuth tokens, hash cache). Kept separate from worldsync.json5
     * itself so these files don't appear alongside user-edited config.
     */
    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(WorldSync.MOD_ID);
    }

    public static int threadThreshold() {
        return HANDLER.instance().threadThreshold;
    }

    public static int maxWorkers() {
        return HANDLER.instance().maxWorkers;
    }

    public static int maxRetries() {
        return HANDLER.instance().maxRetries;
    }

    public static long retryDelay() {
        return HANDLER.instance().retryDelay;
    }
}