package com.skyl.mq.service;

import com.skyl.mq.dao.TransMessageDao;
import com.skyl.mq.enummeration.TransMessageType;
import com.skyl.mq.po.TransMessagePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TransMessageServiceImpl implements TransMessageService {

    @Autowired
    TransMessageDao transMessageDao;

    @Value("${skylmq.service}")
    String serviceName;

    @Override
    public TransMessagePO messageSendReady(String exchange, String routingKey, String body) {
        final String messageId = UUID.randomUUID().toString();
        TransMessagePO transMessagePO = new TransMessagePO();
        transMessagePO.setId(messageId);
        transMessagePO.setService(serviceName);
        transMessagePO.setExchange(exchange);
        transMessagePO.setRoutingKey(routingKey);
        transMessagePO.setPayload(body);
        transMessagePO.setDate(new Date());
        transMessagePO.setSequence(0);
        transMessagePO.setType(TransMessageType.SEND);
        transMessageDao.insert(transMessagePO);
        return transMessagePO;
    }

    @Override
    public void messageSendSuccess(String id) {
        transMessageDao.delete(id, serviceName);

    }

    @Override
    public TransMessagePO messageSendReturn(String id, String exchange, String routingKey, String body) {
        TransMessagePO transMessagePO = new TransMessagePO();
        transMessagePO.setId(UUID.randomUUID().toString());
        transMessagePO.setService(serviceName);
        transMessagePO.setExchange(exchange);
        transMessagePO.setRoutingKey(routingKey);
        transMessagePO.setPayload(body);
        transMessagePO.setDate(new Date());
        transMessagePO.setSequence(0);
        transMessagePO.setType(TransMessageType.SEND);
        transMessageDao.insert(transMessagePO);
        return transMessagePO;
    }

    @Override
    public void messageResend(String id) {
        TransMessagePO transMessagePO = transMessageDao.selectByIdAndService(id, serviceName);
        transMessagePO.setSequence(transMessagePO.getSequence() + 1);
        transMessageDao.update(transMessagePO);
    }

    @Override
    public List<TransMessagePO> listReadyMessages() {
        return transMessageDao.selectByTypeAndService(TransMessageType.SEND.name(), serviceName);
    }

    @Override
    public void messageDead(String id, String exchange, String routingKey, String queue, String body) {
        TransMessagePO transMessagePO = new TransMessagePO();
        transMessagePO.setId(id);
        transMessagePO.setService(serviceName);
        transMessagePO.setExchange(exchange);
        transMessagePO.setRoutingKey(routingKey);
        transMessagePO.setPayload(body);
        transMessagePO.setDate(new Date());
        transMessagePO.setSequence(0);
        transMessagePO.setType(TransMessageType.DEAD);
        transMessageDao.insert(transMessagePO);
    }

    @Override
    public void messageDead(String id) {
        TransMessagePO transMessagePO = transMessageDao.selectByIdAndService(id, serviceName);
        transMessagePO.setType(TransMessageType.DEAD);
        transMessageDao.update(transMessagePO);
    }

    @Override
    public TransMessagePO messageReceiveReady(String id, String exchange, String routingKey, String queue,
                                              String body) {
        TransMessagePO transMessagePO = transMessageDao.selectByIdAndService(id, serviceName);
        if (null == transMessagePO) {
            transMessagePO = new TransMessagePO();
            transMessagePO.setId(id);
            transMessagePO.setService(serviceName);
            transMessagePO.setExchange(exchange);
            transMessagePO.setRoutingKey(routingKey);
            transMessagePO.setQueue(queue);
            transMessagePO.setPayload(body);
            transMessagePO.setDate(new Date());
            transMessagePO.setSequence(0);
            transMessagePO.setType(TransMessageType.RECEIVE);
            transMessageDao.insert(transMessagePO);
        } else {
            transMessagePO.setSequence(transMessagePO.getSequence() + 1);
            transMessageDao.update(transMessagePO);
        }
        return transMessagePO;
    }

    @Override
    public void messageReceiveSuccess(String id) {
        transMessageDao.delete(id, serviceName);
    }
}
