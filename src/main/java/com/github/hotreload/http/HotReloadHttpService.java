package com.github.hotreload.http;

import com.github.hotreload.http.model.HotReloadResult;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * @author liuzhengyang
 */
public interface HotReloadHttpService {
    @Multipart
    @POST("/hotfix")
    Call<HotReloadResult> reloadClass(@Part MultipartBody.Part file,
            @Part("targetPid") RequestBody targetPid);
}
