package com.kkl.kklplus.b2b.viomi.mq.receiver;

import com.kkl.kklplus.entity.b2b.mq.B2BMQConstant;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class B2BCenterOrderMQReceiver {

    //@RabbitListener(queues = B2BMQConstant.MQ_B2BCENTER_RECEIVE_NEW_B2BORDER)
    public void onMessage(Message message, Channel channel) throws Exception {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        try {
            MQB2BOrderMessage.B2BOrderMessage b2BOrderMessage =
                    MQB2BOrderMessage.B2BOrderMessage.parseFrom(message.getBody());
            b2BOrderMessage.getRemarks();
            log.error("{}", b2BOrderMessage.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
