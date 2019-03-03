package com.github.hotreload.config;

import javax.swing.JComponent;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import com.github.hotreload.component.SettingPanel;
import com.github.hotreload.component.SettingStorage;
import com.intellij.openapi.options.Configurable;

/**
 * @author liuzhengyang
 */
public class PluginConfig implements Configurable {

    private SettingPanel hotReloadConfiguration;
    private ApplicationConfig applicationConfig;

    public PluginConfig() {
        this.applicationConfig = SettingStorage.getApplicationConfig();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "HotReloadConfig";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (hotReloadConfiguration == null) {
            hotReloadConfiguration = new SettingPanel();
            hotReloadConfiguration.initComponent();
        }
        return hotReloadConfiguration.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return hotReloadConfiguration != null && hotReloadConfiguration.isModifiable(applicationConfig);
    }

    @Override
    public void apply() {
        if (hotReloadConfiguration != null) {
            hotReloadConfiguration.getData(applicationConfig);
        }
    }

    @Override
    public void reset() {
        if (hotReloadConfiguration != null) {
            hotReloadConfiguration.setData(applicationConfig);
        }
    }
}
