package com.helpdesk.ticket.service;

import com.helpdesk.ticket.event.TicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, TicketEvent> kafkaTemplate;

    public void sendTicketCreatedEvent(TicketEvent event) {
        log.info("Publishing ticket created event for ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("ticket-created", event.getTicketId(), event);
    }

    public void sendTicketUpdatedEvent(TicketEvent event) {
        log.info("Publishing ticket updated event for ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("ticket-updated", event.getTicketId(), event);
    }

    public void sendTicketAssignedEvent(TicketEvent event) {
        log.info("Publishing ticket assigned event for ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("ticket-assigned", event.getTicketId(), event);
    }

    public void sendTicketResolvedEvent(TicketEvent event) {
        log.info("Publishing ticket resolved event for ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("ticket-resolved", event.getTicketId(), event);
    }

    public void sendTicketDeletedEvent(TicketEvent event) {
        log.info("Publishing ticket deleted event for ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("ticket-deleted", event.getTicketId(), event);
    }
}
