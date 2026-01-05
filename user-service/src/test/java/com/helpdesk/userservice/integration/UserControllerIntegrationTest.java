package com.helpdesk.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.userservice.dtos.CreateUserRequest;
import com.helpdesk.userservice.dtos.UpdatePasswordRequest;
import com.helpdesk.userservice.services.EmailService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for UserController - tests the full flow:
 * Controller → Service → External Keycloak interaction
 * 
 * Note: Keycloak is mocked since it's an external service,
 * but the test validates the full Spring Boot application context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Keycloak keycloak;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RealmResource realmResource;

    @MockBean
    private UsersResource usersResource;

    @MockBean
    private UserResource userResource;

    @MockBean
    private ClientsResource clientsResource;

    @MockBean
    private ClientResource clientResource;

    @MockBean
    private RolesResource rolesResource;

    @MockBean
    private RoleResource roleResource;

    @MockBean
    private RoleMappingResource roleMappingResource;

    @MockBean
    private RoleScopeResource roleScopeResource;

    @MockBean
    private Response response;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_CLIENT_ID = "client-123";

    @BeforeEach
    void setUp() {
        // Reset all mocks before each test
        reset(keycloak, emailService, realmResource, usersResource, userResource,
                clientsResource, clientResource, rolesResource, roleResource,
                roleMappingResource, roleScopeResource, response);
    }

    @Test
    void createUser_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("USER");

        setupSuccessfulUserCreationFlow();

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User created successfully")));

        verify(usersResource).create(any(UserRepresentation.class));
        verify(emailService).sendPasswordSetupEmail(eq("test@example.com"), eq("Test"), anyString());
    }

    @Test
    void createUser_WithoutRole_ShouldCreateUserSuccessfully() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");

        setupBasicUserCreation();

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User created successfully")));

        verify(usersResource).create(any(UserRepresentation.class));
        verify(clientsResource, never()).findByClientId(anyString());
    }

    @Test
    void createUser_WhenKeycloakFails_ShouldReturn400() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);
        when(response.getStatusInfo()).thenReturn(Response.Status.BAD_REQUEST);

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error creating user")));
    }

    @Test
    void createUser_WhenEmailServiceFails_ShouldReturn500ButUserCreated() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://test.com/users/" + TEST_USER_ID));

        doThrow(new RuntimeException("Email service down"))
                .when(emailService).sendPasswordSetupEmail(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("failed to send setup email")));

        // Verify user was NOT deleted (email failure doesn't rollback user creation)
        verify(usersResource, never()).delete(anyString());
    }

    @Test
    void getAllUsers_WhenUsersExist_ShouldReturn200WithUserList() throws Exception {
        // Arrange
        UserRepresentation user1 = createMockUserRepresentation("user1", "user1@test.com");
        UserRepresentation user2 = createMockUserRepresentation("user2", "user2@test.com");

        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);

        RoleRepresentation role = new RoleRepresentation();
        role.setName("USER");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of(user1, user2));

        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));

        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(TEST_CLIENT_ID)).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of(role));

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("user1")))
                .andExpect(jsonPath("$[0].email", is("user1@test.com")))
                .andExpect(jsonPath("$[0].password", is("********")))
                .andExpect(jsonPath("$[1].username", is("user2")))
                .andExpect(jsonPath("$[1].email", is("user2@test.com")));

        verify(usersResource).list();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturn200WithEmptyList() throws Exception {
        // Arrange
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getUser_WithValidId_ShouldReturn200WithUserDetails() throws Exception {
        // Arrange
        String userId = "user-123";
        UserRepresentation userRep = createMockUserRepresentation("testuser", "test@test.com");
        userRep.setId(userId);

        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);

        RoleRepresentation role = new RoleRepresentation();
        role.setName("ADMIN");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);

        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));

        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(TEST_CLIENT_ID)).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of(role));

        // Act & Assert
        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@test.com")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.password", is("********")));

        verify(usersResource, atLeastOnce()).get(userId);
    }

    @Test
    void getUser_WithInvalidId_ShouldReturn404() throws Exception {
        // Arrange
        String invalidUserId = "invalid-id";

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(invalidUserId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new jakarta.ws.rs.NotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/users/{id}", invalidUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePassword_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        String userId = "user-123";
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setNewPassword("NewSecurePassword123!");

        UserRepresentation userRep = new UserRepresentation();
        userRep.setRequiredActions(List.of("UPDATE_PASSWORD"));

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).resetPassword(any());
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act & Assert
        mockMvc.perform(post("/users/{id}/password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password updated successfully"));

        verify(userResource).resetPassword(any());
        verify(userResource).update(any(UserRepresentation.class));
    }

    @Test
    void updatePassword_WithInvalidUserId_ShouldReturn404() throws Exception {
        // Arrange
        String invalidUserId = "invalid-id";
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setNewPassword("NewPassword123!");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(invalidUserId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new jakarta.ws.rs.NotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(post("/users/{id}/password", invalidUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_WithValidId_ShouldReturn200() throws Exception {
        // Arrange
        String userId = "user-to-delete";

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));

        verify(usersResource).delete(userId);
    }

    @Test
    void deleteUser_WhenKeycloakFails_ShouldPropagateError() throws Exception {
        // Arrange
        String userId = "user-to-delete";

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        doThrow(new RuntimeException("Keycloak deletion failed")).when(usersResource).delete(userId);

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createUser_WithRoleAssignmentFailure_ShouldRollbackUser() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("ADMIN");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://test.com/users/" + TEST_USER_ID));

        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app"))
                .thenThrow(new RuntimeException("Client service error"));

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error assigning role")));

        // Verify user was rolled back
        verify(usersResource).delete(TEST_USER_ID);
    }

    // Helper methods

    private void setupSuccessfulUserCreationFlow() {
        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);

        RoleRepresentation role = new RoleRepresentation();
        role.setName("USER");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://test.com/users/" + TEST_USER_ID));

        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));
        when(clientsResource.get(TEST_CLIENT_ID)).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(role);

        when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(TEST_CLIENT_ID)).thenReturn(roleScopeResource);
        doNothing().when(roleScopeResource).add(anyList());

        doNothing().when(emailService).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    private void setupBasicUserCreation() {
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://test.com/users/" + TEST_USER_ID));

        doNothing().when(emailService).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    private UserRepresentation createMockUserRepresentation(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setId(java.util.UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }
}
