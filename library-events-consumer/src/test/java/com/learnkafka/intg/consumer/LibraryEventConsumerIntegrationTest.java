package com.learnkafka.intg.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.consumer.LibraryEventConsumer;
import com.learnkafka.entity.LibraryEvent;
import com.learnkafka.entity.LibraryEventType;
import com.learnkafka.repository.LibraryEventsRepository;
import com.learnkafka.service.LibraryEventsService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@EmbeddedKafka(topics = {"library-events"
        , "library-events.RETRY"
        , "library-events.DLT"
}
        , partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class LibraryEventConsumerIntegrationTest {


    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;
    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;
    @Autowired
    LibraryEventsRepository libraryEventsRepository;

    @Autowired
    ObjectMapper objectMapper;


    @MockitoSpyBean
    LibraryEventConsumer libraryEventsConsumerSpy;

    @MockitoSpyBean
    LibraryEventsService libraryEventsServiceSpy;

    private Producer<Integer, String> producer;


    @BeforeEach
    void setUp() {
        var container = endpointRegistry.getListenerContainers()
                .stream().filter(messageListenerContainer ->
                        Objects.equals(messageListenerContainer.getGroupId(), "library-events-consumer-group"))
                .collect(Collectors.toList()).get(0);
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
//        for (MessageListenerContainer msg : endpointRegistry.getListenerContainers()){
//            ContainerTestUtils.waitForAssignment(msg, embeddedKafkaBroker.getPartitionsPerTopic());
//        }
    }


    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
    }

    @Test
    void publishNewEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        String json = " {\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        kafkaTemplate.send("library-events", json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).saveLibraryEvent(isA(ConsumerRecord.class));
        List<LibraryEvent> libraryEventList = this.libraryEventsRepository.findAll();
        assertEquals(1, libraryEventList.size());
        LibraryEvent libraryEvent = libraryEventList.get(0);
        assertEquals(LibraryEventType.NEW, libraryEvent.getLibraryEventType());
        assertEquals(456, libraryEvent.getBook().getBookId());
        assertEquals("Kafka Using Spring Boot", libraryEvent.getBook().getBookName());
        assertEquals("Dilip", libraryEvent.getBook().getBookAuthor());
    }


    @Test
    void publishUpdateEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        String json = " {\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);
//        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        LibraryEvent saved = this.libraryEventsRepository.save(libraryEvent);
        String newBookName = "Kafka Using Spring Boot Updated";
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.getBook().setBookName(newBookName);
        kafkaTemplate.send("library-events", objectMapper.writeValueAsString(libraryEvent)).get();


        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(1)).saveLibraryEvent(isA(ConsumerRecord.class));

        libraryEvent = this.libraryEventsRepository.findById(saved.getLibraryEventId()).get();

        assertEquals(LibraryEventType.UPDATE, libraryEvent.getLibraryEventType());
        assertEquals(456, libraryEvent.getBook().getBookId());
        assertEquals(newBookName, libraryEvent.getBook().getBookName());
        assertEquals("Dilip", libraryEvent.getBook().getBookAuthor());
    }


    @Test
    void publishUpdateEvent_null_libraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        //given
        String json = " {\"libraryEventId\":null,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        kafkaTemplate.send("library-events", json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        //then
        verify(libraryEventsConsumerSpy, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventsServiceSpy, times(3)).saveLibraryEvent(isA(ConsumerRecord.class));

    }

}