package com.kkl.kklplus.b2b.viomi.http.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.http.request.CommonParam;
import com.kkl.kklplus.b2b.viomi.http.response.FaultType;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.utils.SpringContextHolder;
import okhttp3.*;

import java.util.ArrayList;
import java.util.List;

public class OkHttpUtils {

    private static final MediaType CONTENT_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient okHttpClient = SpringContextHolder.getBean(OkHttpClient.class);

    private static B2BVioMiProperties vioMiProperties = SpringContextHolder.getBean(B2BVioMiProperties.class);

    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();;

    /**
     * 调用云米接口(POST)
     * @param command
     * @param dataClass
     * @param <T>
     * @return
     */
    public static <T> ResponseBody<T> postSyncGenericNew(OperationCommand command, Class<T> dataClass) {
        ResponseBody<T> responseBody = null;
        B2BVioMiProperties.DataSourceConfig dataSourceConfig = vioMiProperties.getDataSourceConfig();
        if (dataSourceConfig != null && command != null && command.getOpCode() != null &&
                command.getReqBody() != null && command.getReqBody().getClass().getName().equals(command.getOpCode().reqBodyClass.getName())) {
            String url = dataSourceConfig.getRequestMainUrl().concat("/").concat(command.getOpCode().apiUrl);
            CommonParam commonParam = new CommonParam();
            commonParam.setKey(dataSourceConfig.getKey());
            commonParam.setData(command.getReqBody());
            String reqbodyJson = gson.toJson(commonParam);
            RequestBody requestBody = RequestBody.create(CONTENT_TYPE_JSON, reqbodyJson);   //请求体参数
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Call call = okHttpClient.newCall(request);
            try {
                Response response = call.execute();
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        String responseBodyJson = response.body().string();
                        try {
                            responseBody = gson.fromJson(responseBodyJson, new TypeToken<ResponseBody>() {
                            }.getType());
                            responseBody.setOriginalJson(responseBodyJson);
                            try {
                                T data = gson.fromJson(responseBodyJson, dataClass);
                                responseBody.setData(data);
                            } catch (Exception e) {
                                return new ResponseBody<>(ResponseBody.ErrorCode.DATA_PARSE_FAILURE, e);
                            }
                        } catch (Exception e) {
                            responseBody = new ResponseBody<>(ResponseBody.ErrorCode.JSON_PARSE_FAILURE, e);
                            responseBody.setOriginalJson(responseBodyJson);
                            return responseBody;
                        }
                    } else {
                        responseBody = new ResponseBody<>(ResponseBody.ErrorCode.HTTP_RESPONSE_BODY_ERROR);
                    }
                } else {
                    responseBody = new ResponseBody<>(ResponseBody.ErrorCode.HTTP_STATUS_CODE_ERROR);
                }
            } catch (Exception e) {
                return new ResponseBody<>(ResponseBody.ErrorCode.REQUEST_INVOCATION_FAILURE, e);
            }
        } else {
            responseBody = new ResponseBody<>(ResponseBody.ErrorCode.REQUEST_PARAMETER_FORMAT_ERROR);
        }
        return responseBody;
    }
}
