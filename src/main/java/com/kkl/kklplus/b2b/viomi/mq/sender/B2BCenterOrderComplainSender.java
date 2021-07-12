package com.kkl.kklplus.b2b.viomi.mq.sender;

import com.googlecode.protobuf.format.JsonFormat;
import com.kkl.kklplus.b2b.viomi.service.SysLogService;
import com.kkl.kklplus.entity.b2b.mq.B2BMQConstant;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderComplainMessage;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderMessage;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderReminderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 *投诉队列
 * @author chenxj
 * @date 2020/10/14
 */
@Slf4j
@Component
public class B2BCenterOrderComplainSender implements RabbitTemplate.ConfirmCallback {

    @Autowired
    private SysLogService sysLogService;

    private RabbitTemplate rabbitTemplate;

    private RetryTemplate retryTemplate;

    @Autowired
    public B2BCenterOrderComplainSender(RabbitTemplate kklRabbitTemplate, RetryTemplate kklRabbitRetryTemplate) {
        this.rabbitTemplate = kklRabbitTemplate;
        this.rabbitTemplate.setConfirmCallback(this);
        this.retryTemplate = kklRabbitRetryTemplate;
    }

    /**
     * 正常发送消息
     *
     * @param message 消息体
     */
    public void send(MQB2BOrderComplainMessage.B2BOrderComplainMessage message) {
        try {
            retryTemplate.execute((RetryCallback<Object, Exception>) context -> {
                context.setAttribute(B2BMQConstant.RETRY_CONTEXT_ATTRIBUTE_KEY_MESSAGE, message);
                rabbitTemplate.convertAndSend(
                        B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY,
                        B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY,
                        message.toByteArray(), msg -> {
                            msg.getMessageProperties().setDelay(0);
                            msg.getMessageProperties().setHeader(B2BMQConstant.MESSAGE_PROPERTIES_HEADER_KEY_TIMES, 0);
                            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return msg;
                        },
                        new CorrelationData());
                return null;
            }, context -> {
                Object msgObj = context.getAttribute(B2BMQConstant.RETRY_CONTEXT_ATTRIBUTE_KEY_MESSAGE);
                MQB2BOrderMessage.B2BOrderMessage msg = MQB2BOrderMessage.B2BOrderMessage.parseFrom((byte[]) msgObj);
                Throwable throwable = context.getLastThrowable();
                log.error("MQ_B2BCENTER_COMPLAIN_DELAY {}, {}", throwable.getLocalizedMessage(), msg);
                String msgJson = new JsonFormat().printToString(msg);
                sysLogService.insert(1L,msgJson,"调用工单投诉队列失败：" + throwable.getLocalizedMessage(),
                        "调用工单投诉队列失败", B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY, "");
                return null;
            });
        } catch (Exception e) {
            String msgJson = new JsonFormat().printToString(message);
            sysLogService.insert(1L,msgJson,"调用工单投诉队列失败：" + e.getMessage(),
                    "调用工单投诉队列失败", B2BMQConstant.MQ_B2BCENTER_COMPLAIN_DELAY, "");
        }

    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

    }
}
