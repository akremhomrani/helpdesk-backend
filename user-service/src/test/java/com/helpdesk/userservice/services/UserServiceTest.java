package com.helpdesk.userservice.services;

import com.helpdesk.userservice.dtos.CreateUserRequest;
import com.helpdesk.userservice.dtos.UserResponseDTO;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private EmailService emailService;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private ClientsResource clientsResource;

    @Mock
    private ClientResource clientResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private Response response;

    @InjectMocks
    private UserService userService;

    private static final String TEST_REALM = "test-realm";
    private static final String PASSWORD_SETUP_BASE_URL = "http://test.com/setup";
    private static final String TEST_USER_ID = "test-user-id-123";
    private static final String TEST_CLIENT_ID = "test-client-id";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "realm", TEST_REALM);
        ReflectionTestUtils.setField(userService, "passwordSetupBaseUrl", PASSWORD_SETUP_BASE_URL);
    }

    @Test
    void createUser_WithValidRequest_ShouldReturnSuccessResponse() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        setupSuccessfulUserCreation();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("User created successfully; password setup email sent", result.getBody());
        
        // Verify user creation
        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(userCaptor.capture());
        
        UserRepresentation capturedUser = userCaptor.getValue();
        assertEquals(request.getUsername(), capturedUser.getUsername());
        assertEquals(request.getEmail(), capturedUser.getEmail());
        assertEquals(request.getFirstName(), capturedUser.getFirstName());
        assertEquals(request.getLastName(), capturedUser.getLastName());
        assertTrue(capturedUser.isEnabled());
        assertTrue(capturedUser.getRequiredActions().contains("UPDATE_PASSWORD"));
        
        // Verify email was sent
        verify(emailService).sendPasswordSetupEmail(
                eq(request.getEmail()),
                eq(request.getFirstName()),
                contains(TEST_USER_ID)
        );
    }

    @Test
    void createUser_WithRole_ShouldAssignRoleSuccessfully() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        request.setRole("ADMIN");
        setupSuccessfulUserCreationWithRole();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(roleScopeResource).add(anyList());
    }

    @Test
    void createUser_WhenKeycloakReturnsError_ShouldReturnErrorResponse() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);
        when(response.getStatusInfo()).thenReturn(Response.Status.BAD_REQUEST);

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertTrue(result.getBody().contains("Error creating user"));
    }

    @Test
    void createUser_WhenUserIdCannotBeRetrieved_ShouldReturnError() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(null);

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("User created but could not retrieve ID", result.getBody());
    }

    @Test
    void createUser_WhenClientNotFound_ShouldRollbackUserCreation() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        request.setRole("ADMIN");
        
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://test.com/users/" + TEST_USER_ID));
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertTrue(result.getBody().contains("Error assigning role"));
        verify(usersResource).delete(TEST_USER_ID);
    }

    @Test
    void createUser_WhenRoleNotFound_ShouldRollbackUserCreation() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        request.setRole("INVALID_ROLE");
        
        setupUserCreationWithInvalidRole();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertTrue(result.getBody().contains("Role not found"));
        verify(usersResource).delete(TEST_USER_ID);
    }

    @Test
    void createUser_WhenRoleAssignmentFails_ShouldRollbackUserCreation() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        request.setRole("ADMIN");
        
        setupUserCreationWithRoleAssignmentFailure();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertTrue(result.getBody().contains("Error assigning role"));
        verify(usersResource).delete(TEST_USER_ID);
    }

    @Test
    void createUser_WhenEmailSendingFails_ShouldReturnWarningButKeepUser() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        setupUserCreationWithEmailFailure();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertTrue(result.getBody().contains("User created but failed to send setup email"));
        verify(usersResource, never()).delete(anyString());
    }

    @Test
    void createUser_WithNullRole_ShouldSkipRoleAssignment() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        request.setRole(null);
        setupSuccessfulUserCreation();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(clientsResource, never()).findByClientId(anyString());
    }

    @Test
    void createUser_WithEmptyRole_ShouldSkipRoleAssignment() {
        // Arrange
        CreateUserRequest request = createValidUserRequest();
        request.setRole("");
        setupSuccessfulUserCreation();

        // Act
        ResponseEntity<String> result = userService.createUser(request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(clientsResource, never()).findByClientId(anyString());
    }

    @Test
    void getAllUsers_WhenUsersExist_ShouldReturnUserList() {
        // Arrange
        List<UserRepresentation> mockUsers = createMockUserRepresentations();
        setupGetAllUsersSuccess(mockUsers);

        // Act
        List<UserResponseDTO> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        UserResponseDTO firstUser = result.get(0);
        assertEquals("user1", firstUser.getUsername());
        assertEquals("user1@test.com", firstUser.getEmail());
        assertEquals("********", firstUser.getPassword());
    }

    @Test
    void getAllUsers_WhenNoUsersExist_ShouldReturnEmptyList() {
        // Arrange
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(Collections.emptyList());

        // Act
        List<UserResponseDTO> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteUser_WithValidId_ShouldCallKeycloakDelete() {
        // Arrange
        String userId = "user-to-delete";
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(usersResource).delete(userId);
    }

    @Test
    void getUser_WithValidId_ShouldReturnUserDetails() {
        // Arrange
        UserRepresentation mockUser = createMockUserRepresentation("test-id", "testuser");
        setupGetUserSuccess(mockUser);

        // Act
        UserResponseDTO result = userService.getUser("test-id");

        // Assert
        assertNotNull(result);
        assertEquals("test-id", result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@test.com", result.getEmail());
        assertEquals("********", result.getPassword());
    }

    @Test
    void getUser_WhenUserNotFound_ShouldThrowNotFoundException() {
        // Arrange
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException("User not found"));

        // Act & Assert
        assertThrows(NotFoundException.class, () -> userService.getUser("invalid-id"));
    }

    @Test
    void getUser_WhenRoleRetrievalFails_ShouldReturnUserWithoutRole() {
        // Arrange
        UserRepresentation mockUser = createMockUserRepresentation("test-id", "testuser");
        
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("test-id")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(mockUser);
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenThrow(new RuntimeException("Client error"));

        // Act
        UserResponseDTO result = userService.getUser("test-id");

        // Assert
        assertNotNull(result);
        assertNull(result.getRole());
    }

    @Test
    void updatePassword_WithValidIdAndPassword_ShouldUpdateSuccessfully() {
        // Arrange
        String userId = "test-user-id";
        String newPassword = "NewSecurePassword123!";
        UserRepresentation userRep = new UserRepresentation();
        userRep.setRequiredActions(List.of("UPDATE_PASSWORD"));
        
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).resetPassword(any(CredentialRepresentation.class));
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act
        userService.updatePassword(userId, newPassword);

        // Assert
        ArgumentCaptor<CredentialRepresentation> credCaptor = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credCaptor.capture());
        
        CredentialRepresentation capturedCred = credCaptor.getValue();
        assertFalse(capturedCred.isTemporary());
        assertEquals(CredentialRepresentation.PASSWORD, capturedCred.getType());
        assertEquals(newPassword, capturedCred.getValue());
        
        ArgumentCaptor<UserRepresentation> userRepCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(userResource).update(userRepCaptor.capture());
        assertTrue(userRepCaptor.getValue().getRequiredActions().isEmpty());
    }

    @Test
    void updatePassword_WhenUserNotFound_ShouldThrowNotFoundException() {
        // Arrange
        String userId = "non-existent-user";
        String newPassword = "NewPassword123!";
        
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException("User not found"));

        // Act & Assert
        assertThrows(NotFoundException.class, () -> userService.updatePassword(userId, newPassword));
    }

    @Test
    void updatePassword_WithEmptyPassword_ShouldStillExecute() {
        // Arrange
        String userId = "test-user-id";
        String emptyPassword = "";
        UserRepresentation userRep = new UserRepresentation();
        
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).resetPassword(any(CredentialRepresentation.class));

        // Act & Assert
        assertDoesNotThrow(() -> userService.updatePassword(userId, emptyPassword));
        verify(userResource).resetPassword(any(CredentialRepresentation.class));
    }

    // Helper methods for setting up test scenarios

    private CreateUserRequest createValidUserRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        return request;
    }

    private UserRepresentation createMockUserRepresentation(String id, String username) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        user.setUsername(username);
        user.setEmail("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private List<UserRepresentation> createMockUserRepresentations() {
        UserRepresentation user1 = createMockUserRepresentation("id1", "user1");
        user1.setEmail("user1@test.com");
        
        UserRepresentation user2 = createMockUserRepresentation("id2", "user2");
        user2.setEmail("user2@test.com");
        
        return List.of(user1, user2);
    }

    private void setupSuccessfulUserCreation() {
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://test.com/users/" + TEST_USER_ID));
        
        doNothing().when(emailService).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    private void setupSuccessfulUserCreationWithRole() {
        setupSuccessfulUserCreation();
        
        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);
        
        RoleRepresentation role = new RoleRepresentation();
        role.setName("ADMIN");
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));
        when(clientsResource.get(TEST_CLIENT_ID)).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("ADMIN")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(role);
        
        when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(TEST_CLIENT_ID)).thenReturn(roleScopeResource);
        doNothing().when(roleScopeResource).add(anyList());
    }

    private void setupUserCreationWithInvalidRole() {
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://test.com/users/" + TEST_USER_ID));
        
        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));
        when(clientsResource.get(TEST_CLIENT_ID)).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("INVALID_ROLE")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(null);
    }

    private void setupUserCreationWithRoleAssignmentFailure() {
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://test.com/users/" + TEST_USER_ID));
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenThrow(new RuntimeException("Role assignment failed"));
    }

    private void setupUserCreationWithEmailFailure() {
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://test.com/users/" + TEST_USER_ID));
        
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailService).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    private void setupGetAllUsersSuccess(List<UserRepresentation> mockUsers) {
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(mockUsers);
        
        // Setup for role retrieval in mapToDto
        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);
        
        RoleRepresentation role = new RoleRepresentation();
        role.setName("USER");
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));
        
        for (UserRepresentation user : mockUsers) {
            when(usersResource.get(user.getId())).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.clientLevel(TEST_CLIENT_ID)).thenReturn(roleScopeResource);
            when(roleScopeResource.listAll()).thenReturn(List.of(role));
        }
    }

    private void setupGetUserSuccess(UserRepresentation mockUser) {
        ClientRepresentation client = new ClientRepresentation();
        client.setId(TEST_CLIENT_ID);
        
        RoleRepresentation role = new RoleRepresentation();
        role.setName("USER");
        
        when(keycloak.realm(TEST_REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(mockUser.getId())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(mockUser);
        
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("spring-boot-app")).thenReturn(List.of(client));
        
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(TEST_CLIENT_ID)).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of(role));
    }
}
