package com.qrcode.attendance.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
@Data
@EqualsAndHashCode(callSuper = true)
public class Student extends User {

    @Column(name = "student_id", unique = true, nullable = false, length = 20)
    private String studentId; // 学号

    @Column(name = "class_name", nullable = false, length = 50)
    private String className; // 行政班级

    @Column(name = "enrollment_year", nullable = false, length = 4)
    private String enrollmentYear; // 入学年份

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // 状态：ACTIVE, INACTIVE, GRADUATED

    @Column(name = "last_signin_time")
    private LocalDateTime lastSigninTime; // 最后签到时间
}