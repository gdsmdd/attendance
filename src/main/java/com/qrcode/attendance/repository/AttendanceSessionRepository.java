package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.AttendanceSession;
import com.qrcode.attendance.entity.TeachingClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByTeachingClassOrderByStartTimeDesc(TeachingClass teachingClass);

    @Query("SELECT a FROM AttendanceSession a WHERE a.teachingClass.teacher.id = :teacherId AND a.status = :status")
    List<AttendanceSession> findByTeacherIdAndStatus(@Param("teacherId") Long teacherId, @Param("status") String status);

    Optional<AttendanceSession> findByAttendanceCode(String attendanceCode);

    @Query("SELECT a FROM AttendanceSession a WHERE a.status = 'STARTED' AND a.endTime > :now")
    List<AttendanceSession> findActiveSessions(@Param("now") LocalDateTime now);
}