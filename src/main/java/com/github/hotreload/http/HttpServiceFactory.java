package com.github.hotreload.http;

import static com.github.hotreload.utils.Constants.DEFAULT_PROTOCOL;
import static com.github.hotreload.utils.Constants.PROTOCOL_PREFIX;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author zhangjikai
 * Created on 2019-02-24
 */
public class HttpServiceFactory {

    private static HttpService httpService;

    public static HttpService getInstance() {
        checkNotNull(httpService);
        return httpService;
    }

    /**
     * Init httpService with serverUrl if httpService is null.
     */
    public static void trySetServer(String serverUrl) {
        if (httpService == null) {
            setServer(serverUrl);
        }
    }

    public static void setServer(String serverUrl) {
        checkNotNull(serverUrl);
        if (!hasProtocol(serverUrl)) {
            serverUrl = DEFAULT_PROTOCOL + serverUrl;
        }
        // todo: 如果域名不合法，DNS查找时间较长，后面看看怎么优化一下
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        httpService = retrofit.create(HttpService.class);
    }

    private static boolean hasProtocol(String url) {
        return url.startsWith(PROTOCOL_PREFIX);
    }
}
