package com.example.service;


import com.example.entity.MailMQDTO;
import com.rabbitmq.client.Channel;

public interface MailMQ {
    void processEmailQueue(MailMQDTO mailMQDTO, Channel channel,long tag) throws Exception;
}
