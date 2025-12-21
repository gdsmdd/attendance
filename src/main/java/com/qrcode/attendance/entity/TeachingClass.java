package com.qrcode.attendance.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teaching_classes")
@Data
public class TeachingClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String className; // 教学班名称，如 "Java程序设计-2024春"

    @Column(nullable = false, length = 50)
    private String courseName; // 课程名称，如 "Java程序设计"

    @Column(length = 20)
    private String semester; // 学期，如 "2024春"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher; // 授课教师

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 是否激活

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}