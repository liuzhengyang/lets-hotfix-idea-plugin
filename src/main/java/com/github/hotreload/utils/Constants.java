package com.github.hotreload.utils;

import com.github.hotreload.model.JvmProcess;

/**
 * @author zhangjikai
 * Created on 2019-03-02
 */
public class Constants {
    public static final String PROTOCOL_PREFIX = "http";
    public static final String DEFAULT_PROTOCOL = "http://";
    public static final String DEFAULT_HOST = "Default";
    public static final JvmProcess NEED_SELECT_JVM_PROCESS = new JvmProcess("", "Select process...", "");
}
