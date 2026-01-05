package com.helpdesk.userservice.dtos;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String password; // Optional: update password if provided
    private String departmentId; // Only for TECH_SUPPORT and DEVELOPER roles
}
