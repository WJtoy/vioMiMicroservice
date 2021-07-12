package com.kkl.kklplus.b2b.viomi.http.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viomi")
public class B2BVioMiProperties {

    @Getter
    @Setter
    private String appKey;

    @Getter
    @Setter
    private String appSecret;


    @Getter
    private final OkHttpProperties okhttp = new OkHttpProperties();

    public static class OkHttpProperties {
        /**
         * 设置连接超时
         */
        @Getter
        @Setter
        private Integer connectTimeout = 10;

        /**
         * 设置读超时
         */
        @Getter
        @Setter
        private Integer writeTimeout = 10;

        /**
         * 设置写超时
         */
        @Getter
        @Setter
        private Integer readTimeout = 10;

        /**
         * 是否自动重连
         */
        @Getter
        @Setter
        private Boolean retryOnConnectionFailure = true;

        /**
         * 设置ping检测网络连通性的间隔
         */
        @Getter
        @Setter
        private Integer pingInterval = 0;
    }

    /**
     * 数据源配置
     */
    @Getter
    private final DataSourceConfig dataSourceConfig = new DataSourceConfig();

    public static class DataSourceConfig {
        @Getter
        @Setter
        private String requestMainUrl;

        @Getter
        @Setter
        private String key;
    }
    @Getter
    private final ThreadPoolProperties threadPool = new ThreadPoolProperties();

    public static class ThreadPoolProperties {

        @Getter
        @Setter
        private Integer corePoolSize = 1;

        @Getter
        @Setter
        private Integer maxPoolSize = 12;

        @Getter
        @Setter
        private Integer keepAliveSeconds = 60;

        @Getter
        @Setter
        private Integer queueCapacity = 24;

    }
}
