package com.learnkafka.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.controller.LibraryEventsController;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.producer.LibraryEventsProducer;
import com.learnkafka.util.TestUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.isA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//Test Slice
@WebMvcTest(LibraryEventsController.class)
class LibraryEventsControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LibraryEventsProducer libraryEventsProducer;

    @Test
    void postLibraryEvent() throws Exception {
        //given
        String json = objectMapper.writeValueAsString(TestUtil.libraryEventRecord());
        Mockito.when(libraryEventsProducer
                .sendLibraryEventSynch(isA(LibraryEvent.class))).thenReturn(null);
        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(json).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated());
        //then


    }

    @Test
    void postLibraryEvent_invalidValues() throws Exception {
        //given

        var errorMsgExpected = """
                Field: book.bookId -> Msg: must not be null,
                Field: book.bookName -> Msg: must not be blank""";
        String json = objectMapper.writeValueAsString(TestUtil.libraryEventRecordWithInvalidBook());
        Mockito.when(libraryEventsProducer
                .sendLibraryEventSynch(isA(LibraryEvent.class))).thenReturn(null);
        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(json).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> result.getResolvedException().getMessage().contains(errorMsgExpected));
        //then


    }
}