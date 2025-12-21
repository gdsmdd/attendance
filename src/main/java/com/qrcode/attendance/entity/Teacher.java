package com.qrcode.attendance.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;

@Entity
@Table(name = "teachers")
@DiscriminatorValue("TEACHER")
@Data
@EqualsAndHashCode(callSuper = true)
public class Teacher extends User {
    @Column(name = "teacher_id", unique = true, nullable = false, length = 20)
    private String teacherId; // 工号

    @Column(name = "title")
    private String title; // 职称

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // 状态：ACTIVE, INACTIVE

    @Column(name = "last_login_time")
    private java.time.LocalDateTime lastLoginTime;
}