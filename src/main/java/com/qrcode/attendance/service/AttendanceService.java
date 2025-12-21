package com.qrcode.attendance.service;

import com.qrcode.attendance.entity.*;
import com.qrcode.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final TeachingClassStudentRepository teachingClassStudentRepository;
    private final StudentRepository studentRepository;
    @Transactional
    public AttendanceSession createAttendanceSession(AttendanceSession session) {
        // 生成签到码（6位随机数字）
        String attendanceCode = generateAttendanceCode();
        session.setAttendanceCode(attendanceCode);
        session.setStatus("CREATED");

        return attendanceSessionRepository.save(session);
    }

    @Transactional
    public AttendanceSession startAttendanceSession(Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("签到会话不存在"));

        if (!"CREATED".equals(session.getStatus())) {
            throw new RuntimeException("签到会话状态不正确");
        }

        session.setStatus("STARTED");
        session.setStartTime(LocalDateTime.now());

        return attendanceSessionRepository.save(session);
    }

    @Transactional
    public AttendanceSession endAttendanceSession(Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("签到会话不存在"));

        if (!"STARTED".equals(session.getStatus())) {
            throw new RuntimeException("签到会话状态不正确");
        }

        session.setStatus("ENDED");
        session.setEndTime(LocalDateTime.now());

        return attendanceSessionRepository.save(session);
    }

    @Transactional
    public AttendanceRecord signIn(String attendanceCode, String studentId) {
        // 查找签到会话
        AttendanceSession session = attendanceSessionRepository.findByAttendanceCode(attendanceCode)
                .orElseThrow(() -> new RuntimeException("签到码无效"));

        // 检查签到状态
        if (!"STARTED".equals(session.getStatus())) {
            throw new RuntimeException("签到未开始或已结束");
        }

        if (LocalDateTime.now().isAfter(session.getEndTime())) {
            session.setStatus("ENDED");
            attendanceSessionRepository.save(session);
            throw new RuntimeException("签到已结束");
        }

        // 查找学生
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        // 检查学生是否在该教学班中
        boolean isInClass = teachingClassStudentRepository.existsByTeachingClassAndStudent(
                session.getTeachingClass(), student);

        if (!isInClass) {
            throw new RuntimeException("您不在该课程中，无法签到");
        }

        // 检查是否已签到
        Optional<AttendanceRecord> existingRecord = attendanceRecordRepository
                .findByAttendanceSessionAndStudent(session, student);

        if (existingRecord.isPresent()) {
            throw new RuntimeException("您已经签到过了");
        }

        // 创建签到记录
        AttendanceRecord record = new AttendanceRecord();
        record.setAttendanceSession(session);
        record.setStudent(student);
        record.setSigninTime(LocalDateTime.now());

        // 判断是否迟到
        LocalDateTime startTime = session.getStartTime();
        long minutesLate = java.time.Duration.between(startTime, record.getSigninTime()).toMinutes();
        if (minutesLate > 5) { // 超过5分钟算迟到
            record.setStatus("LATE");
        } else {
            record.setStatus("PRESENT");
        }

        // 更新学生的最后签到时间
        student.setLastSigninTime(LocalDateTime.now());
        studentRepository.save(student);

        return attendanceRecordRepository.save(record);
    }

    public Map<String, Object> getAttendanceStatistics(Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("签到会话不存在"));

        // 获取教学班中的学生
        List<Student> studentsInClass = teachingClassStudentRepository
                .findActiveStudentsByTeachingClass(session.getTeachingClass());

        // 获取已签到的记录
        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository
                .findByAttendanceSession(session);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudentCount", studentsInClass.size());
        stats.put("presentCount", attendanceRecordRepository.countPresentByAttendanceSession(session));
        stats.put("lateCount", attendanceRecordRepository.countLateByAttendanceSession(session));
        stats.put("absentCount", studentsInClass.size() - attendanceRecords.size());
        stats.put("attendanceRate", studentsInClass.size() > 0 ?
                (double) attendanceRecords.size() / studentsInClass.size() * 100 : 0);

        // 详细记录
        List<Map<String, Object>> detailedRecords = new ArrayList<>();
        for (Student student : studentsInClass) {
            Map<String, Object> record = new HashMap<>();
            record.put("studentId", student.getStudentId());
            record.put("name", student.getName());
            record.put("className", student.getClassName());

            Optional<AttendanceRecord> ar = attendanceRecords.stream()
                    .filter(r -> r.getStudent().getId().equals(student.getId()))
                    .findFirst();

            if (ar.isPresent()) {
                record.put("status", ar.get().getStatus());
                record.put("signinTime", ar.get().getSigninTime());
                record.put("isSigned", true);
            } else {
                record.put("status", "ABSENT");
                record.put("signinTime", null);
                record.put("isSigned", false);
            }

            detailedRecords.add(record);
        }

        stats.put("detailedRecords", detailedRecords);

        return stats;
    }

    private String generateAttendanceCode() {
        // 生成6位随机数字
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }


    public List<AttendanceSession> getActiveSessions() {
        return attendanceSessionRepository.findActiveSessions(LocalDateTime.now());
    }

    public List<AttendanceSession> getAttendanceSessionsByTeachingClass(TeachingClass teachingClass) {
        return attendanceSessionRepository.findByTeachingClassOrderByStartTimeDesc(teachingClass);
    }

    public List<AttendanceRecord> getAttendanceRecordsBySessionId(Long sessionId) {
        // 假设AttendanceRecord实体中有attendanceSession字段关联签到会话
        return attendanceRecordRepository.findByAttendanceSessionId(sessionId);
    }

    public Map<Long, Integer> getTotalStudentCountByClasses(List<TeachingClass> teachingClasses) {
        Map<Long, Integer> totalStudentCountMap = new HashMap<>();

        for (TeachingClass tc : teachingClasses) {
            // 查询每个教学班的有效学生数（status=ACTIVE）
            List<TeachingClassStudent> students = teachingClassStudentRepository
                    .findByTeachingClassAndStatus(tc, "ACTIVE"); // 替换为你的实际查询方法

            totalStudentCountMap.put(tc.getId(), students.size());
        }
        return totalStudentCountMap;
    }
}