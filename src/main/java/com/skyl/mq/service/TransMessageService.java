package com.skyl.mq.service;

import com.skyl.mq.po.TransMessagePO;

import java.util.List;

public interface TransMessageService {

    TransMessagePO messageSendReady(String exchange, String routingKey, String body);

    void messageSendSuccess(String id);

    TransMessagePO messageSendReturn(String id, String exchange, String routingKey, String body);

    void messageResend(String id);

    void messageDead(String id, String exchange, String routingKey, String queue, String body);

    void messageDead(String id);

    TransMessagePO messageReceiveReady(String id, String exchange, String routingKey, String queue, String body);

    void messageReceiveSuccess(String id);

    List<TransMessagePO> listReadyMessages();

}
