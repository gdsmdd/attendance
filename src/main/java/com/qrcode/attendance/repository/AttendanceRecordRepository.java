package com.qrcode.attendance.repository;

import com.qrcode.attendance.entity.AttendanceRecord;
import com.qrcode.attendance.entity.AttendanceSession;
import com.qrcode.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByAttendanceSession(AttendanceSession attendanceSession);
    Optional<AttendanceRecord> findByAttendanceSessionAndStudent(AttendanceSession attendanceSession, Student student);
    boolean existsByAttendanceSessionAndStudent(AttendanceSession attendanceSession, Student student);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.attendanceSession = :attendanceSession AND ar.status = 'PRESENT'")
    long countPresentByAttendanceSession(@Param("attendanceSession") AttendanceSession attendanceSession);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.attendanceSession = :attendanceSession AND ar.status = 'LATE'")
    long countLateByAttendanceSession(@Param("attendanceSession") AttendanceSession attendanceSession);

    // 查询指定会话的所有签到记录
    List<AttendanceRecord> findByAttendanceSessionId(Long sessionId);
}