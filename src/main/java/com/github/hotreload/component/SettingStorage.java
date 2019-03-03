package com.github.hotreload.component;

import static com.alibaba.fastjson.JSON.parseArray;
import static com.alibaba.fastjson.JSON.parseObject;
import static com.alibaba.fastjson.JSON.toJSONString;
import static java.util.Optional.ofNullable;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.hotreload.config.ApplicationConfig;
import com.github.hotreload.model.JvmProcess;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * @author liuzhengyang
 */
@State(name = "HotReloadPluginConfiguration", storages = {@Storage(value = "HotReloadPlugin")})
public class SettingStorage implements PersistentStateComponent<Element> {

    private static ApplicationConfig applicationConfig = new ApplicationConfig();

    @Nullable
    @Override
    public Element getState() {
        Element root = new Element("root");
        Element hotReloadNode = new Element("hotReload");
        hotReloadNode.setAttribute("server", applicationConfig.getServer());
        hotReloadNode.setAttribute("hostName", applicationConfig.getSelectedHostName());
        hotReloadNode.setAttribute("keywords", toJSONString(applicationConfig.getKeywords()));
        hotReloadNode.setAttribute("process", toJSONString(applicationConfig.getSelectedProcess()));
        root.addContent(hotReloadNode);
        return root;
    }

    @Override
    public void loadState(@NotNull Element state) {
        Element hotReloadNode = state.getChild("hotReload");
        if (hotReloadNode == null) {
            return;
        }
        String server = hotReloadNode.getAttributeValue("server");
        ofNullable(server).ifPresent(applicationConfig::setServer);
        String hostName = hotReloadNode.getAttributeValue("hostName");
        ofNullable(hostName).ifPresent(applicationConfig::setSelectedHostName);
        String keywordsText = hotReloadNode.getAttributeValue("keywords");
        ofNullable(keywordsText).ifPresent(text -> applicationConfig.setKeywords(parseArray(text, String.class)));
        String processText = hotReloadNode.getAttributeValue("process");
        ofNullable(processText).ifPresent(text ->
                applicationConfig.setSelectedProcess(parseObject(text, JvmProcess.class)));
    }

    public static ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }
}
