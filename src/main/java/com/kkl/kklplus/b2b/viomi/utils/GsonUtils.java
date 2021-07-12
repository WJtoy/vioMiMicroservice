package com.kkl.kklplus.b2b.viomi.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

/**
 * Gson字符串工具类
 * @autor Ryan Lu
 * @date 2018/12/25 4:11 PM
 */
@Slf4j
public class GsonUtils {

    private static final Gson underscoresGson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)//由于js精度不够(2的53次方)，返回json时将Long转成字符
            .create();
    private static final Gson gson = new GsonBuilder()
            //.addSerializationExclusionStrategy(new GsonIgnoreStrategy())
            //序列化null
            //.serializeNulls()
            //null <-> String
            //.registerTypeAdapter(String.class, new StringConverter())
            //.registerTypeAdapter(CdrDownloadRequestBody.class, new CdrDownloadRequestBodyAdapter()) //自定义Json序列化/返序列化类
            //禁止转义html标签
            .disableHtmlEscaping()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            //.excludeFieldsWithoutExposeAnnotation() // <---
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)//由于js精度不够(2的53次方)，返回json时将Long转成字符
            .create();
    private static GsonUtils gsonUtils;

    public GsonUtils() {}

    /**
     * 创建只输出非Null且非Empty(如List.isEmpty)的属性到Json字符串的Mapper,建议在外部接口中使用.
     */
    public static GsonUtils getInstance() {
        if (gsonUtils == null){
            gsonUtils = new GsonUtils();
        }
        return gsonUtils;
    }

    public Gson getGson(){
        return gson;
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。
     * 如果对象为Null, 返回"null".
     * 如果集合为空集合, 返回"[]".
     */
    public String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * JSON转对象
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json,clazz);
    }

    public <T> T fromJson(String json, Type typeOfT ) {
        return underscoresGson.fromJson(json,typeOfT);
    }
    /**
     * JSON转对象[识别下划线转化为驼峰]
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T fromUnderscoresJson(String json, Class<T> clazz) {
        return underscoresGson.fromJson(json,clazz);
    }

    public <T> T fromUnderscoresJson(String json, Type typeOfT ) {
        return underscoresGson.fromJson(json,typeOfT);
    }
    /**
     * [驼峰转化为下划线JSON]
     * Object可以是POJO，也可以是Collection或数组。
     * 如果对象为Null, 返回"null".
     * 如果集合为空集合, 返回"[]".
     */
    public String toUnderscoresJson(Object object) {
        return underscoresGson.toJson(object);
    }

}
