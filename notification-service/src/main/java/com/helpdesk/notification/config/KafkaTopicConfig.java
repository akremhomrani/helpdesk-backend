package com.helpdesk.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic ticketCreatedTopic() {
        return TopicBuilder.name("ticket-created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketUpdatedTopic() {
        return TopicBuilder.name("ticket-updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketAssignedTopic() {
        return TopicBuilder.name("ticket-assigned")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketResolvedTopic() {
        return TopicBuilder.name("ticket-resolved")
                .partitions(3)
                .replicas(1)
                .build();
    }
}