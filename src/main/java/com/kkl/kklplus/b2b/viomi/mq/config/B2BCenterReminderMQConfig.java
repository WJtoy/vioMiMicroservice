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
 *催单队列
 * @author chenxj
 * @date 2020/01/04
 */
@EnableRabbit
@Configuration
public class B2BCenterReminderMQConfig {

    @Bean
    public Queue b2bCenterReminderQueue() {
        return new Queue(B2BMQConstant.MQ_B2BCENTER_NEW_B2BORDER_REMINDER, true);
    }

    @Bean
    DirectExchange b2bCenterReminderExchange() {
        return new DirectExchange(B2BMQConstant.MQ_B2BCENTER_NEW_B2BORDER_REMINDER);
    }

    @Bean
    Binding bindingB2BCenterReminderExchangeMessage(Queue b2bCenterReminderQueue, DirectExchange b2bCenterReminderExchange) {
        return BindingBuilder.bind(b2bCenterReminderQueue)
                .to(b2bCenterReminderExchange)
                .with(B2BMQConstant.MQ_B2BCENTER_NEW_B2BORDER_REMINDER);
    }

}
