package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.TeachingClass;
import com.qrcode.attendance.entity.TeachingClassStudent;
import com.qrcode.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeachingClassStudentRepository extends JpaRepository<TeachingClassStudent, Long> {
    List<TeachingClassStudent> findByTeachingClass(TeachingClass teachingClass);
    List<TeachingClassStudent> findByTeachingClassAndStatus(TeachingClass teachingClass, String status);
    Optional<TeachingClassStudent> findByTeachingClassAndStudent(TeachingClass teachingClass, Student student);
    boolean existsByTeachingClassAndStudent(TeachingClass teachingClass, Student student);

    @Query("SELECT tcs.student FROM TeachingClassStudent tcs WHERE tcs.teachingClass = :teachingClass AND tcs.status = 'ACTIVE'")
    List<Student> findActiveStudentsByTeachingClass(@Param("teachingClass") TeachingClass teachingClass);

    @Query("SELECT COUNT(tcs) FROM TeachingClassStudent tcs WHERE tcs.teachingClass = :teachingClass AND tcs.status = 'ACTIVE'")
    long countActiveStudentsByTeachingClass(@Param("teachingClass") TeachingClass teachingClass);

}