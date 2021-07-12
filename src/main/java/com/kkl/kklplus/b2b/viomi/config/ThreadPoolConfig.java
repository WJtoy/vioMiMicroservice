package com.kkl.kklplus.b2b.viomi.config;

import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/***
 *  线程池
 *  2020-10-15
 * @author cxj
 */
@Configuration
public class ThreadPoolConfig {

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Bean
    ThreadPoolTaskExecutor cancelThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(vioMiProperties.getThreadPool().getCorePoolSize());
        executor.setKeepAliveSeconds(vioMiProperties.getThreadPool().getKeepAliveSeconds());
        executor.setMaxPoolSize(vioMiProperties.getThreadPool().getMaxPoolSize());
        executor.setQueueCapacity(vioMiProperties.getThreadPool().getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

}
