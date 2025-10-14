package com.learnkafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.LibraryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Component
public class LibraryEventsProducer {

    @Value("${spring.kafka.topic}")
    public String topic;

    private final KafkaTemplate<Integer, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CompletableFuture<SendResult<Integer, String>> sendLibraryEventAsynch(LibraryEvent libraryEvent) throws JsonProcessingException {
        var key = libraryEvent.libraryEventId();
        var value = objectMapper.writeValueAsString(libraryEvent);
        // 1. blocking call for the first time - send the message to kafka
        // 2. Send the message to kafka asynchronously happens in the background
        var sendResultCompletableFuture = this.kafkaTemplate.send(topic, key, value);
        return sendResultCompletableFuture.whenComplete((sendResult, throwable) -> {
            if (throwable != null) {
                handleFailure(key, value, throwable);
            } else {
                log.info("Message sent to topic : {} with offset ", sendResult.getRecordMetadata().offset());
                handleSuccess(key, value, sendResult);
            }
        });
    }


    public SendResult<Integer, String> sendLibraryEventSynch(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {
        var key = libraryEvent.libraryEventId();
        var value = objectMapper.writeValueAsString(libraryEvent);
        // 1. blocking call for the first time - send the message to kafka
        // 2. Block and Wait until the msg is sent to Kafka, Send the message to kafka asynchronously happens in the background
        var integerStringSendResult = this.kafkaTemplate.send(topic, key, value).get();
        handleSuccess(key, value, integerStringSendResult);
        return integerStringSendResult;
    }


    public CompletableFuture<SendResult<Integer, String>> sendLibraryEventAsynchV3(LibraryEvent libraryEvent) throws JsonProcessingException {
        var key = libraryEvent.libraryEventId();
        var value = objectMapper.writeValueAsString(libraryEvent);
        // 1. blocking call for the first time - send the message to kafka
        // 2. Send the message to kafka asynchronously happens in the background
        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "scanner reading books barcode".getBytes()), new RecordHeader("event-media", "SpringBoot Application".getBytes()));
        ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>(topic, null, key, value, recordHeaders);
        var sendResultCompletableFuture = this.kafkaTemplate.send(producerRecord);
        CompletableFuture<SendResult<Integer, String>> sendResultCompletableFuture1 = sendResultCompletableFuture.whenComplete((sendResult, throwable) -> {
            if (throwable != null) {
                handleFailure(key, value, throwable);
            } else {
                log.info("Message sent to topic : {} with offset ", sendResult.getRecordMetadata().offset());
                handleSuccess(key, value, sendResult);
            }
        });
        return sendResultCompletableFuture1;
    }

    private void handleFailure(Integer key, String value, Throwable throwable) {
        log.error("Error sending message to topic ( {} ) : ", throwable.getMessage());
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {
        log.info("Message sent to topic : {}, key: {}, value: {} with offset: {}, partition: {} "
                , topic, key, value, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition());
    }

}
