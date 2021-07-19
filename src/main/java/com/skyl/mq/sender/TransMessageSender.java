package com.skyl.mq.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyl.mq.po.TransMessagePO;
import com.skyl.mq.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class TransMessageSender {

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    TransMessageService transMessageService;

    @Transactional
    public void send(String exchange, String routingKey, Object payload) {
        log.info("send():exchange:{}, routingKey:{}, payload{}",
                exchange, routingKey, payload);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String payloadStr = mapper.writeValueAsString(payload);

            //发送前暂存消息
            TransMessagePO transMessagePO = transMessageService.messageSendReady(
                    exchange,
                    routingKey,
                    payloadStr);

            //设置消息属性
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            Message message = new Message(payloadStr.getBytes(), messageProperties);
            message.getMessageProperties().setMessageId(transMessagePO.getId());
            //发送
            rabbitTemplate.convertAndSend(exchange, routingKey,
                    message, new CorrelationData(transMessagePO.getId()));

            log.info("发送消息，消息ID:{}", transMessagePO.getId());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("发送RabbitMQ消息失败！", e);
        }
    }
}
