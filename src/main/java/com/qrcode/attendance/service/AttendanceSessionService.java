package com.qrcode.attendance.service;
import com.qrcode.attendance.entity.AttendanceSession;
import com.qrcode.attendance.repository.AttendanceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AttendanceSessionService {

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    // 根据签到码查询会话
    public AttendanceSession getByAttendanceCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        // 调用 Repository 方法查询（JPA 写法）
        return attendanceSessionRepository.findByAttendanceCode(code).orElse(null);
    }
}