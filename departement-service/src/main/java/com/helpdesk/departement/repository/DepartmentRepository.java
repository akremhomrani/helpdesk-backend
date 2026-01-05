package com.helpdesk.departement.repository;

import com.helpdesk.departement.entity.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepartmentRepository extends MongoRepository<Department, String> {
}
