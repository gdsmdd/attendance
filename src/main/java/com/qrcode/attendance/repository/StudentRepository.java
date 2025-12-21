package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
    boolean existsByUsername(String username);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT s FROM Student s WHERE s.department.id = :departmentId")
    List<Student> findByDepartmentId(@Param("departmentId") Long departmentId);

    List<Student> findByClassName(String className);
}