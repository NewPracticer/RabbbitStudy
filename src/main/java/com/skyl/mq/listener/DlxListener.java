package com.skyl.mq.listener;

import com.skyl.mq.service.TransMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 死信队列监听器
 */
@Component
@ConditionalOnProperty("skylmq.dlxEnabled")
@Slf4j
public class DlxListener implements ChannelAwareMessageListener {

    @Autowired
    TransMessageService transMessageService;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        log.error("dead letter message：{} | tag：{}", messageBody, message.getMessageProperties().getDeliveryTag());

        //         发邮件
        //        sendEmail(logKey, messageProperties.getMessageId(), messageBody);
        log.error("sendEmail");

        //入库
        transMessageService.messageDead(
                message.getMessageProperties().getMessageId(),
                message.getMessageProperties().getReceivedExchange(),
                message.getMessageProperties().getReceivedRoutingKey(),
                message.getMessageProperties().getConsumerQueue(),
                messageBody);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
