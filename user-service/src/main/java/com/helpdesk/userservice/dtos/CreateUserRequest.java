package com.helpdesk.userservice.dtos;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String role;
    private String departmentId; // Only for TECH_SUPPORT and DEVELOPER roles
}

