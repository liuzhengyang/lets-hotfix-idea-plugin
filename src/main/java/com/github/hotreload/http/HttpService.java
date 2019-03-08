package com.github.hotreload.http;

import java.util.List;

import com.github.hotreload.model.HotfixResult;
import com.github.hotreload.model.JvmProcess;
import com.github.hotreload.model.Result;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * @author liuzhengyang
 */
public interface HttpService {
    @Multipart
    @POST("/hotfix")
    Call<Result<HotfixResult>> reloadClass(@Part MultipartBody.Part file,
            @Part("targetPid") RequestBody targetPid, @Part("proxyServer") RequestBody hostName);

    @GET("/processList")
    Call<Result<List<JvmProcess>>> processList(@Query("proxyServer") String hostName);

    @GET("/hostList")
    Call<Result<List<String>>> hostList();

}
