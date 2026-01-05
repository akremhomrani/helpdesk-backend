package com.helpdesk.userservice.controllers;

import com.helpdesk.userservice.dtos.CreateUserRequest;
import com.helpdesk.userservice.dtos.UpdatePasswordRequest;
import com.helpdesk.userservice.dtos.UpdateUserRequest;
import com.helpdesk.userservice.dtos.UserResponseDTO;
import com.helpdesk.userservice.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest req) {
        return userService.createUser(req);
    }

    @GetMapping
    public List<UserResponseDTO> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/department/{departmentId}")
    public List<UserResponseDTO> getUsersByDepartment(@PathVariable String departmentId) {
        return userService.getUsersByDepartment(departmentId);
    }

    @GetMapping("/admins")
    public List<UserResponseDTO> getAdminUsers() {
        return userService.getAdminUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDTO getUser(@PathVariable String id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest req) {
        return userService.updateUser(id, req);
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<String> setPassword(@PathVariable String id, @RequestBody UpdatePasswordRequest req) {
        userService.updatePassword(id, req.getNewPassword());
        return ResponseEntity.ok("Password updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Deleted");
    }
}
