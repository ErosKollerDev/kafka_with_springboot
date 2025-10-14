package com.learnkafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.LibraryEvent;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class LibraryEventsProducer {

    @Value("${spring.kafka.topic}")
    public String topic;

    private final KafkaTemplate<Integer, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        var key = libraryEvent.libraryEventId();
        var value = objectMapper.writeValueAsString(libraryEvent);
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

    private void handleFailure(Integer key, String value, Throwable throwable) {
        log.error("Error sending message to topic ( {} ) : ", throwable.getMessage());
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {
        log.info("Message sent to topic : {}, key: {}, value: {} with offset: {}, partition: {} "
                , topic, key, value, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition());
    }

}
