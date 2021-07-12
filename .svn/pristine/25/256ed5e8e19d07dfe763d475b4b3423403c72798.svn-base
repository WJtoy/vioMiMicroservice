package com.kkl.kklplus.b2b.viomi.feign;

import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 为所有的Feign请求设置Header
 */
@Component
public class FeignClientConfig {

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Bean
    public RequestInterceptor headerInterceptor() {
        return template -> {
            template.header("appKey", vioMiProperties.getAppKey());
            template.header("appSecret", vioMiProperties.getAppSecret());
        };
    }

}
