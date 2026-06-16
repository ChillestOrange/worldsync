package io.github.chillestorange.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class WorldSyncConfig {

    public static final ConfigClassHandler<WorldSyncConfig> HANDLER =
            ConfigClassHandler.createBuilder(WorldSyncConfig.class)
                    .id(Identifier.fromNamespaceAndPath(
                            "worldsync",
                            "config"
                    ))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(
                                    FabricLoader.getInstance()
                                            .getConfigDir()
                                            .resolve("worldsync.json5")
                            )
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                            .setJson5(true)
                            .build())
                    .build();

    @SerialEntry(comment = "Path to the directory where File_accesser.exe is at.")
    public String syncExecutableDirectory = "";

    @SerialEntry(comment = "Name of the world to be synced.")
    public String targetWorld = "";

    @SerialEntry(comment = "Whether autosave should be enabled. (WARNING: uses more internet) (experimental)")
    public boolean autosaveSyncEnabled = false;

    @SerialEntry(comment = "Interval in ticks between autosaves.")
    public int autosaveIntervalTicks = 6000;

    public static Path syncExecutableDirectory() {
        return Path.of(HANDLER.instance().syncExecutableDirectory);
    }

    public static String targetWorld() {
        return HANDLER.instance().targetWorld;
    }

    public static boolean autosaveSyncEnabled() {
        return HANDLER.instance().autosaveSyncEnabled;
    }

    public static int autosaveIntervalTicks() {
        return HANDLER.instance().autosaveIntervalTicks;
    }
}