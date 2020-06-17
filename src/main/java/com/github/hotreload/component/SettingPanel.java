package com.github.hotreload.component;

import static com.github.hotreload.utils.Constants.DEFAULT_HOST;
import static com.github.hotreload.utils.Constants.NEED_SELECT_JVM_PROCESS;
import static com.github.hotreload.utils.ReloadUtil.filterProcess;
import static com.github.hotreload.utils.ReloadUtil.getHostList;
import static com.github.hotreload.utils.ReloadUtil.getProcessList;
import static com.github.hotreload.utils.ReloadUtil.splitKeywordsText;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;

import com.github.hotreload.config.ApplicationConfig;
import com.github.hotreload.http.HttpServiceFactory;
import com.github.hotreload.model.JvmProcess;
import com.github.hotreload.utils.ReloadUtil;

/**
 * @author liuzhengyang
 */
public class SettingPanel {
    private JTextField serverField;
    private JComboBox<String> hostNameBox;
    private JTextField keywordFiled;
    private JComboBox<JvmProcess> processBox;
    private JPanel rootPanel;
    private JTextField hostNameKeywordField;

    private List<JvmProcess> currentProcessList;
    private JvmProcess selectedProcess;
    private List<String> hostList;

    public JTextField getServerField() {
        return serverField;
    }

    public JTextField getKeywordFiled() {
        return keywordFiled;
    }

    public JComboBox<JvmProcess> getProcessBox() {
        return processBox;
    }

    public JComboBox<String> getHostNameBox() {
        return hostNameBox;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public void setData(ApplicationConfig applicationConfig) {
        String serverUrl = applicationConfig.getServer();
        serverField.setText(serverUrl);
        keywordFiled.setText(applicationConfig.getKeywordText());
        hostNameKeywordField.setText(applicationConfig.getHostNameKeyword());
        if (StringUtils.isBlank(serverUrl)) {
            return;
        }
        HttpServiceFactory.setServer(serverUrl);
        selectedProcess = applicationConfig.getSelectedProcess();
        fillHostBox(serverUrl, applicationConfig.getSelectedHostName());

    }

    public void getData(ApplicationConfig applicationConfig) {
        applicationConfig.setServer(serverField.getText());
        if (processBox.getSelectedItem() != null) {
            applicationConfig.setSelectedProcess((JvmProcess) processBox.getSelectedItem());
        }
        applicationConfig.setHostNameKeyword(hostNameKeywordField.getText());
        applicationConfig.setKeywords(splitKeywordsText(keywordFiled.getText()));
        if (hostNameBox.getSelectedItem() != null) {
            applicationConfig.setSelectedHostName(hostNameBox.getSelectedItem().toString());
        }
    }

    public boolean isModifiable(ApplicationConfig applicationConfig) {
        try {
            checkArgument(Objects.equals(serverField.getText(), applicationConfig.getServer()));
            checkArgument(Objects.equals(hostNameBox.getSelectedItem(), applicationConfig.getSelectedHostName()));
            checkArgument(Objects.equals(keywordFiled.getText(), applicationConfig.getKeywordText()));
            checkArgument(Objects.equals(processBox.getSelectedItem(), applicationConfig.getSelectedProcess()));
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public void initComponent() {
        addServerFocusListener();
        addHostNameChangeListener();
        addHostNameKeywordChangeListener();
        addKeywordChangeListener();
        addHorizontalScrollForProcessBox();
    }

    private void addHorizontalScrollForProcessBox() {
        Object comp = processBox.getUI().getAccessibleChild(processBox, 0);
        if (comp instanceof JPopupMenu) {
            JPopupMenu popup = (JPopupMenu) comp;
            JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
            scrollPane.setAutoscrolls(true);
            scrollPane.setHorizontalScrollBar(new JScrollBar(JScrollBar.HORIZONTAL));
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
    }

    private void addServerFocusListener() {
        serverField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // do nothing
            }

            @Override
            public void focusLost(FocusEvent e) {
                String serverUrl = serverField.getText();
                if (StringUtils.isEmpty(serverUrl)) {
                    return;
                }
                fillHostBox(serverUrl, "");
            }
        });
    }

    private void addHostNameChangeListener() {
        hostNameBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            String hostName = e.getItem().toString();
            fillProcessBox(hostName);
        });
    }

    private void addHostNameKeywordChangeListener() {
        hostNameKeywordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changeShownHostList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changeShownHostList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changeShownHostList();
            }
        });
    }

    private void addKeywordChangeListener() {
        keywordFiled.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changeShownProcessList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changeShownProcessList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changeShownProcessList();
            }
        });
    }

    private void fillHostBox(String serverUrl, String selectedHostName) {
        if (StringUtils.isBlank(serverUrl)) {
            return;
        }
        HttpServiceFactory.setServer(serverUrl);
        List<String> hosts = getHostList();
        if (isEmpty(hosts)) {
            hosts = singletonList(DEFAULT_HOST);
        }
        hostList = hosts;
        String hostNameKeyword = hostNameKeywordField.getText();
        if (StringUtils.isNotBlank(hostNameKeyword)) {
            hosts = ReloadUtil.filterHost(hosts, hostNameKeyword);
        }
        hostNameBox.removeAllItems();
        hosts.forEach(hostNameBox::addItem);
        if (StringUtils.isBlank(selectedHostName)) {
            return;
        }
        hostNameBox.setSelectedItem(selectedHostName);
    }

    private void fillProcessBox(String hostName) {
        List<JvmProcess> processes = getProcessList(hostName);
        if (isEmpty(processes)) {
            return;
        }
        currentProcessList = processes;
        String keywordText = keywordFiled.getText();
        boolean hasKeywords = false;
        if (isNotBlank(keywordText)) {
            List<String> keywords = splitKeywordsText(keywordText);
            processes = filterProcess(processes, keywords);
            hasKeywords = true;
        }
        processBox.removeAllItems();
        if (isEmpty(processes)) {
            return;
        }
        if (hasKeywords && processes.size() > 1) {
            processes.add(0, NEED_SELECT_JVM_PROCESS);
        }
        processes.forEach(processBox::addItem);
        if (selectedProcess != null) {
            processBox.setSelectedItem(selectedProcess);
        }
        JTextField textField = (JTextField) processBox.getEditor().getEditorComponent();
        textField.setCaretPosition(0);
    }

    private void changeShownHostList() {
        String hostNameKeyWordText = hostNameKeywordField.getText();

        List<String> shownHostList = hostList;
        if (StringUtils.isNotBlank(hostNameKeyWordText)) {
            shownHostList = ReloadUtil.filterHost(shownHostList, hostNameKeyWordText);
        }
        if (shownHostList.size() > 0) {
            hostNameBox.removeAllItems();
            shownHostList.forEach(hostNameBox::addItem);
            hostNameBox.showPopup();
        } else {
            hostNameBox.removeAllItems();
            hostNameBox.hidePopup();
        }
    }

    private void changeShownProcessList() {
        if (isEmpty(currentProcessList)) {
            return;
        }
        String keywordText = keywordFiled.getText();
        List<JvmProcess> shownProcessList;
        if (StringUtils.isBlank(keywordText)) {
            shownProcessList = currentProcessList;
        } else {
            List<String> keywords = ReloadUtil.splitKeywordsText(keywordText);
            shownProcessList = ReloadUtil.filterProcess(currentProcessList, keywords);
        }
        if (shownProcessList.size() > 0) {
            processBox.removeAllItems();
            shownProcessList.forEach(processBox::addItem);
            processBox.showPopup();
        } else {
            processBox.removeAllItems();
            processBox.hidePopup();
        }
    }
}
