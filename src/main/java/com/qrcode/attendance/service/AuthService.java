package com.qrcode.attendance.service;

import com.qrcode.attendance.entity.Admin;
import com.qrcode.attendance.entity.Teacher;
import com.qrcode.attendance.repository.AdminRepository;
import com.qrcode.attendance.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public Object login(String username, String password, String userType) {
        try {
            if ("ADMIN".equalsIgnoreCase(userType)) {
                return authenticateAdmin(username, password);
            } else if ("TEACHER".equalsIgnoreCase(userType)) {
                return authenticateTeacher(username, password);
            }
            return null;
        } catch (Exception e) {
            log.error("登录验证失败 - 用户名: {}, 类型: {}", username, userType, e);
            return null;
        }
    }

    private Object authenticateAdmin(String username, String password) {
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);

        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();

            // 简单的密码验证（实际项目中应该使用加密验证）
            if (password.equals(admin.getPassword())) {
                // 检查账号状态
                if (!isAccountActive(admin)) {
                    log.warn("管理员账号被禁用 - 用户名: {}", username);
                    return null;
                }
                return admin;
            }
        }

        return null;
    }

    private Object authenticateTeacher(String username, String password) {
        Optional<Teacher> teacherOpt = teacherRepository.findByUsername(username);

        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();

            // 简单的密码验证
            if (password.equals(teacher.getPassword())) {
                // 检查账号状态
                if (!isAccountActive(teacher)) {
                    log.warn("教师账号被禁用 - 用户名: {}", username);
                    return null;
                }
                return teacher;
            }
        }

        return null;
    }

    private boolean isAccountActive(Admin admin) {
        // 管理员总是活跃的，可以添加其他检查逻辑
        return true;
    }

    private boolean isAccountActive(Teacher teacher) {
        return "ACTIVE".equals(teacher.getStatus());
    }

    @Transactional(readOnly = true)
    public boolean checkUsernameExists(String username, String userType) {
        if ("ADMIN".equals(userType)) {
            return adminRepository.existsByUsername(username);
        } else if ("TEACHER".equals(userType)) {
            return teacherRepository.existsByUsername(username);
        }
        return false;
    }
}