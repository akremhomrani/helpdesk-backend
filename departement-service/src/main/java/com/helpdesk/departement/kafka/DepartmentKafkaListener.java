package com.helpdesk.departement.kafka;

import com.helpdesk.departement.entity.Department;
import com.helpdesk.departement.event.DepartmentRequestEvent;
import com.helpdesk.departement.event.DepartmentResponseEvent;
import com.helpdesk.departement.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentKafkaListener {

    private final DepartmentRepository departmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "department-request",
            groupId = "department-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDepartmentRequest(DepartmentRequestEvent request) {
        log.info("Received department validation request for ID: {}, correlationId: {}", 
                request.getDepartmentId(), request.getCorrelationId());

        Optional<Department> departmentOpt = departmentRepository.findById(request.getDepartmentId());
        
        DepartmentResponseEvent response;
        if (departmentOpt.isPresent()) {
            Department department = departmentOpt.get();
            response = DepartmentResponseEvent.builder()
                    .correlationId(request.getCorrelationId())
                    .departmentId(department.getId())
                    .name(department.getName())
                    .description(department.getDescription())
                    .exists(true)
                    .build();
            log.info("Department found: {} - {}", department.getId(), department.getName());
        } else {
            response = DepartmentResponseEvent.builder()
                    .correlationId(request.getCorrelationId())
                    .departmentId(request.getDepartmentId())
                    .exists(false)
                    .build();
            log.warn("Department not found: {}", request.getDepartmentId());
        }

        kafkaTemplate.send("department-response", response.getDepartmentId(), response);
        log.info("Sent department validation response");
    }
}
