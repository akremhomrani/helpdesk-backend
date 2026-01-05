package com.helpdesk.userservice.services;

import com.helpdesk.userservice.dtos.CreateUserRequest;
import com.helpdesk.userservice.dtos.UpdateUserRequest;
import com.helpdesk.userservice.dtos.UserResponseDTO;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final Keycloak keycloak;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${app.password-setup-base-url}")
    private String passwordSetupBaseUrl;

    @Value("${department.service.url:http://localhost:8083}")
    private String departmentServiceUrl;

    private static final String TECH_SUPPORT_ROLE = "TECH_SUPPORT";
    private static final String DEVELOPER_ROLE = "DEVELOPER";

    public ResponseEntity<String> createUser(CreateUserRequest req) {
        // Validate department assignment
        if (req.getDepartmentId() != null && !req.getDepartmentId().isEmpty()) {
            if (!isRoleAllowedForDepartment(req.getRole())) {
                return ResponseEntity.badRequest()
                        .body("Only TECH_SUPPORT and DEVELOPER roles can be assigned to departments");
            }
        } else if (isRoleAllowedForDepartment(req.getRole())) {
            return ResponseEntity.badRequest()
                    .body("TECH_SUPPORT and DEVELOPER roles must be assigned to a department");
        }

        // Prepare user representation with mandatory fields only
        UserRepresentation user = new UserRepresentation();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setEnabled(true);
        user.setRequiredActions(List.of("UPDATE_PASSWORD"));

        // Store department ID in user attributes if provided
        if (req.getDepartmentId() != null && !req.getDepartmentId().isEmpty()) {
            user.singleAttribute("departmentId", req.getDepartmentId());
        }

        // Set a temporary random password; user must change it via emailed link
        CredentialRepresentation pass = new CredentialRepresentation();
        pass.setTemporary(true);
        pass.setType(CredentialRepresentation.PASSWORD);
        pass.setValue(UUID.randomUUID().toString());
        user.setCredentials(List.of(pass));

        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            return ResponseEntity.status(response.getStatus())
                    .body("Error creating user: " + response.getStatusInfo());
        }

        String userId = CreatedResponseUtil.getCreatedId(response);
        if (userId == null) {
            return ResponseEntity.status(500).body("User created but could not retrieve ID");
        }

        // 2️⃣ Assign client role
        if (req.getRole() != null && !req.getRole().isEmpty()) {
            try {
                var client = realmResource.clients().findByClientId("spring-boot-app").get(0);
                if (client == null) {
                    // Rollback user creation
                    usersResource.delete(userId);
                    return ResponseEntity.status(404).body("Client not found. User creation rolled back.");
                }

                RoleRepresentation roleRep = realmResource.clients()
                        .get(client.getId())
                        .roles()
                        .get(req.getRole())
                        .toRepresentation();

                if (roleRep == null) {
                    // Rollback user creation
                    usersResource.delete(userId);
                    return ResponseEntity.status(404).body("Role not found in client. User creation rolled back.");
                }

                usersResource.get(userId).roles().clientLevel(client.getId()).add(List.of(roleRep));
            } catch (Exception e) {
                // Rollback user creation on any exception
                usersResource.delete(userId);
                return ResponseEntity.status(500)
                        .body("Error assigning role. User creation rolled back: " + e.getMessage());
            }
        }

        // Send password setup email (non-blocking - log error but don't fail)
        try {
            String setupLink = passwordSetupBaseUrl + "?userId=" + userId;
            emailService.sendPasswordSetupEmail(req.getEmail(), req.getFirstName(), setupLink);
            System.out.println("Password setup email sent successfully to: " + req.getEmail());
        } catch (RuntimeException e) {
            // Log email failure but don't fail user creation
            System.err.println("Failed to send password setup email to " + req.getEmail() + ": " + e.getMessage());
            // Continue with user creation
        }

        // Add user to department if specified
        if (req.getDepartmentId() != null && !req.getDepartmentId().isEmpty()) {
            try {
                String url = departmentServiceUrl + "/departments/" + req.getDepartmentId() + "/users/" + userId;
                restTemplate.postForEntity(url, null, Void.class);
            } catch (Exception e) {
                System.err.println("Failed to add user to department: " + e.getMessage());
                // Don't fail user creation if department assignment fails
            }
        }

        return ResponseEntity.ok("User created successfully; password setup email sent");
    }


    public List<UserResponseDTO> getAllUsers() {
        return keycloak.realm(realm).users().list()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<UserResponseDTO> getUsersByDepartment(String departmentId) {
        return keycloak.realm(realm).users().list()
                .stream()
                .filter(user -> {
                    if (user.getAttributes() == null || !user.getAttributes().containsKey("departmentId")) {
                        return false;
                    }
                    List<String> deptIds = user.getAttributes().get("departmentId");
                    return !deptIds.isEmpty() && departmentId.equals(deptIds.get(0));
                })
                .map(this::mapToDto)
                .toList();
    }

    public List<UserResponseDTO> getAdminUsers() {
        RealmResource realmResource = keycloak.realm(realm);
        String clientId = realmResource.clients().findByClientId("spring-boot-app").get(0).getId();
        
        return realmResource.users().list()
                .stream()
                .filter(user -> {
                    try {
                        var clientRoles = realmResource
                                .users().get(user.getId())
                                .roles().clientLevel(clientId)
                                .listAll();
                        
                        return clientRoles.stream()
                                .anyMatch(role -> "ADMIN".equals(role.getName()));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(this::mapToDto)
                .toList();
    }

    private UserResponseDTO mapToDto(UserRepresentation userRep) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUsername(userRep.getUsername());
        dto.setEmail(userRep.getEmail());
        dto.setId(userRep.getId());
        dto.setPassword("********");
        dto.setFirstName(userRep.getFirstName());
        dto.setLastName(userRep.getLastName());
        dto.setCreatedAt(userRep.getCreatedTimestamp());

        // Fetch department name from department service
        try {
            String url = departmentServiceUrl + "/departments/user/" + userRep.getId() + "/name";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                dto.setDepartmentName(response.getBody());
            }
        } catch (Exception e) {
            // Department not found or service unavailable - leave as null
        }

        // Optionally get first client role for the user
        var clientRoles = keycloak.realm(realm)
                .users().get(userRep.getId())
                .roles().clientLevel(
                        keycloak.realm(realm).clients().findByClientId("spring-boot-app").get(0).getId()
                ).listAll();

        if (!clientRoles.isEmpty()) {
            dto.setRole(clientRoles.get(0).getName());
        }

        return dto;
    }


    public void deleteUser(String id) {
        keycloak.realm(realm).users().delete(id);
    }

    public UserResponseDTO getUser(String id) {
        try {
            UserRepresentation userRep = keycloak.realm(realm).users().get(id).toRepresentation();
            return mapToDetailedDto(userRep);
        } catch (NotFoundException ex) {
            throw new NotFoundException("User not found with ID: " + id);
        }
    }

    private UserResponseDTO mapToDetailedDto(UserRepresentation userRep) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(userRep.getId());
        dto.setUsername(userRep.getUsername());
        dto.setEmail(userRep.getEmail());
        dto.setFirstName(userRep.getFirstName());
        dto.setLastName(userRep.getLastName());
        dto.setCreatedAt(userRep.getCreatedTimestamp());
        
        // Password is not retrievable from Keycloak for security reasons
        dto.setPassword("********");

        // Fetch department name from department service
        try {
            String url = departmentServiceUrl + "/departments/user/" + userRep.getId() + "/name";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                dto.setDepartmentName(response.getBody());
            }
        } catch (Exception e) {
            // Department not found or service unavailable - leave as null
        }

        // Get client roles
        try {
            var client = keycloak.realm(realm).clients().findByClientId("spring-boot-app").get(0);
            var clientRoles = keycloak.realm(realm)
                    .users().get(userRep.getId())
                    .roles().clientLevel(client.getId())
                    .listAll();

            if (!clientRoles.isEmpty()) {
                dto.setRole(clientRoles.get(0).getName());
            }
        } catch (Exception e) {
            // Role not found, leave as null
        }

        return dto;
    }

    public void updatePassword(String id, String newPassword) {
        try {
            var userResource = keycloak.realm(realm).users().get(id);

            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setTemporary(false);
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(newPassword);

            userResource.resetPassword(cred);

            // Clear required actions now that password is set
            UserRepresentation rep = userResource.toRepresentation();
            rep.setRequiredActions(Collections.emptyList());
            userResource.update(rep);
        } catch (NotFoundException ex) {
            throw new NotFoundException("User not found with ID: " + id);
        }
    }

    public ResponseEntity<String> updateUser(String id, UpdateUserRequest req) {
        try {
            var userResource = keycloak.realm(realm).users().get(id);
            UserRepresentation userRep = userResource.toRepresentation();

            // Validate department assignment if role is being updated or department is provided
            String currentRole = null;
            try {
                var client = keycloak.realm(realm).clients().findByClientId("spring-boot-app").get(0);
                var currentRoles = userResource.roles().clientLevel(client.getId()).listAll();
                if (!currentRoles.isEmpty()) {
                    currentRole = currentRoles.get(0).getName();
                }
            } catch (Exception e) {
                // Ignore role retrieval errors
            }

            String targetRole = req.getRole() != null ? req.getRole() : currentRole;
            
            if (req.getDepartmentId() != null && !req.getDepartmentId().isEmpty()) {
                if (!isRoleAllowedForDepartment(targetRole)) {
                    return ResponseEntity.badRequest()
                            .body("Only TECH_SUPPORT and DEVELOPER roles can be assigned to departments");
                }
            } else if (req.getDepartmentId() != null && req.getDepartmentId().isEmpty()) {
                // Explicitly removing department
                if (isRoleAllowedForDepartment(targetRole)) {
                    return ResponseEntity.badRequest()
                            .body("TECH_SUPPORT and DEVELOPER roles must be assigned to a department");
                }
            }

            // Update basic user information
            if (req.getEmail() != null) {
                userRep.setEmail(req.getEmail());
            }
            if (req.getFirstName() != null) {
                userRep.setFirstName(req.getFirstName());
            }
            if (req.getLastName() != null) {
                userRep.setLastName(req.getLastName());
            }

            // Update department if provided
            if (req.getDepartmentId() != null) {
                if (req.getDepartmentId().isEmpty()) {
                    // Remove department assignment
                    if (userRep.getAttributes() != null) {
                        userRep.getAttributes().remove("departmentId");
                    }
                } else {
                    // Add or update department assignment
                    userRep.singleAttribute("departmentId", req.getDepartmentId());
                }
            }

            userResource.update(userRep);

            // Update password if provided
            if (req.getPassword() != null && !req.getPassword().isEmpty()) {
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setTemporary(false);
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(req.getPassword());
                userResource.resetPassword(cred);
            }

            // Update role if provided
            if (req.getRole() != null && !req.getRole().isEmpty()) {
                try {
                    var client = keycloak.realm(realm).clients().findByClientId("spring-boot-app").get(0);
                    if (client == null) {
                        return ResponseEntity.status(404).body("Client not found");
                    }

                    // Get current roles and remove them
                    var currentRoles = userResource.roles().clientLevel(client.getId()).listAll();
                    if (!currentRoles.isEmpty()) {
                        userResource.roles().clientLevel(client.getId()).remove(currentRoles);
                    }

                    // Add new role
                    RoleRepresentation roleRep = keycloak.realm(realm).clients()
                            .get(client.getId())
                            .roles()
                            .get(req.getRole())
                            .toRepresentation();

                    if (roleRep == null) {
                        return ResponseEntity.status(404).body("Role not found in client");
                    }

                    userResource.roles().clientLevel(client.getId()).add(List.of(roleRep));
                } catch (Exception e) {
                    return ResponseEntity.status(500)
                            .body("Error updating role: " + e.getMessage());
                }
            }

            return ResponseEntity.ok("User updated successfully");
        } catch (NotFoundException ex) {
            throw new NotFoundException("User not found with ID: " + id);
        }
    }

    /**
     * Check if a role is allowed to be assigned to a department
     * @param role The role name
     * @return true if the role can be assigned to a department
     */
    private boolean isRoleAllowedForDepartment(String role) {
        return TECH_SUPPORT_ROLE.equals(role) || DEVELOPER_ROLE.equals(role);
    }
}
