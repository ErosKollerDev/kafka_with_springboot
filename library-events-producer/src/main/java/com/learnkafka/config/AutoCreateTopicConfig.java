package com.learnkafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class AutoCreateTopicConfig {


    @Value("${spring.kafka.topic}")
    public String topic;

    @Bean
    public NewTopic libraryEventsNewTopic(){
//        return TopicBuilder.name("library-events").partitions(3).replicas(3).build();
        return new NewTopic(topic, 3, (short)3);
    }


}
