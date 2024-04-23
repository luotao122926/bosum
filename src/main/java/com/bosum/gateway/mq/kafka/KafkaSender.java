package com.bosum.gateway.mq.kafka;

import com.bosum.gateway.mq.constant.Topic;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 消息发送
 */
@Component
@Slf4j
public class KafkaSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void sendMessage(String topic, String message) {
        //log.info("生产者存入消息:{}", message);
        ListenableFuture<SendResult<String, String>> sender = kafkaTemplate.send(new ProducerRecord<>(Topic.topic_system_log, message));
        sender.addCallback(
                result -> log.info("生产者存入消息 success:offset({}),partition({}),topic({})",
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().topic()),
                ex -> log.error("Send fail:{}", ex.getMessage()));
    }
}
