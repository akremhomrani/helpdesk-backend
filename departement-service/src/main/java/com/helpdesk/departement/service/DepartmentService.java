package com.helpdesk.departement.service;

import com.helpdesk.departement.dto.CreateDepartmentRequest;
import com.helpdesk.departement.dto.DepartmentDTO;
import com.helpdesk.departement.dto.UpdateDepartmentRequest;

import java.util.List;

public interface DepartmentService {
    List<DepartmentDTO> getAll();
    DepartmentDTO getById(String id);
    DepartmentDTO create(CreateDepartmentRequest request);
    DepartmentDTO update(String id, UpdateDepartmentRequest request);
    void delete(String id);
    void addUserToDepartment(String departmentId, String userId);
    void removeUserFromDepartment(String departmentId, String userId);
    String getDepartmentNameByUserId(String userId);
}

