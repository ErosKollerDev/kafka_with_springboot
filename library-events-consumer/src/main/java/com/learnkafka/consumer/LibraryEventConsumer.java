package com.learnkafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learnkafka.service.LibraryEventsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class LibraryEventConsumer {

    private final LibraryEventsService libraryEventsService;


    @KafkaListener(topics = {"${spring.kafka.topic}"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.warn("\nRecord: \n{} ", consumerRecord.toString());
        log.info("\nReceived message: \n{} ", consumerRecord.value());
        this.libraryEventsService.saveLibraryEvent(consumerRecord);
    }
}
