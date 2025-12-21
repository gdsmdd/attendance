package com.qrcode.attendance.service;

import com.qrcode.attendance.entity.*;
import com.qrcode.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeachingClassService {

    private final TeachingClassRepository teachingClassRepository;
    private final TeachingClassStudentRepository teachingClassStudentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public TeachingClass createTeachingClass(TeachingClass teachingClass) {
        // 检查教学班名称是否已存在
        if (teachingClassRepository.existsByClassNameAndTeacher(
                teachingClass.getClassName(), teachingClass.getTeacher())) {
            throw new RuntimeException("教学班名称已存在");
        }

        return teachingClassRepository.save(teachingClass);
    }

    @Transactional
    public TeachingClass updateTeachingClass(Long id, TeachingClass teachingClass) {
        TeachingClass existing = teachingClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("教学班不存在"));

        // 检查名称是否被其他教学班使用
        if (!existing.getClassName().equals(teachingClass.getClassName()) &&
                teachingClassRepository.existsByClassNameAndTeacher(
                        teachingClass.getClassName(), teachingClass.getTeacher())) {
            throw new RuntimeException("教学班名称已被使用");
        }

        existing.setClassName(teachingClass.getClassName());
        existing.setCourseName(teachingClass.getCourseName());
        existing.setSemester(teachingClass.getSemester());
        existing.setIsActive(teachingClass.getIsActive());

        return teachingClassRepository.save(existing);
    }

    @Transactional
    public void deleteTeachingClass(Long id) {
        TeachingClass teachingClass = teachingClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("教学班不存在"));

        // 检查是否有学生关联
        long studentCount = teachingClassStudentRepository.countActiveStudentsByTeachingClass(teachingClass);
        if (studentCount > 0) {
            throw new RuntimeException("教学班中还有学生，无法删除");
        }

        teachingClassRepository.delete(teachingClass);
    }

    public List<TeachingClass> getTeachingClassesByTeacher(Teacher teacher) {
        return teachingClassRepository.findByTeacherAndIsActiveTrue(teacher);
    }

    public List<Student> getStudentsInTeachingClass(TeachingClass teachingClass) {
        return teachingClassStudentRepository.findActiveStudentsByTeachingClass(teachingClass);
    }

    @Transactional
    public void addStudentToTeachingClass(Long teachingClassId, Long studentId) {
        TeachingClass teachingClass = teachingClassRepository.findById(teachingClassId)
                .orElseThrow(() -> new RuntimeException("教学班不存在"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        // 检查学生是否已在该教学班中
        if (teachingClassStudentRepository.existsByTeachingClassAndStudent(teachingClass, student)) {
            TeachingClassStudent tcs = teachingClassStudentRepository
                    .findByTeachingClassAndStudent(teachingClass, student)
                    .orElseThrow(() -> new RuntimeException("学生记录异常"));

            if ("ACTIVE".equals(tcs.getStatus())) {
                throw new RuntimeException("学生已在该教学班中");
            } else {
                tcs.setStatus("ACTIVE");
                teachingClassStudentRepository.save(tcs);
                return;
            }
        }

        // 添加学生到教学班
        TeachingClassStudent tcs = new TeachingClassStudent();
        tcs.setTeachingClass(teachingClass);
        tcs.setStudent(student);
        tcs.setStatus("ACTIVE");

        teachingClassStudentRepository.save(tcs);
    }

    @Transactional
    public void removeStudentFromTeachingClass(Long teachingClassId, Long studentId) {
        TeachingClass teachingClass = teachingClassRepository.findById(teachingClassId)
                .orElseThrow(() -> new RuntimeException("教学班不存在"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        TeachingClassStudent tcs = teachingClassStudentRepository
                .findByTeachingClassAndStudent(teachingClass, student)
                .orElseThrow(() -> new RuntimeException("学生不在该教学班中"));

        tcs.setStatus("DROPPED");
        teachingClassStudentRepository.save(tcs);
    }

    public List<Student> searchStudentsNotInTeachingClass(TeachingClass teachingClass, String keyword) {
        // 获取教学班中已有的学生
        List<Student> existingStudents = getStudentsInTeachingClass(teachingClass);
        List<Long> existingStudentIds = existingStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        // 根据关键词搜索学生
        List<Student> allStudents;
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 这里需要根据实际情况实现搜索逻辑
            allStudents = studentRepository.findAll().stream()
                    .filter(s -> s.getName().contains(keyword) ||
                            s.getStudentId().contains(keyword) ||
                            s.getClassName().contains(keyword))
                    .collect(Collectors.toList());
        } else {
            allStudents = studentRepository.findAll();
        }

        // 过滤掉已在教学班中的学生
        return allStudents.stream()
                .filter(s -> !existingStudentIds.contains(s.getId()))
                .collect(Collectors.toList());
    }
}