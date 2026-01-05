package com.helpdesk.userservice.dtos;

import lombok.Data;

@Data
public class UserResponseDTO {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String role;
    private Long createdAt;
    private String departmentId;
    private String departmentName;
}
