package com.kkl.kklplus.b2b.viomi.http.config;

import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.TimeUnit;

@EnableConfigurationProperties({B2BVioMiProperties.class})
@Configuration
public class OKHttpConfig {

    @Bean
    public OkHttpClient okHttpClient(B2BVioMiProperties vioMiProperties) {
        return new OkHttpClient().newBuilder()
                .connectTimeout(vioMiProperties.getOkhttp().getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(vioMiProperties.getOkhttp().getWriteTimeout(), TimeUnit.SECONDS)
                .readTimeout(vioMiProperties.getOkhttp().getReadTimeout(), TimeUnit.SECONDS)
                .pingInterval(vioMiProperties.getOkhttp().getPingInterval(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(vioMiProperties.getOkhttp().getRetryOnConnectionFailure())
                .build();
    }
}
