package com.qrcode.attendance.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teaching_class_students",
        uniqueConstraints = @UniqueConstraint(columnNames = {"teaching_class_id", "student_id"}))
@Data
public class TeachingClassStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_class_id", nullable = false)
    private TeachingClass teachingClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "join_time", nullable = false)
    private LocalDateTime joinTime = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // 状态：ACTIVE, DROPPED
}