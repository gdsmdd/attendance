package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String name);
    List<Department> findAllByOrderByName();
}