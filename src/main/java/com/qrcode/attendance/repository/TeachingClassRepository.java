package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.TeachingClass;
import com.qrcode.attendance.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeachingClassRepository extends JpaRepository<TeachingClass, Long> {
    List<TeachingClass> findByTeacherAndIsActiveTrue(Teacher teacher);
    List<TeachingClass> findByTeacher(Teacher teacher);
    Optional<TeachingClass> findByIdAndTeacher(Long id, Teacher teacher);
    boolean existsByClassNameAndTeacher(String className, Teacher teacher);

    @Query("SELECT COUNT(tc) FROM TeachingClass tc WHERE tc.teacher = :teacher")
    long countByTeacher(@Param("teacher") Teacher teacher);
}