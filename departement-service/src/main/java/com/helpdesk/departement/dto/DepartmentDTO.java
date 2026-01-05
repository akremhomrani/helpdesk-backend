package com.helpdesk.departement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private String id;
    private String name;
    private String description;
    private List<String> userIds;
    private LocalDateTime createdAt;
}