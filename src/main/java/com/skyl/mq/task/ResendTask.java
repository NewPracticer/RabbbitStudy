package com.skyl.mq.task;

import com.skyl.mq.po.TransMessagePO;
import com.skyl.mq.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Configuration
@Component
@Slf4j
public class ResendTask {

    @Autowired
    TransMessageService transMessageService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("#{new Integer('${skylmq.resendTimes}')}")
    int resendTimes;

    @Scheduled(fixedDelayString = "${skylmq.resendFreq}")
    public void resendMessage() {
        log.info("resendMessage()");
        //遍历当前所有处在准备状态的失败消息
        List<TransMessagePO> messagePOS = transMessageService.listReadyMessages();
        log.info("resendMessage():messagePOS:{}", messagePOS);
        for (TransMessagePO messagePO : messagePOS) {
            log.info("resendMessage(): messagePO:{}", messagePO);
            //如果消息的最大重试次数超过最大次数
            if (messagePO.getSequence() > resendTimes) {
                log.error("message resend too many times! transMessagePO:{}", messagePO);
                //设置消息为死信
                transMessageService.messageDead(messagePO.getId());
                continue;
            }
            //设置消息属性
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            Message message = new Message(messagePO.getPayload().getBytes(), messageProperties);
            message.getMessageProperties().setMessageId(messagePO.getId());
            //再次发送消息
            rabbitTemplate.convertAndSend(messagePO.getExchange(), messagePO.getRoutingKey(),
                    message, new CorrelationData(messagePO.getId()));
            transMessageService.messageResend(messagePO.getId());
        }
    }
}
