package com.qrcode.attendance.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_sessions")
@Data
public class AttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_class_id", nullable = false)
    private TeachingClass teachingClass;

    @Column(nullable = false, length = 100)
    private String title; // 签到标题，如 "第1次课签到"

    @Column(length = 500)
    private String description; // 签到描述

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime; // 签到开始时间

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime; // 签到结束时间

    @Column(name = "qr_code_url", columnDefinition = "TEXT") // TEXT 类型可存大量文本（MySQL 中约 65535 字符）
    // 或用 VARCHAR(20000)（若数据库支持）：@Column(name = "qr_code_url", length = 20000)
    private String qrCodeUrl;

    @Column(name = "attendance_code", length = 20)
    private String attendanceCode; // 签到码（用于验证）

    @Column(name = "status", nullable = false)
    private String status = "CREATED"; // 状态：CREATED, STARTED, ENDED, CANCELLED

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}