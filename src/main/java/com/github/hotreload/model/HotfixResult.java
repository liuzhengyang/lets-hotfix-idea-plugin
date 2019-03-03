package com.github.hotreload.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjikai
 * Created on 2019-03-02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotfixResult {
    private String targetClass;
}
