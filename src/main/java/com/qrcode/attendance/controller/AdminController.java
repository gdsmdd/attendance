package com.qrcode.attendance.controller;

import com.qrcode.attendance.entity.*;
import com.qrcode.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final AdminRepository adminRepository;

    // 管理员仪表板
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        // 统计数据
        long teacherCount = teacherRepository.count();
        long studentCount = studentRepository.count();
        long departmentCount = departmentRepository.count();

        model.addAttribute("teacherCount", teacherCount);
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("departmentCount", departmentCount);
        model.addAttribute("admin", user);

        return "admin/dashboard";
    }

    // ========== 教师管理 ==========

    // 教师列表
    @GetMapping("/teachers")
    public String teacherList(Model model, HttpSession session) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            // 获取所有教师
            List<Teacher> teachers = teacherRepository.findAll();

            // 获取所有院系（用于显示）
            List<Department> departments = departmentRepository.findAll();

            long activeCount = teachers.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count();
            long inactiveCount = teachers.stream().filter(s -> "INACTIVE".equals(s.getStatus())).count();

            // 添加到模型
            model.addAttribute("activeCount", activeCount);
            model.addAttribute("inactiveCount", inactiveCount);
            model.addAttribute("teachers", teachers);
            model.addAttribute("departments", departments);
            model.addAttribute("admin", user);

            // 检查是否有成功/错误消息
            if (session.getAttribute("success") != null) {
                model.addAttribute("success", session.getAttribute("success"));
                session.removeAttribute("success");
            }
            if (session.getAttribute("error") != null) {
                model.addAttribute("error", session.getAttribute("error"));
                session.removeAttribute("error");
            }

            return "admin/teacher-list";

        } catch (Exception e) {
            // 记录错误
            log.error("获取教师列表失败", e);

            // 设置错误消息
            model.addAttribute("error", "获取教师列表失败：" + e.getMessage());

            // 返回空列表
            model.addAttribute("teachers", new ArrayList<Teacher>());
            model.addAttribute("departments", new ArrayList<Department>());

            return "admin/teacher-list";
        }
    }

    // 添加教师页面
    @GetMapping("/teachers/add")
    public String addTeacherPage(Model model, HttpSession session) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            // 获取所有院系
            List<Department> departments = departmentRepository.findAll();
            model.addAttribute("departments", departments);
            model.addAttribute("admin", user);

            return "admin/teacher-add";

        } catch (Exception e) {
            log.error("访问添加教师页面失败", e);
            return "redirect:/admin/teachers?error=系统错误";
        }
    }

    // 添加教师
    @PostMapping("/teachers/add")
    public String addTeacher(
            @RequestParam String teacherId,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String title,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            // 检查工号是否已存在
            if (teacherRepository.existsByTeacherId(teacherId)) {
                redirectAttributes.addFlashAttribute("error", "工号已存在！");
                return "redirect:/admin/teachers/add";
            }

            // 检查用户名是否已存在
            if (teacherRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "用户名已存在！");
                return "redirect:/admin/teachers/add";
            }

            // 获取院系
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("院系不存在"));

            // 创建教师
            Teacher teacher = new Teacher();
            teacher.setTeacherId(teacherId);
            teacher.setUsername(username);
            teacher.setPassword(password);
            teacher.setName(name);
            teacher.setDepartment(department);
            teacher.setTitle(title);
            teacher.setStatus("ACTIVE");

            teacherRepository.save(teacher);

            redirectAttributes.addFlashAttribute("success", "教师添加成功！");
            return "redirect:/admin/teachers";

        } catch (Exception e) {
            log.error("添加教师失败", e);
            redirectAttributes.addFlashAttribute("error", "添加失败：" + e.getMessage());
            return "redirect:/admin/teachers/add";
        }
    }

    // 编辑教师页面
    @GetMapping("/teachers/edit/{id}")
    public String editTeacherPage(@PathVariable Long id, Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("教师不存在"));
        List<Department> departments = departmentRepository.findAll();

        model.addAttribute("teacher", teacher);
        model.addAttribute("departments", departments);
        model.addAttribute("admin", user);

        return "admin/teacher-edit";
    }

    // 更新教师
    @PostMapping("/teachers/update")
    public String updateTeacher(
            @RequestParam Long id,
            @RequestParam String teacherId,
            @RequestParam String username,
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String title,
            @RequestParam String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Teacher teacher = teacherRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("教师不存在"));

            // 检查工号是否被其他教师使用
            if (!teacher.getTeacherId().equals(teacherId) &&
                    teacherRepository.existsByTeacherId(teacherId)) {
                redirectAttributes.addFlashAttribute("error", "工号已被其他教师使用！");
                return "redirect:/admin/teachers/edit/" + id;
            }

            // 检查用户名是否被其他教师使用
            if (!teacher.getUsername().equals(username) &&
                    teacherRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "用户名已被其他教师使用！");
                return "redirect:/admin/teachers/edit/" + id;
            }

            // 获取院系
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("院系不存在"));

            // 更新教师信息
            teacher.setTeacherId(teacherId);
            teacher.setUsername(username);
            teacher.setName(name);
            teacher.setDepartment(department);
            teacher.setTitle(title);
            teacher.setStatus(status);

            teacherRepository.save(teacher);

            redirectAttributes.addFlashAttribute("success", "教师信息更新成功！");
            return "redirect:/admin/teachers";

        } catch (Exception e) {
            log.error("更新教师失败", e);
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
            return "redirect:/admin/teachers/edit/" + id;
        }
    }

    // 删除教师
    @GetMapping("/teachers/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            teacherRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "教师删除成功！");

        } catch (Exception e) {
            log.error("删除教师失败", e);
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }

        return "redirect:/admin/teachers";
    }

    // ========== 学生管理 ==========

    // 学生列表
    @GetMapping("/students")
    public String studentList(Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        List<Student> students = studentRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        long activeCount = students.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count();
        long inactiveCount = students.stream().filter(s -> "INACTIVE".equals(s.getStatus())).count();
        long graduatedCount = students.stream().filter(s -> "GRADUATED".equals(s.getStatus())).count();

        model.addAttribute("students", students);
        model.addAttribute("departments", departments);
        model.addAttribute("admin", user);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("graduatedCount", graduatedCount);

        return "admin/student-list";
    }

    // 添加学生页面
    @GetMapping("/students/add")
    public String addStudentPage(Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);
        model.addAttribute("admin", user);

        return "admin/student-add";
    }

    // 添加学生
    @PostMapping("/students/add")
    public String addStudent(
            @RequestParam String studentId,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam String className,
            @RequestParam String enrollmentYear,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            // 检查学号是否已存在
            if (studentRepository.existsByStudentId(studentId)) {
                redirectAttributes.addFlashAttribute("error", "学号已存在！");
                return "redirect:/admin/students/add";
            }

            // 检查用户名是否已存在
            if (studentRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "用户名已存在！");
                return "redirect:/admin/students/add";
            }

            // 获取院系
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("院系不存在"));

            // 创建学生
            Student student = new Student();
            student.setStudentId(studentId);
            student.setUsername(username);
            student.setPassword(password);
            student.setName(name);
            student.setDepartment(department);
            student.setClassName(className);
            student.setEnrollmentYear(enrollmentYear);
            student.setStatus("ACTIVE");

            studentRepository.save(student);

            redirectAttributes.addFlashAttribute("success", "学生添加成功！");
            return "redirect:/admin/students";

        } catch (Exception e) {
            log.error("添加学生失败", e);
            redirectAttributes.addFlashAttribute("error", "添加失败：" + e.getMessage());
            return "redirect:/admin/students/add";
        }
    }

    // 编辑学生页面
    @GetMapping("/students/edit/{id}")
    public String editStudentPage(@PathVariable Long id, Model model, HttpSession session) {
        // 检查是否登录
        Object user = session.getAttribute("user");
        if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("学生不存在"));
        List<Department> departments = departmentRepository.findAll();

        model.addAttribute("student", student);
        model.addAttribute("departments", departments);
        model.addAttribute("admin", user);

        return "admin/student-edit";
    }

    // 更新学生
    @PostMapping("/students/update")
    public String updateStudent(
            @RequestParam Long id,
            @RequestParam String studentId,
            @RequestParam String username,
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam String className,
            @RequestParam String enrollmentYear,
            @RequestParam String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Student student = studentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("学生不存在"));

            // 检查学号是否被其他学生使用
            if (!student.getStudentId().equals(studentId) &&
                    studentRepository.existsByStudentId(studentId)) {
                redirectAttributes.addFlashAttribute("error", "学号已被其他学生使用！");
                return "redirect:/admin/students/edit/" + id;
            }

            // 检查用户名是否被其他学生使用
            if (!student.getUsername().equals(username) &&
                    studentRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "用户名已被其他学生使用！");
                return "redirect:/admin/students/edit/" + id;
            }

            // 获取院系
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("院系不存在"));

            // 更新学生信息
            student.setStudentId(studentId);
            student.setUsername(username);
            student.setName(name);
            student.setDepartment(department);
            student.setClassName(className);
            student.setEnrollmentYear(enrollmentYear);
            student.setStatus(status);

            studentRepository.save(student);

            redirectAttributes.addFlashAttribute("success", "学生信息更新成功！");
            return "redirect:/admin/students";

        } catch (Exception e) {
            log.error("更新学生失败", e);
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
            return "redirect:/admin/students/edit/" + id;
        }
    }

    // 删除学生
    @GetMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            studentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "学生删除成功！");

        } catch (Exception e) {
            log.error("删除学生失败", e);
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }

        return "redirect:/admin/students";
    }

    // ========== 院系管理 ==========

    // ========== 院系管理（简化版） ==========

    // 院系列表
    @GetMapping("/departments")
    public String departmentList(Model model, HttpSession session) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            // 获取所有院系
            List<Department> departments = departmentRepository.findAll();

            model.addAttribute("departments", departments);
            model.addAttribute("admin", user);

            return "admin/department-list";

        } catch (Exception e) {
            log.error("获取院系列表失败", e);
            model.addAttribute("error", "获取院系列表失败：" + e.getMessage());
            model.addAttribute("departments", new ArrayList<Department>());
            return "admin/department-list";
        }
    }

    // 添加院系
    @PostMapping("/departments/add")
    public String addDepartment(
            @RequestParam String name,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            // 检查院系名称是否已存在
            if (departmentRepository.existsByName(name)) {
                redirectAttributes.addFlashAttribute("error", "院系名称已存在！");
                return "redirect:/admin/departments";
            }

            // 创建院系
            Department department = new Department();
            department.setName(name);

            departmentRepository.save(department);

            redirectAttributes.addFlashAttribute("success", "院系添加成功！");
            return "redirect:/admin/departments";

        } catch (Exception e) {
            log.error("添加院系失败", e);
            redirectAttributes.addFlashAttribute("error", "添加失败：" + e.getMessage());
            return "redirect:/admin/departments";
        }
    }

    // 更新院系
    @PostMapping("/departments/update")
    public String updateDepartment(
            @RequestParam Long id,
            @RequestParam String name,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Department department = departmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("院系不存在"));

            // 检查新名称是否被其他院系使用
            if (!department.getName().equals(name) &&
                    departmentRepository.existsByName(name)) {
                redirectAttributes.addFlashAttribute("error", "院系名称已被其他院系使用！");
                return "redirect:/admin/departments";
            }

            // 更新院系名称
            department.setName(name);

            departmentRepository.save(department);

            redirectAttributes.addFlashAttribute("success", "院系信息更新成功！");
            return "redirect:/admin/departments";

        } catch (Exception e) {
            log.error("更新院系失败", e);
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
            return "redirect:/admin/departments";
        }
    }

    // 删除院系
    @GetMapping("/departments/delete/{id}")
    public String deleteDepartment(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            // 检查是否登录
            Object user = session.getAttribute("user");
            if (user == null || !"ADMIN".equals(session.getAttribute("userType"))) {
                return "redirect:/login";
            }

            Department department = departmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("院系不存在"));

            // 检查院系下是否有用户关联
            // 查询该院系下是否有教师
            long teacherCount = teacherRepository.countByDepartmentId(id);
            // 查询该院系下是否有学生
            long studentCount = studentRepository.countByDepartmentId(id);

            if (teacherCount > 0 || studentCount > 0) {
                redirectAttributes.addFlashAttribute("error",
                        "删除失败：该院系下还有" + (teacherCount + studentCount) + "个用户关联，请先移除关联用户！");
                return "redirect:/admin/departments";
            }

            // 删除院系
            departmentRepository.delete(department);

            redirectAttributes.addFlashAttribute("success", "院系删除成功！");

        } catch (Exception e) {
            log.error("删除院系失败", e);
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }

        return "redirect:/admin/departments";
    }
}