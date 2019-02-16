package com.github.hotreload.config;

import javax.swing.JComponent;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

/**
 * @author liuzhengyang
 */
public class HotReloadPluginConfig implements Configurable {

    private HotReloadConfiguration hotReloadConfiguration;
    private ApplicationConfig applicationConfig;

    public HotReloadPluginConfig() {
        this.applicationConfig = HotReloadPluginComponent.getApplicationConfig();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "hotReloadConfig";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (hotReloadConfiguration == null) {
            hotReloadConfiguration = new HotReloadConfiguration();
        }
        return hotReloadConfiguration.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return hotReloadConfiguration != null && hotReloadConfiguration.isModifiable(applicationConfig);
    }

    @Override
    public void apply() throws ConfigurationException {
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
