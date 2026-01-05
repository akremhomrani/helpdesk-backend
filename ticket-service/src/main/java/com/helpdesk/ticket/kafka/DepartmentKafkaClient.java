package com.helpdesk.ticket.kafka;

import com.helpdesk.ticket.event.DepartmentRequestEvent;
import com.helpdesk.ticket.event.DepartmentResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DepartmentKafkaClient {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<DepartmentResponseEvent>> pendingRequests = new ConcurrentHashMap<>();

    public DepartmentKafkaClient(@Qualifier("objectKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public DepartmentResponseEvent getDepartmentById(String departmentId) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<DepartmentResponseEvent> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        DepartmentRequestEvent request = DepartmentRequestEvent.builder()
                .correlationId(correlationId)
                .departmentId(departmentId)
                .build();

        log.info("Sending department validation request via Kafka for ID: {}", departmentId);
        kafkaTemplate.send("department-request", departmentId, request);

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingRequests.remove(correlationId);
            log.error("Timeout waiting for department validation response", e);
            throw new Exception("Department service not responding for id: " + departmentId);
        }
    }

    public boolean departmentExists(String departmentId) {
        try {
            DepartmentResponseEvent response = getDepartmentById(departmentId);
            return response.isExists();
        } catch (Exception e) {
            log.error("Failed to validate department: {}", departmentId, e);
            return false;
        }
    }

    @KafkaListener(topics = "department-response", groupId = "ticket-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleDepartmentResponse(DepartmentResponseEvent response) {
        log.info("Received department validation response: exists={}, name={}", response.isExists(), response.getName());
        
        CompletableFuture<DepartmentResponseEvent> future = pendingRequests.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response);
        }
    }
}
