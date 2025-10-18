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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeliveryAttemptAwareRetryListener;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Objects;

@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class LibraryEventsConsumerConfig {

    private final KafkaProperties properties;



    public DefaultErrorHandler errorHandler() {
        FixedBackOff backOff = new FixedBackOff(1000L, 2);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(backOff);

        defaultErrorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Fail to Record Message, custom Listener by eRoS: ->  message {}. Attempt {}. Error: {}", record, deliveryAttempt, ex.getMessage());
        });


        return  defaultErrorHandler;
    }


    @Bean
    @ConditionalOnMissingBean(
            name = {"kafkaListenerContainerFactory"}
    )
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer configurer, ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory, ObjectProvider<ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>> kafkaContainerCustomizer) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, (ConsumerFactory)kafkaConsumerFactory.getIfAvailable(() -> new DefaultKafkaConsumerFactory(this.properties.buildConsumerProperties())));
        Objects.requireNonNull(factory);
        kafkaContainerCustomizer.ifAvailable(factory::setContainerCustomizer);

        factory.setConcurrency(3);
        factory.setCommonErrorHandler(this.errorHandler());
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
