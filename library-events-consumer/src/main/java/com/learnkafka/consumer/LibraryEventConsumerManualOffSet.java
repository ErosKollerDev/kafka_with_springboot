package com.learnkafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Deprecated(since = "Manual Offset, only use it if you want to have a fine tunne!")
@Slf4j
//@Component
public class LibraryEventConsumerManualOffSet implements AcknowledgingMessageListener<Integer, String> {
//    @KafkaListener(topics = {"${spring.kafka.topic}"})
//    public void onMessage(ConsumerRecord<Integer, String> record) {
//        log.warn("\nRecord: \n{} ", record.toString());
//        log.info("\nReceived message: \n{} ", record.value());
//    }

    @KafkaListener(topics = {"${spring.kafka.topic}"})
    @Override
    public void onMessage(ConsumerRecord<Integer, String> data, Acknowledgment acknowledgment) {
        log.warn("\nRecord: \n{} ", data.toString());
        log.info("\nReceived message: \n{} ", data.value());
        assert acknowledgment != null;
        acknowledgment.acknowledge();
    }
}
