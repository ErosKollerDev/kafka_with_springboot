package com.learnkafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class LibraryEventsConsumerConfig {

    private final KafkaProperties properties;


    public DefaultErrorHandler errorHandler() {



        var fixedBackOff = new FixedBackOff(1000L, 2);
        var exponentialBackOffWithMaxRetries = new ExponentialBackOffWithMaxRetries(2);
        exponentialBackOffWithMaxRetries.setInitialInterval(1_000L);
        exponentialBackOffWithMaxRetries.setMaxInterval(2_000L);
        exponentialBackOffWithMaxRetries.setMultiplier(3.0D);

        var defaultErrorHandler = new DefaultErrorHandler(
                exponentialBackOffWithMaxRetries
//                fixedBackOff
        );
        var errorListsNotRetriable = List.of(
                IllegalArgumentException.class
        );
        errorListsNotRetriable.forEach(defaultErrorHandler::addNotRetryableExceptions);


        defaultErrorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Fail to Record Message, custom Listener by eRoS: ->  message {}. Attempt {}. Error: {}", record, deliveryAttempt, ex.getMessage());
        });


        return defaultErrorHandler;
    }


    @Bean
    @ConditionalOnMissingBean(
            name = {"kafkaListenerContainerFactory"}
    )
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer configurer, ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory, ObjectProvider<ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>> kafkaContainerCustomizer) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, (ConsumerFactory) kafkaConsumerFactory.getIfAvailable(() -> new DefaultKafkaConsumerFactory(this.properties.buildConsumerProperties())));
        Objects.requireNonNull(factory);
        kafkaContainerCustomizer.ifAvailable(factory::setContainerCustomizer);

        factory.setConcurrency(3);
        factory.setCommonErrorHandler(this.errorHandler());
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
