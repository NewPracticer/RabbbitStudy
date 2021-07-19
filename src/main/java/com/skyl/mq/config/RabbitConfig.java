package com.skyl.food.orderservicemanager.config;

import com.fasterxml.jackson.databind.JavaType;
import com.skyl.food.orderservicemanager.dto.OrderMessageDTO;
import com.skyl.food.orderservicemanager.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitConfig {

    @Autowired
    OrderMessageService orderMessageService;


    /*---------------------restaurant---------------------*/
    @Bean
    public Exchange exchange1() {
        return new DirectExchange("exchange.order.restaurant");
    }

    @Bean
    public Queue queue1() {
        return new Queue("queue.order");
    }

    @Bean
    public Binding binding1() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.restaurant",
                "key.order",
                null);
    }

    /*---------------------deliveryman---------------------*/
    @Bean
    public Exchange exchange2() {
        return new DirectExchange("exchange.order.deliveryman");
    }

    @Bean
    public Binding binding2() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.deliveryman",
                "key.order",
                null);
    }


    /*---------settlement---------*/
    @Bean
    public Exchange exchange3() {
        return new FanoutExchange("exchange.order.settlement");
    }

    @Bean
    public Exchange exchange4() {
        return new FanoutExchange("exchange.settlement.order");
    }

    @Bean
    public Binding binding3() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.settlement",
                "key.order",
                null);
    }

    /*--------------reward----------------*/
    @Bean
    public Exchange exchange5() {
        return new TopicExchange("exchange.order.reward");
    }

    @Bean
    public Binding binding4() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.reward",
                "key.order",
                null);
    }

    /**
    * 设置 connectionFactory 
    *  ip地址 用户名 密码
    */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        //默认的用户名和密码
        connectionFactory.setPassword("guest");
        connectionFactory.setUsername("guest");
        //设置 发送的确认模式
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        //设置发送的的回调
        connectionFactory.setPublisherReturns(true);
        //创建连接 保证连接时进行扫描
        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        //设置自动启动
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        //确认消息是否发送到服务器
        rabbitTemplate.setMandatory(true);
        //设置返回回调
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.info("message:{}, replyCode:{}, replyText:{}, exchange:{}, routingKey:{}",
                    message, replyCode, replyText, exchange, routingKey);
        });
        //设置确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) ->
                log.info("correlationData:{}, ack:{}, cause:{}",
                        correlationData, ack, cause));
        return rabbitTemplate;
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(
            @Autowired ConnectionFactory connectionFactory
    ) {
    	//简单的消息监听器容器
        SimpleMessageListenerContainer messageListenerContainer =
                new SimpleMessageListenerContainer(connectionFactory);
        messageListenerContainer.setQueueNames("queue.order");
        //设置并发的消费者
        messageListenerContainer.setConcurrentConsumers(3);
        //设置最大的并发消费者
        messageListenerContainer.setMaxConcurrentConsumers(5);
        //设置确认模式，有手动和自动模式
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.AUTO);
        //设置消息监听器
        //        messageListenerContainer.setMessageListener(new MessageListener() {
        //            @Override
        //            public void onMessage(Message message) {
        //                log.info("message:{}", message);
        //            }
        //        });
//        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//                        messageListenerContainer.setMessageListener(new ChannelAwareMessageListener() {
//                            @Override
//                            public void onMessage(Message message, Channel channel) throws Exception {
//                                log.info("message:{}", message);
//                                orderMessageService.handleMessage(message.getBody());
//                                channel.basicAck(
//                                        message.getMessageProperties().getDeliveryTag(),
//                                        false
//                                );
//                            }
//                        });
        //设置 最大的数量
        messageListenerContainer.setPrefetchCount(1);
        //消息适配器
        // 设置相应的消息处理方法
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(orderMessageService);
        Jackson2JsonMessageConverter messageConverter = new Jackson2JsonMessageConverter();
        messageConverter.setClassMapper(new ClassMapper() {
            @Override
            public void fromClass(Class<?> clazz, MessageProperties properties) {

            }

            @Override
            public Class<?> toClass(MessageProperties properties) {
                return OrderMessageDTO.class;
            }
        });

//        messageConverter.setJavaTypeMapper(Jackson2JavaTypeMapper);
        //设置相应的消息转化器
        messageListenerAdapter.setMessageConverter(messageConverter);

        messageListenerContainer.setMessageListener(messageListenerAdapter);

        return messageListenerContainer;
    }
}
