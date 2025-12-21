package com.qrcode.attendance.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Data
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession attendanceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "signin_time", nullable = false)
    private LocalDateTime signinTime; // 签到时间

    @Column(name = "status", nullable = false)
    private String status = "PRESENT"; // 状态：PRESENT, LATE, ABSENT

    @Column(name = "remark", length = 500)
    private String remark; // 备注
}