package com.learnkafka.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.entity.LibraryEvent;
import com.learnkafka.entity.LibraryEventType;
import com.learnkafka.repository.BooksRepository;
import com.learnkafka.repository.LibraryEventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryEventsService {


    private final LibraryEventsRepository libraryEventsRepository;
    private final BooksRepository booksRepository;
    private final ObjectMapper objectMapper;


    public void saveLibraryEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.info("Saving LibraryEvent");
        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);

        if(libraryEvent != null && libraryEvent.getLibraryEventId() != null && libraryEvent.getLibraryEventId() == 9999){
            throw new RecoverableDataAccessException("""
                        
                        Testing RecoverableDataAccessException.
                        Temporary exception, should be resolved in next retry.
                    """);
        }


        switch (libraryEvent.getLibraryEventType()) {
            case LibraryEventType.NEW -> save(libraryEvent);
            case LibraryEventType.UPDATE -> validate(libraryEvent);

            case LibraryEventType.DELETE -> delete(libraryEvent);
            default -> {
                log.warn("Unknown LibraryEventType {}", libraryEvent.getLibraryEventType());
            }
        }
        log.info("\nSaved LibraryEvent \n{}", libraryEvent);
    }

    private void validate(LibraryEvent libraryEvent) {
        if (libraryEvent.getLibraryEventId() == null)
            throw new IllegalArgumentException("LibraryEventId cannot be null");
        libraryEventsRepository.findById(libraryEvent.getLibraryEventId())
                .ifPresentOrElse(
                        libraryEvent1 -> {
                            log.info("LibraryEventId exists in the database");
                            save(libraryEvent);
                        },
                        () -> {
                            throw new IllegalArgumentException("LibraryEventId does not exists in the database");
                        }
                );

    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);
//        booksRepository.save(libraryEvent.getBook());
    }

    private void delete(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
//        libraryEventsRepository.delete(libraryEvent);
        booksRepository.delete(libraryEvent.getBook());
    }


}
