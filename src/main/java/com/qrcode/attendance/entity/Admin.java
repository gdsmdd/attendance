package com.qrcode.attendance.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;

@Entity
@Table(name = "admins")
@DiscriminatorValue("ADMIN")
@Data
@EqualsAndHashCode(callSuper = true)
public class Admin extends User {
    @Column(name = "last_login_time")
    private java.time.LocalDateTime lastLoginTime;
}