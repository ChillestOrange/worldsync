package io.github.chillestorange.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import io.github.chillestorange.logging.GameSyncLogger;
import io.github.chillestorange.service.cloud.CloudStorageFactory.ProviderType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GameSyncConfigScreen {

    private GameSyncConfigScreen() {
    }

    public static Screen createScreen(Screen parent) {
        GameSyncConfig instance = GameSyncConfig.HANDLER.instance();

        // --- General: what to sync, and when ---

        Option<String> targetWorldOption = Option.<String>createBuilder()
                .name(Component.literal("Target World"))
                .description(OptionDescription.of(Component.literal(
                        "Name of the world to be synced.")))
                .binding("", () -> instance.targetWorld, v -> instance.targetWorld = v)
                .controller(StringControllerBuilder::create)
                .build();

        Option<ProviderType> cloudProviderOption = Option.<ProviderType>createBuilder()
                .name(Component.literal("Cloud Provider"))
                .description(OptionDescription.of(Component.literal(
                        "The cloud service GameSync stores your backup with. Only Google Drive is supported right now.")))
                .binding(
                        ProviderType.GOOGLE_DRIVE,
                        () -> {
                            try {
                                return GameSyncConfig.providerType();
                            } catch (IllegalArgumentException e) {
                                return ProviderType.GOOGLE_DRIVE;
                            }
                        },
                        v -> instance.cloudProvider = v.name()
                )
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(ProviderType.class))
                .build();

        OptionGroup syncTargetGroup = OptionGroup.createBuilder()
                .name(Component.literal("Sync Target"))
                .option(targetWorldOption)
                .option(cloudProviderOption)
                .build();

        Option<Boolean> autosaveEnabledOption = Option.<Boolean>createBuilder()
                .name(Component.literal("Sync on Autosave"))
                .description(OptionDescription.of(Component.literal(
                        "Whether syncing should also run after each autosave, not just on manual sync or world unload.")))
                .binding(false, () -> instance.autosaveSyncEnabled, v -> instance.autosaveSyncEnabled = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<Integer> autosaveIntervalOption = Option.<Integer>createBuilder()
                .name(Component.literal("Autosave Sync Interval"))
                .description(OptionDescription.of(Component.literal(
                        "Interval in ticks between autosave-triggered syncs. 6000 ticks = 5 minutes.")))
                .binding(6000, () -> instance.autosaveIntervalTicks, v -> instance.autosaveIntervalTicks = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(1200, 72000)
                        .step(1200)
                        .formatValue(v -> Component.literal((v / 1200) + " min")))
                .build();

        OptionGroup autosaveGroup = OptionGroup.createBuilder()
                .name(Component.literal("Autosave (Experimental)"))
                .description(OptionDescription.of(Component.literal(
                        "These settings are experimental and may change in a future update.")))
                .option(autosaveEnabledOption)
                .option(autosaveIntervalOption)
                .build();

        ConfigCategory generalCategory = ConfigCategory.createBuilder()
                .name(Component.literal("General"))
                .group(syncTargetGroup)
                .group(autosaveGroup)
                .build();

        // --- Google Drive: where the world goes and how GameSync authenticates ---

        Option<String> remoteFolderIdOption = Option.<String>createBuilder()
                .name(Component.literal("Remote Folder ID"))
                .description(OptionDescription.of(Component.literal(
                        "Google Drive folder ID to sync the world with. Find it in your Drive URL: " +
                                "drive.google.com/drive/folders/<THIS_PART>")))
                .binding("", () -> instance.remoteFolderId, v -> instance.remoteFolderId = v)
                .controller(StringControllerBuilder::create)
                .build();

        Option<String> clientIdOption = Option.<String>createBuilder()
                .name(Component.literal("OAuth Client ID"))
                .description(OptionDescription.of(Component.literal(
                        "From your Google Cloud 'Desktop app' credential under APIs & Services > Credentials.")))
                .binding("", () -> instance.clientId, v -> instance.clientId = v)
                .controller(StringControllerBuilder::create)
                .build();

        Option<String> clientSecretOption = Option.<String>createBuilder()
                .name(Component.literal("OAuth Client Secret"))
                .description(OptionDescription.of(Component.literal(
                        "From the same Google Cloud credential. For an installed-app OAuth flow this isn't a " +
                                "true secret, but still avoid sharing your config file publicly.")))
                .binding("", () -> instance.clientSecret, v -> instance.clientSecret = v)
                .controller(StringControllerBuilder::create)
                .build();

        OptionGroup driveConnectionGroup = OptionGroup.createBuilder()
                .name(Component.literal("Connection"))
                .description(OptionDescription.of(Component.literal(
                        "Create an OAuth 'Desktop app' credential in the Google Cloud Console, then paste its " +
                                "details here.")))
                .option(remoteFolderIdOption)
                .option(clientIdOption)
                .option(clientSecretOption)
                .build();

        ConfigCategory driveCategory = ConfigCategory.createBuilder()
                .name(Component.literal("Google Drive"))
                .group(driveConnectionGroup)
                .build();

        // --- Advanced: performance and reliability tuning ---

        Option<Integer> threadThresholdOption = Option.<Integer>createBuilder()
                .name(Component.literal("Thread Threshold"))
                .description(OptionDescription.of(Component.literal(
                        "Number of files after which threaded execution is used.")))
                .binding(5, () -> instance.threadThreshold, v -> instance.threadThreshold = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 50).step(1))
                .build();

        Option<Integer> maxWorkersOption = Option.<Integer>createBuilder()
                .name(Component.literal("Max Workers"))
                .description(OptionDescription.of(Component.literal(
                        "Number of simultaneous file transfers during threaded execution.")))
                .binding(12, () -> instance.maxWorkers, v -> instance.maxWorkers = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 32).step(1))
                .build();

        OptionGroup performanceGroup = OptionGroup.createBuilder()
                .name(Component.literal("Performance"))
                .option(threadThresholdOption)
                .option(maxWorkersOption)
                .build();

        Option<Integer> maxRetriesOption = Option.<Integer>createBuilder()
                .name(Component.literal("Max Retries"))
                .description(OptionDescription.of(Component.literal(
                        "Number of attempts per file before giving up.")))
                .binding(3, () -> instance.maxRetries, v -> instance.maxRetries = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 10).step(1))
                .build();

        Option<Integer> retryDelayOption = Option.<Integer>createBuilder()
                .name(Component.literal("Retry Delay (ms)"))
                .description(OptionDescription.of(Component.literal(
                        "Delay between each retry attempt, in milliseconds.")))
                .binding(1500, () -> (int) instance.retryDelay, v -> instance.retryDelay = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 30000).step(500))
                .build();

        OptionGroup reliabilityGroup = OptionGroup.createBuilder()
                .name(Component.literal("Reliability"))
                .option(maxRetriesOption)
                .option(retryDelayOption)
                .build();

        Option<Boolean> debugModeOption = Option.<Boolean>createBuilder()
                .name(Component.literal("Debug Mode"))
                .description(OptionDescription.of(Component.literal(
                        "Verbose logging for troubleshooting.")))
                .binding(false, () -> instance.debugMode, v -> {
                    instance.debugMode = v;
                    GameSyncLogger.setDebugEnabled(v);
                })
                .controller(BooleanControllerBuilder::create)
                .build();

        OptionGroup debugGroup = OptionGroup.createBuilder()
                .name(Component.literal("Debug"))
                .option(debugModeOption)
                .build();

        ConfigCategory advancedCategory = ConfigCategory.createBuilder()
                .name(Component.literal("Advanced"))
                .group(performanceGroup)
                .group(reliabilityGroup)
                .group(debugGroup)
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("GameSync"))
                .category(generalCategory)
                .category(driveCategory)
                .category(advancedCategory)
                .save(GameSyncConfig.HANDLER::save)
                .build()
                .generateScreen(parent);
    }
}
