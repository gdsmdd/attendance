package com.qrcode.attendance.controller;

import com.qrcode.attendance.entity.AttendanceSession;
import com.qrcode.attendance.service.AttendanceService;
import com.qrcode.attendance.service.AttendanceSessionService;
import com.qrcode.attendance.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final QRCodeService qrCodeService;

    // 注入签到会话的 Service/Repository（根据你的项目结构调整）
    @Autowired
    private AttendanceSessionService attendanceSessionService;

    // 学生签到页面
    @GetMapping("/signin")
    public String signinPage(@RequestParam String code, Model model) {
        try {
            // ========== 核心修改1：查询签到会话数据 ==========
            // 根据签到码查询对应的会话（关键！之前缺失这一步）
            AttendanceSession sessionInfo = attendanceSessionService.getByAttendanceCode(code);

            // ========== 核心修改2：传递 sessionInfo 到前端 ==========
            model.addAttribute("sessionInfo", sessionInfo); // 前端依赖这个参数判断是否有效
            model.addAttribute("attendanceCode", code);

            // ========== 核心修改3：空值判断 + 错误提示 ==========
            if (sessionInfo == null) {
                model.addAttribute("error", "签到码【" + code + "】无效，未找到对应的签到信息");
                // 仍返回签到页面，让前端显示无效提示（而非跳转到 error 页面）
                return "attendance/signin";
            }

            // 可选：判断签到状态（未开始/已结束）
            if (!"STARTED".equals(sessionInfo.getStatus())) {
                model.addAttribute("error",
                        "签到状态异常：" + ("ENDED".equals(sessionInfo.getStatus()) ? "签到已结束" : "签到未开始")
                );
            }

            return "attendance/signin";

        } catch (Exception e) {
            log.error("访问签到页面失败", e);
            model.addAttribute("error", "签到链接无效或已过期");
            // 异常时也可返回签到页面，统一提示
            // return "attendance/signin";
            return "attendance/error";
        }
    }

    // 处理签到
    @PostMapping("/signin")
    public String signin(
            @RequestParam String attendanceCode,
            @RequestParam String studentId,
            HttpServletRequest request,
            Model model) {

        try {
            // 获取IP地址和设备信息
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 执行签到
            var record = attendanceService.signIn(attendanceCode, studentId);

            model.addAttribute("success", true);
            model.addAttribute("message", "签到成功！");
            model.addAttribute("studentId", studentId);
            model.addAttribute("signinTime", record.getSigninTime());
            model.addAttribute("status", record.getStatus());

        } catch (Exception e) {
            log.error("签到失败", e);
            model.addAttribute("success", false);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("attendanceCode", attendanceCode);
        }

        return "attendance/signin-result";
    }

    // 获取签到统计信息（可用于API）
    @GetMapping("/statistics/{sessionId}")
    @ResponseBody
    public Map<String, Object> getStatistics(@PathVariable Long sessionId) {
        try {
            return attendanceService.getAttendanceStatistics(sessionId);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}