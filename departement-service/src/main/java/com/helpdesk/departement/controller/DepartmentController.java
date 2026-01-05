package com.helpdesk.departement.controller;

import com.helpdesk.departement.dto.CreateDepartmentRequest;
import com.helpdesk.departement.dto.DepartmentDTO;
import com.helpdesk.departement.dto.UpdateDepartmentRequest;
import com.helpdesk.departement.exception.BadRequestException;
import com.helpdesk.departement.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> getOne(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentDTO created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDTO> update(@PathVariable String id, @RequestBody UpdateDepartmentRequest request) {
        DepartmentDTO updated = service.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{departmentId}/users/{userId}")
    public ResponseEntity<Void> addUserToDepartment(@PathVariable String departmentId, @PathVariable String userId) {
        service.addUserToDepartment(departmentId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{departmentId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromDepartment(@PathVariable String departmentId, @PathVariable String userId) {
        service.removeUserFromDepartment(departmentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/name")
    public ResponseEntity<String> getDepartmentNameByUserId(@PathVariable String userId) {
        String departmentName = service.getDepartmentNameByUserId(userId);
        if (departmentName != null) {
            return ResponseEntity.ok(departmentName);
        }
        return ResponseEntity.notFound().build();
    }

}
