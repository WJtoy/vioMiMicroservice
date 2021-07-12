package com.kkl.kklplus.b2b.viomi.mq.config;

import com.kkl.kklplus.entity.b2b.mq.B2BMQConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *工单处理队列
 * @author chenxj
 * @date 2019/09/09
 */
@EnableRabbit
@Configuration
public class B2BCenterOrderProcessMQConfig {

    @Bean
    public Queue b2BCenterOrderProcessQueue() {
        return new Queue(B2BMQConstant.MQ_B2BCENTER_PROCESS_KKL_ORDER, true);
    }

    @Bean
    DirectExchange b2BCenterOrderProcessExchange() {
        return new DirectExchange(B2BMQConstant.MQ_B2BCENTER_PROCESS_KKL_ORDER);
    }

    @Bean
    Binding bindingB2BCenterOrderProcessExchangeMessage(Queue b2BCenterOrderProcessQueue,
                                                        DirectExchange b2BCenterOrderProcessExchange) {
        return BindingBuilder.bind(b2BCenterOrderProcessQueue)
                .to(b2BCenterOrderProcessExchange)
                .with(B2BMQConstant.MQ_B2BCENTER_PROCESS_KKL_ORDER);
    }

}
