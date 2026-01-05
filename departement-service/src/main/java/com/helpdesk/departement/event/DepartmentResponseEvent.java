package com.helpdesk.departement.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseEvent {
    private String correlationId;
    private String departmentId;
    private String name;
    private String description;
    private boolean exists;
}
