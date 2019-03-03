package com.github.hotreload.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjikai
 * Created on 2019-02-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    public static final int SUCCESS_CODE = 0;

    private int code;
    private String msg;
    private T data;
}
