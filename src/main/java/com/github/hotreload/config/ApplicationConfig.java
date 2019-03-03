package com.github.hotreload.config;

import static com.github.hotreload.utils.ReloadUtil.joinKeywords;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;

import com.github.hotreload.model.JvmProcess;

import lombok.Data;

/**
 * @author liuzhengyang
 */
@Data
public class ApplicationConfig {
    private String server;
    private String selectedHostName;
    private List<String> keywords;
    private JvmProcess selectedProcess;

    public String getKeywordText() {
        if (isEmpty(keywords)) {
            return "";
        }
        return joinKeywords(keywords);
    }
}
