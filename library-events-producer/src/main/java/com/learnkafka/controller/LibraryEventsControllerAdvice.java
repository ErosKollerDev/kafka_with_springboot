package com.learnkafka.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class LibraryEventsControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleRequestBody(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fieldError ->  "%nField: %s -> Msg: %s".formatted(fieldError.getField(),fieldError.getDefaultMessage())  )
                .sorted()
                .collect(Collectors.joining(", "));
      log.error("errorMsg : {} ", errorMsg);
        return ResponseEntity.badRequest().body(errorMsg);
    }

}
