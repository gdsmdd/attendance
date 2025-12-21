package com.qrcode.attendance.controller;

import com.qrcode.attendance.config.QrCodeConfig;
import com.qrcode.attendance.entity.*;
import com.qrcode.attendance.repository.*;
import com.qrcode.attendance.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final TeacherRepository teacherRepository;
    private final TeachingClassService teachingClassService;
    private final AttendanceService attendanceService;
    private final QRCodeService qrCodeService;
    private final TeachingClassRepository teachingClassRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final StudentRepository studentRepository;
    private final TeachingClassStudentRepository teachingClassStudentRepository;

    // 教师仪表板
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;

        // 统计数据
        long teachingClassCount = teachingClassRepository.countByTeacher(teacher);
        List<TeachingClass> teachingClasses = teachingClassService.getTeachingClassesByTeacher(teacher);

        long totalStudents = teachingClasses.stream()
                .mapToLong(tc -> teachingClassStudentRepository.countActiveStudentsByTeachingClass(tc))
                .sum();

        // 获取进行中的签到
        List<AttendanceSession> activeSessions = attendanceSessionRepository
                .findByTeacherIdAndStatus(teacher.getId(), "STARTED");

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClassCount", teachingClassCount);
        model.addAttribute("teachingClasses", teachingClasses);
        model.addAttribute("activeSessions", activeSessions);
        model.addAttribute("activeSessionCount", activeSessions.size());
        model.addAttribute("totalStudents", totalStudents);

        return "teacher/dashboard";
    }

    // ========== 教学班管理 ==========

    // 教学班列表
    @GetMapping("/teaching-classes")
    public String teachingClassList(Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;

        List<TeachingClass> teachingClasses = Optional.ofNullable(
                teachingClassService.getTeachingClassesByTeacher(teacher)
        ).orElse(Collections.emptyList());

        Map<Long, Integer> totalStudentCountMap = new HashMap<>(); // 先初始化空Map

        // 只有当有教学班时才统计
        if (!teachingClasses.isEmpty()) {
            totalStudentCountMap = attendanceService.getTotalStudentCountByClasses(teachingClasses);
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClasses", teachingClasses);
        model.addAttribute("totalStudentCount", totalStudentCountMap); // 确保这里名称一致

        // 添加调试日志
        log.info("教学班数量: {}", teachingClasses.size());
        log.info("学生人数统计Map: {}", totalStudentCountMap);

        return "teacher/teaching-class-list";
    }

    // 添加教学班页面
    @GetMapping("/teaching-classes/add")
    public String addTeachingClassPage(Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        model.addAttribute("teacher", teacher);

        return "teacher/teaching-class-add";
    }

    // 添加教学班
    @PostMapping("/teaching-classes/add")
    public String addTeachingClass(
            @RequestParam String className,
            @RequestParam String courseName,
            @RequestParam(required = false) String semester,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = (Teacher) user;

            // 创建教学班
            TeachingClass teachingClass = new TeachingClass();
            teachingClass.setClassName(className);
            teachingClass.setCourseName(courseName);
            teachingClass.setSemester(semester);
            teachingClass.setTeacher(teacher);
            teachingClass.setIsActive(true);

            teachingClassService.createTeachingClass(teachingClass);

            redirectAttributes.addFlashAttribute("success", "教学班创建成功！");
            return "redirect:/teacher/teaching-classes";

        } catch (Exception e) {
            log.error("创建教学班失败", e);
            redirectAttributes.addFlashAttribute("error", "创建失败：" + e.getMessage());
            return "redirect:/teacher/teaching-classes/add";
        }
    }

    // 编辑教学班页面
    @GetMapping("/teaching-classes/edit/{id}")
    public String editTeachingClassPage(@PathVariable Long id, Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        TeachingClass teachingClass = teachingClassRepository.findByIdAndTeacher(id, teacher)
                .orElseThrow(() -> new RuntimeException("教学班不存在或无权访问"));

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClass", teachingClass);

        return "teacher/teaching-class-edit";
    }

    // 更新教学班
    @PostMapping("/teaching-classes/update")
    public String updateTeachingClass(
            @RequestParam Long id,
            @RequestParam String className,
            @RequestParam String courseName,
            @RequestParam(required = false) String semester,
            @RequestParam Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = (Teacher) user;

            TeachingClass teachingClass = new TeachingClass();
            teachingClass.setClassName(className);
            teachingClass.setCourseName(courseName);
            teachingClass.setSemester(semester);
            teachingClass.setIsActive(isActive);
            teachingClass.setTeacher(teacher);

            teachingClassService.updateTeachingClass(id, teachingClass);

            redirectAttributes.addFlashAttribute("success", "教学班更新成功！");
            return "redirect:/teacher/teaching-classes";

        } catch (Exception e) {
            log.error("更新教学班失败", e);
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
            return "redirect:/teacher/teaching-classes/edit/" + id;
        }
    }

    // 删除教学班
    @GetMapping("/teaching-classes/delete/{id}")
    public String deleteTeachingClass(@PathVariable Long id, HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            teachingClassService.deleteTeachingClass(id);
            redirectAttributes.addFlashAttribute("success", "教学班删除成功！");

        } catch (Exception e) {
            log.error("删除教学班失败", e);
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }

        return "redirect:/teacher/teaching-classes";
    }

    // ========== 教学班学生管理 ==========

    // 查看教学班学生
    @GetMapping("/teaching-classes/{id}/students")
    public String teachingClassStudents(@PathVariable Long id, Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        TeachingClass teachingClass = teachingClassRepository.findByIdAndTeacher(id, teacher)
                .orElseThrow(() -> new RuntimeException("教学班不存在或无权访问"));

        List<Student> students = teachingClassService.getStudentsInTeachingClass(teachingClass);

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClass", teachingClass);
        model.addAttribute("students", students);

        return "teacher/teaching-class-students";
    }

    // 添加学生到教学班页面
    @GetMapping("/teaching-classes/{id}/students/add")
    public String addStudentToTeachingClassPage(@PathVariable Long id,
                                                @RequestParam(required = false) String keyword,
                                                Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        TeachingClass teachingClass = teachingClassRepository.findByIdAndTeacher(id, teacher)
                .orElseThrow(() -> new RuntimeException("教学班不存在或无权访问"));

        List<Student> availableStudents = teachingClassService
                .searchStudentsNotInTeachingClass(teachingClass, keyword);

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClass", teachingClass);
        model.addAttribute("availableStudents", availableStudents);
        model.addAttribute("keyword", keyword);

        return "teacher/teaching-class-add-student";
    }

    // 添加学生到教学班
    @PostMapping("/teaching-classes/{classId}/students/add")
    public String addStudentToTeachingClass(
            @PathVariable Long classId,
            @RequestParam Long studentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            teachingClassService.addStudentToTeachingClass(classId, studentId);

            redirectAttributes.addFlashAttribute("success", "学生添加成功！");
            return "redirect:/teacher/teaching-classes/" + classId + "/students";

        } catch (Exception e) {
            log.error("添加学生失败", e);
            redirectAttributes.addFlashAttribute("error", "添加失败：" + e.getMessage());
            return "redirect:/teacher/teaching-classes/" + classId + "/students/add";
        }
    }

    // 从教学班移除学生
    @GetMapping("/teaching-classes/{classId}/students/remove/{studentId}")
    public String removeStudentFromTeachingClass(
            @PathVariable Long classId,
            @PathVariable Long studentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            teachingClassService.removeStudentFromTeachingClass(classId, studentId);

            redirectAttributes.addFlashAttribute("success", "学生移除成功！");

        } catch (Exception e) {
            log.error("移除学生失败", e);
            redirectAttributes.addFlashAttribute("error", "移除失败：" + e.getMessage());
        }

        return "redirect:/teacher/teaching-classes/" + classId + "/students";
    }

    // ========== 签到管理 ==========

    // 签到会话列表
    @GetMapping("/attendance/sessions")
    public String attendanceSessionList(
            @RequestParam(required = false) Long classId,
            Model model, HttpSession session) {

        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;

        List<TeachingClass> teachingClasses = teachingClassService.getTeachingClassesByTeacher(teacher);
        List<AttendanceSession> sessions = new ArrayList<>();

        if (classId != null) {
            TeachingClass teachingClass = teachingClassRepository.findByIdAndTeacher(classId, teacher)
                    .orElseThrow(() -> new RuntimeException("教学班不存在或无权访问"));
            sessions = attendanceService.getAttendanceSessionsByTeachingClass(teachingClass);
            model.addAttribute("selectedClass", teachingClass);
        } else if (!teachingClasses.isEmpty()) {
            sessions = attendanceService.getAttendanceSessionsByTeachingClass(teachingClasses.get(0));
            if (!teachingClasses.isEmpty()) {
                model.addAttribute("selectedClass", teachingClasses.get(0));
            }
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClasses", teachingClasses);
        model.addAttribute("sessions", sessions);

        return "teacher/attendance-session-list";
    }

    // 创建签到会话页面
    @GetMapping("/attendance/sessions/add")
    public String addAttendanceSessionPage(
            @RequestParam Long classId,
            Model model, HttpSession session) {

        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        TeachingClass teachingClass = teachingClassRepository.findByIdAndTeacher(classId, teacher)
                .orElseThrow(() -> new RuntimeException("教学班不存在或无权访问"));

        model.addAttribute("teacher", teacher);
        model.addAttribute("teachingClass", teachingClass);

        // 设置默认时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        String defaultStartTime = LocalDateTime.now().format(formatter);
        String defaultEndTime = LocalDateTime.now().plusHours(1).format(formatter);

        model.addAttribute("defaultStartTime", defaultStartTime);
        model.addAttribute("defaultEndTime", defaultEndTime);

        return "teacher/attendance-session-add";
    }

    // 创建签到会话
    @PostMapping("/attendance/sessions/add")
    public String addAttendanceSession(
            @RequestParam Long teachingClassId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String startTimeStr,
            @RequestParam String endTimeStr,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = (Teacher) user;
            TeachingClass teachingClass = teachingClassRepository.findByIdAndTeacher(teachingClassId, teacher)
                    .orElseThrow(() -> new RuntimeException("教学班不存在或无权访问"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 检查时间
            if (endTime.isBefore(startTime)) {
                throw new RuntimeException("结束时间不能早于开始时间");
            }

            if (startTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("开始时间不能早于当前时间");
            }

            // 创建签到会话
            AttendanceSession attendanceSession = new AttendanceSession();
            attendanceSession.setTeachingClass(teachingClass);
            attendanceSession.setTitle(title);
            attendanceSession.setDescription(description);
            attendanceSession.setStartTime(startTime);
            attendanceSession.setEndTime(endTime);
            attendanceSession.setStatus("CREATED");

            attendanceService.createAttendanceSession(attendanceSession);

            redirectAttributes.addFlashAttribute("success", "签到会话创建成功！");
            return "redirect:/teacher/attendance/sessions?classId=" + teachingClassId;

        } catch (Exception e) {
            log.error("创建签到会话失败", e);
            redirectAttributes.addFlashAttribute("error", "创建失败：" + e.getMessage());
            return "redirect:/teacher/attendance/sessions/add?classId=" + teachingClassId;
        }
    }

    // 开始签到
    @GetMapping("/attendance/sessions/{id}/start")
    public String startAttendanceSession(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = (Teacher) user;
            AttendanceSession attendanceSession = attendanceSessionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("签到会话不存在"));

            // 检查权限
            if (!attendanceSession.getTeachingClass().getTeacher().getId().equals(teacher.getId())) {
                throw new RuntimeException("无权操作此签到会话");
            }

            attendanceService.startAttendanceSession(id);

            redirectAttributes.addFlashAttribute("success", "签到已开始！");

        } catch (Exception e) {
            log.error("开始签到失败", e);
            redirectAttributes.addFlashAttribute("error", "开始签到失败：" + e.getMessage());
        }

        return "redirect:/teacher/attendance/sessions?classId=" +
                attendanceSessionRepository.findById(id).get().getTeachingClass().getId();
    }

    // 结束签到
    @GetMapping("/attendance/sessions/{id}/end")
    public String endAttendanceSession(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = (Teacher) user;
            AttendanceSession attendanceSession = attendanceSessionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("签到会话不存在"));

            // 检查权限
            if (!attendanceSession.getTeachingClass().getTeacher().getId().equals(teacher.getId())) {
                throw new RuntimeException("无权操作此签到会话");
            }

            attendanceService.endAttendanceSession(id);

            redirectAttributes.addFlashAttribute("success", "签到已结束！");

        } catch (Exception e) {
            log.error("结束签到失败", e);
            redirectAttributes.addFlashAttribute("error", "结束签到失败：" + e.getMessage());
        }

        return "redirect:/teacher/attendance/sessions?classId=" +
                attendanceSessionRepository.findById(id).get().getTeachingClass().getId();
    }

    // 查看签到详情和统计
    @GetMapping("/attendance/sessions/{id}/details")
    public String attendanceSessionDetails(
            @PathVariable Long id,
            Model model, HttpSession session) {

        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        AttendanceSession attendanceSession = attendanceSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("签到会话不存在"));

        // 检查权限
        if (!attendanceSession.getTeachingClass().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("无权查看此签到会话");
        }

        // 获取统计信息
        var statistics = attendanceService.getAttendanceStatistics(id);

        model.addAttribute("attendanceSession", attendanceSession);
        model.addAttribute("totalStudentCount", statistics.get("totalStudentCount"));
        model.addAttribute("signInCount", statistics.get("presentCount"));
        model.addAttribute("lateCount", statistics.get("lateCount"));
        model.addAttribute("absentCount", statistics.get("absentCount"));
        // 3. 传入签到记录列表（前端需要循环显示）
        model.addAttribute("signInRecords", attendanceService.getAttendanceRecordsBySessionId(id));


        model.addAttribute("teacher", teacher);
        model.addAttribute("attendanceSession", attendanceSession);
        model.addAttribute("statistics", statistics);

        return "teacher/attendance-session-details";
    }

    private final QrCodeConfig qrCodeConfig;
    // 显示签到二维码页面
    @GetMapping("/attendance/sessions/{id}/qr-code")
    public String showQRCode(
            @PathVariable Long id,
            Model model, HttpSession session) {

        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = (Teacher) user;
        AttendanceSession attendanceSession = null;

        try {
            // 1. 获取签到会话（强制校验）
            attendanceSession = attendanceSessionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("签到会话不存在 [ID: " + id + "]"));

            // 2. 关联关系校验
            if (attendanceSession.getTeachingClass() == null) {
                throw new RuntimeException("签到会话未关联教学班");
            }
            if (attendanceSession.getTeachingClass().getTeacher() == null) {
                throw new RuntimeException("教学班未关联教师");
            }

            // 3. 权限校验
            if (!Objects.equals(attendanceSession.getTeachingClass().getTeacher().getId(), teacher.getId())) {
                throw new RuntimeException("无权限操作此签到会话（非本人创建）");
            }

            // 4. 状态校验（放宽：即使未开始也能显示二维码，方便测试）
            if (!Arrays.asList("STARTED", "CREATED").contains(attendanceSession.getStatus())) {
                model.addAttribute("warn", "签到尚未开始/已结束，二维码可能无效");
                // 不抛异常，仅提示，方便测试二维码显示
            }
            // 5. 获取配置的baseUrl
            String baseUrl = qrCodeConfig.getBaseUrl();
            String attendanceCode = attendanceSession.getAttendanceCode();
            String attendanceUrl = baseUrl + "/attendance/signin?code=" + attendanceCode;

            // 打印关键日志，定位二维码生成问题
            log.info("开始生成二维码 - 签到码: {}, 签到URL: {}",
                    attendanceCode, attendanceUrl);
            String qrCodeBase64 = qrCodeService.generateAttendanceQRCode(attendanceCode, baseUrl);

            // 6. 校验二维码生成结果（日志+异常）
            if (qrCodeBase64 == null || qrCodeBase64.isEmpty()) {
                log.error("二维码生成失败 - 返回空值 | 签到码: {}", attendanceCode);
                throw new RuntimeException("二维码生成服务返回空值，请检查生成逻辑");
            }
            log.info("二维码生成成功 - Base64长度: {}", qrCodeBase64.length());

            // 7. 数据库存储
            attendanceSession.setQrCodeUrl("data:image/png;base64," + qrCodeBase64);
            attendanceSessionRepository.save(attendanceSession);

            // 8. 统一参数传递（只传必要参数，避免混淆）
            model.addAttribute("teacher", teacher);
            model.addAttribute("attendanceSession", attendanceSession); // 主对象
            model.addAttribute("qrCodeBase64", qrCodeBase64); // 纯Base64（前端拼前缀）
            model.addAttribute("qrCodeUrl", "data:image/png;base64," + qrCodeBase64); // 可直接渲染的URL
            model.addAttribute("attendanceUrl", attendanceUrl); // 签到链接（前端JS生成二维码用）
            model.addAttribute("attendanceCode", attendanceCode);

            return "teacher/attendance-qr-code";

        } catch (Exception e) {
            log.error("显示二维码页面失败 - sessionId: {}, teacherId: {}", id, teacher.getId(), e);
            // 异常时返回二维码页面，携带错误信息（而非跳转到error页，方便调试）
            model.addAttribute("teacher", teacher);
            model.addAttribute("attendanceSession", attendanceSession);
            model.addAttribute("errorMsg", "二维码加载失败：" + e.getMessage());
            model.addAttribute("attendanceCode", attendanceSession != null ? attendanceSession.getAttendanceCode() : "");
            // 仍返回二维码页面，而非error页，方便前端调试
            return "teacher/attendance-qr-code";
        }
    }

    private final AttendanceRecordRepository attendanceRecordRepository;
    // 取消学生签到
    @GetMapping("/attendance/sessions/{sessionId}/cancel/{recordId}")
    public String cancelAttendanceRecord(
            @PathVariable Long sessionId,
            @PathVariable Long recordId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"TEACHER".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = (Teacher) user;

            // 1. 验证签到记录是否存在
            AttendanceRecord record = attendanceRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("签到记录不存在"));

            // 2. 验证签到会话（通过sessionId参数，更可靠）
            AttendanceSession attendanceSession = attendanceSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("签到会话不存在"));

            // 3. 验证记录是否属于这个会话（可选但推荐）
            if (record.getAttendanceSession() != null &&
                    !record.getAttendanceSession().getId().equals(sessionId)) {
                throw new RuntimeException("签到记录不属于当前签到会话");
            }

            // 4. 验证权限（是否是该教师的签到会话）
            if (attendanceSession.getTeachingClass() == null ||
                    !attendanceSession.getTeachingClass().getTeacher().getId().equals(teacher.getId())) {
                throw new RuntimeException("无权限操作此签到记录");
            }

            // 5. 检查签到会话状态（如果已结束，给出提示但不阻止）
            if ("ENDED".equals(attendanceSession.getStatus())) {
                log.warn("取消已结束的签到会话中的记录 - sessionId: {}, recordId: {}", sessionId, recordId);
                // 可以添加确认提示，但这里我们允许取消
            }

            // 6. 删除签到记录
            attendanceRecordRepository.delete(record);

            // 7. 更新学生最后签到时间（可选）
            Student student = record.getStudent();
            if (student != null) {
                // 可以清除最后签到时间，或者设置为null
                student.setLastSigninTime(null);
                studentRepository.save(student);
            }

            log.info("取消签到成功 - 教师: {}, 学生: {}, 签到记录ID: {}",
                    teacher.getName(), student != null ? student.getName() : "未知", recordId);

            redirectAttributes.addFlashAttribute("success", "已成功取消该学生的签到");

        } catch (Exception e) {
            log.error("取消签到失败 - sessionId: {}, recordId: {}", sessionId, recordId, e);
            redirectAttributes.addFlashAttribute("error", "取消签到失败：" + e.getMessage());
        }

        // 返回签到详情页面
        return "redirect:/teacher/attendance/sessions/" + sessionId + "/details";
    }
}