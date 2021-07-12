package com.kkl.kklplus.b2b.viomi.mq.config;

import com.kkl.kklplus.entity.b2b.mq.B2BMQConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * B2B-创建投诉消息
 * @author Ryan
 * @date 2020-10-13
 */
@EnableRabbit
@Configuration
public class B2BCenterOrderComplainConfig{

    @Bean
    public Queue b2bCenterOrderComplainQueue() {
        return new Queue(B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY, true);
    }

    @Bean
    DirectExchange b2bCenterOrderComplainExchange() {
        return (DirectExchange) ExchangeBuilder.directExchange(B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY).
                delayed().withArgument("x-delayed-type", "direct").build();
    }

    @Bean
    Binding bindingB2BCenterOrderComplainExchangeMessage(Queue b2bCenterOrderComplainQueue, DirectExchange b2bCenterOrderComplainExchange) {
        return BindingBuilder.bind(b2bCenterOrderComplainQueue).
                to(b2bCenterOrderComplainExchange).
                with(B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY);
    }

}
