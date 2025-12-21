package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUsername(String username);
    Optional<Teacher> findByTeacherId(String teacherId);
    boolean existsByTeacherId(String teacherId);
    boolean existsByUsername(String username);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT t FROM Teacher t WHERE t.department.id = :departmentId")
    List<Teacher> findByDepartmentId(@Param("departmentId") Long departmentId);
}