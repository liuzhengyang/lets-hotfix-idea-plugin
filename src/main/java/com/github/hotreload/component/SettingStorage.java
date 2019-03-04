package com.github.hotreload.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.hotreload.config.ApplicationConfig;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * @author liuzhengyang
 */
@State(name = "HotReloadPluginConfiguration", storages = {@Storage("hotReloadPlugin.xml")})
public class SettingStorage implements PersistentStateComponent<ApplicationConfig> {

    private static ApplicationConfig applicationConfig = new ApplicationConfig();

    @Nullable
    @Override
    public ApplicationConfig getState() {
        return applicationConfig;
    }

    @Override
    public void loadState(@NotNull ApplicationConfig state) {
        applicationConfig = state;
    }

    public static ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }
}
