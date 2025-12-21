package com.qrcode.attendance.entity;
/**
 * 考勤统计实体类
 */
public class AttendanceStatistics {
    // 总学生数（教学班下有效学生）
    private Long totalStudentCount;
    // 正常签到人数（状态=PRESENT）
    private Long presentCount;
    // 迟到人数（状态=LATE）
    private Long lateCount;
    // 缺勤人数（总人数 - 正常 - 迟到）
    private Long absentCount;

    // 全参构造器
    public AttendanceStatistics(Long totalStudentCount, Long presentCount, Long lateCount) {
        this.totalStudentCount = totalStudentCount;
        this.presentCount = presentCount;
        this.lateCount = lateCount;
        // 自动计算缺勤人数
        this.absentCount = (totalStudentCount != null ? totalStudentCount : 0)
                - (presentCount != null ? presentCount : 0)
                - (lateCount != null ? lateCount : 0);
        // 防止缺勤数为负数
        if (this.absentCount < 0) {
            this.absentCount = 0L;
        }
    }

    // Getter 方法（前端/Controller 需要通过 getter 获取值）
    public Long getTotalStudentCount() {
        return totalStudentCount;
    }

    public Long getPresentCount() {
        return presentCount;
    }

    public Long getLateCount() {
        return lateCount;
    }

    public Long getAbsentCount() {
        return absentCount;
    }
}