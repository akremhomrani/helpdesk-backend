package com.helpdesk.ticket.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic departmentRequestTopic() {
        return TopicBuilder.name("department-request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic departmentResponseTopic() {
        return TopicBuilder.name("department-response")
                .partitions(3)
                .replicas(1)
                .build();
    }

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

    @Bean
    public NewTopic ticketDeletedTopic() {
        return TopicBuilder.name("ticket-deleted")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
