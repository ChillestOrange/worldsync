package io.github.chillestorange.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.chillestorange.config.GameSyncConfigScreen;

public class GameSyncModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GameSyncConfigScreen::createScreen;
    }
}
