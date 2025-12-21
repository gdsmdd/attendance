package com.qrcode.attendance.controller;

import com.qrcode.attendance.entity.Admin;
import com.qrcode.attendance.entity.Teacher;
import com.qrcode.attendance.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final AuthService authService;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        // 如果已登录，重定向到对应页面
        if (session.getAttribute("user") != null) {
            String userType = (String) session.getAttribute("userType");
            if ("ADMIN".equals(userType)) {
                return "redirect:/admin/dashboard";
            } else if ("TEACHER".equals(userType)) {
                return "redirect:/teacher/dashboard";
            }
        }

        // 清空可能的错误消息
        if (!model.containsAttribute("error")) {
            model.addAttribute("error", "");
        }

        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String userType,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            log.info("用户尝试登录 - 用户名: {}, 类型: {}", username, userType);

            Object user = authService.login(username, password, userType);

            if (user != null) {
                // 更新最后登录时间
                updateLastLoginTime(user);

                session.setAttribute("user", user);
                session.setAttribute("userType", userType);
                session.setAttribute("username", username);

                log.info("用户登录成功 - 用户名: {}, 类型: {}", username, userType);

                if ("ADMIN".equals(userType)) {
                    return "redirect:/admin/dashboard";
                } else if ("TEACHER".equals(userType)) {
                    return "redirect:/teacher/dashboard";
                }
            }

            log.warn("用户名或密码错误 - 用户名: {}, 类型: {}", username, userType);
            redirectAttributes.addFlashAttribute("error", "用户名或密码错误！");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("登录过程发生错误", e);
            redirectAttributes.addFlashAttribute("error", "系统错误，请稍后重试！");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        String userType = (String) session.getAttribute("userType");

        session.invalidate();

        log.info("用户退出登录 - 用户名: {}, 类型: {}", username, userType);
        redirectAttributes.addFlashAttribute("message", "您已成功退出登录！");
        return "redirect:/login";
    }

    private void updateLastLoginTime(Object user) {
        if (user instanceof Admin) {
            Admin admin = (Admin) user;
            admin.setLastLoginTime(LocalDateTime.now());
        } else if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            teacher.setLastLoginTime(LocalDateTime.now());
        }
    }
}