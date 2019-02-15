package com.github.hotreload.config;

import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author liuzhengyang
 */
public class HotReloadConfiguration {
    private JTextField server;
    private JTextField pid;
    private JPanel rootPanel;

    public JTextField getServer() {
        return server;
    }

    public JTextField getPid() {
        return pid;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public void setData(ApplicationConfig applicationConfig) {
        server.setText(applicationConfig.getServer());
        pid.setText(applicationConfig.getPid());
    }

    public void getData(ApplicationConfig applicationConfig) {
        applicationConfig.setServer(server.getText());
        applicationConfig.setPid(pid.getText());
    }

    public boolean isModifiable(ApplicationConfig applicationConfig) {
        if (!Objects.equals(server.getText(), applicationConfig.getServer())) {
            return true;
        }
        if (!Objects.equals(pid.getText(), applicationConfig.getPid())) {
            return true;
        }
        return false;
    }

}
