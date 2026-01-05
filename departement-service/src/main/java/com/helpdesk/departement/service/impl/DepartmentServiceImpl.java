package com.helpdesk.departement.service.impl;

import com.helpdesk.departement.dto.CreateDepartmentRequest;
import com.helpdesk.departement.dto.DepartmentDTO;
import com.helpdesk.departement.dto.UpdateDepartmentRequest;
import com.helpdesk.departement.entity.Department;
import com.helpdesk.departement.exception.ResourceNotFoundException;
import com.helpdesk.departement.repository.DepartmentRepository;
import com.helpdesk.departement.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;

    @Override
    public List<DepartmentDTO> getAll() {
        return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public DepartmentDTO getById(String id) {
        Department d = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return toDto(d);
    }

    @Override
    public DepartmentDTO create(CreateDepartmentRequest request) {
        Department d = new Department();
        d.setName(request.getName());
        d.setDescription(request.getDescription());
        d.setCreatedAt(java.time.LocalDateTime.now());
        Department saved = repository.save(d);
        return toDto(saved);
    }

    @Override
    public DepartmentDTO update(String id, UpdateDepartmentRequest request) {
        Department d = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        if (request.getName() != null) d.setName(request.getName());
        if (request.getDescription() != null) d.setDescription(request.getDescription());
        Department updated = repository.save(d);
        return toDto(updated);
    }

    @Override
    public void delete(String id) {
        Department d = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        repository.delete(d);
    }

    @Override
    public void addUserToDepartment(String departmentId, String userId) {
        Department d = repository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        
        // Initialize userIds list if null (for old departments)
        if (d.getUserIds() == null) {
            d.setUserIds(new java.util.ArrayList<>());
        }
        
        if (!d.getUserIds().contains(userId)) {
            d.getUserIds().add(userId);
            repository.save(d);
            System.out.println("Added user " + userId + " to department " + d.getName());
        } else {
            System.out.println("User " + userId + " already in department " + d.getName());
        }
    }

    @Override
    public void removeUserFromDepartment(String departmentId, String userId) {
        Department d = repository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        
        // Initialize userIds list if null (for old departments)
        if (d.getUserIds() == null) {
            d.setUserIds(new java.util.ArrayList<>());
        }
        
        d.getUserIds().remove(userId);
        repository.save(d);
        System.out.println("Removed user " + userId + " from department " + d.getName());
    }

    @Override
    public String getDepartmentNameByUserId(String userId) {
        List<Department> departments = repository.findAll();
        return departments.stream()
                .filter(d -> d.getUserIds() != null && d.getUserIds().contains(userId))
                .findFirst()
                .map(Department::getName)
                .orElse(null);
    }

    private DepartmentDTO toDto(Department d) {
        return new DepartmentDTO(d.getId(), d.getName(), d.getDescription(), d.getUserIds(), d.getCreatedAt());
    }
}