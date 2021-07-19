package com.skyl.mq.listener;

import com.skyl.mq.po.TransMessagePO;
import com.skyl.mq.service.TransMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * 抽象消息监听器
 */
@Slf4j
public abstract class AbstractMessageListener implements ChannelAwareMessageListener {

    @Autowired
    TransMessageService transMessageService;

    @Value("#{new Integer('${skylmq.reconsumeTimes}')}")
    int reconsumeTimes;

    public abstract void receiveMessage(Message message);

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();

        TransMessagePO transMessagePO =
                transMessageService.messageReceiveReady(
                        message.getMessageProperties().getMessageId(),
                        message.getMessageProperties().getReceivedExchange(),
                        message.getMessageProperties().getReceivedRoutingKey(),
                        message.getMessageProperties().getConsumerQueue(),
                        new String(message.getBody())
                );

        log.info("收到消息,当前消息ID:{} 消费次数：{}", messageProperties.getMessageId(), transMessagePO.getSequence());

        try {
            receiveMessage(message);
            // 成功的回执
            channel.basicAck(deliveryTag, false);

            transMessageService.messageReceiveSuccess(transMessagePO.getId());
        } catch (Exception e) {
            log.error("RabbitMQ 消息消费失败，" + e.getMessage(), e);
            if (transMessagePO.getSequence() >= reconsumeTimes) {
                // 入死信队列
                channel.basicReject(deliveryTag, false);
            } else {
                // 重回到队列，重新消费, 按照2的指数级递增
                Thread.sleep((long) (Math.pow(2, transMessagePO.getSequence()) * 1000));
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }
}
