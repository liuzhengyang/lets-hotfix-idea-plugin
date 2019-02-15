package com.github.hotreload.config;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * @author liuzhengyang
 */
@State(name = "HotReloadPluginConfiguration", storages = {
        @Storage(value = "HotReloadPlugin")
})
public class HotReloadPluginComponent implements PersistentStateComponent<Element> {

    private static ApplicationConfig applicationConfig = new ApplicationConfig();

    @Nullable
    @Override
    public Element getState() {
        Element root = new Element("root");
        Element hotReloadNode = new Element("hotReload");
        hotReloadNode.setAttribute("server", applicationConfig.getServer());
        hotReloadNode.setAttribute("pid", applicationConfig.getPid());
        root.addContent(hotReloadNode);
        return root;
    }

    @Override
    public void loadState(@NotNull Element state) {
        Element hotReloadNode = state.getChild("hotReload");
        if (hotReloadNode != null) {
            String server = hotReloadNode.getAttributeValue("server");
            if (server != null) {
                applicationConfig.setServer(server);
            }
            String pid = hotReloadNode.getAttributeValue("pid");
            if (pid != null) {
                applicationConfig.setServer(pid);
            }
        }
    }

    public static ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }
}
