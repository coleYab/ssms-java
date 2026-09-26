package com.example.ssms.system.service;

import com.example.ssms.academic.entity.AcademicYearStatus;
import com.example.ssms.academic.repository.AcademicYearRepository;
import com.example.ssms.academic.repository.TermRepository;
import com.example.ssms.attendance.entity.AttendanceStatus;
import com.example.ssms.attendance.repository.AttendanceRecordRepository;
import com.example.ssms.attendance.repository.AttendanceSessionRepository;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.course.repository.CourseRepository;
import com.example.ssms.enrollment.repository.EnrollmentRepository;
import com.example.ssms.offering.repository.CourseOfferingRepository;
import com.example.ssms.schoolclass.repository.SchoolClassRepository;
import com.example.ssms.student.repository.StudentProfileRepository;
import com.example.ssms.system.entity.SystemSetting;
import com.example.ssms.system.repository.SystemSettingRepository;
import com.example.ssms.teacher.repository.TeacherProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class SystemService {

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;

    public SystemService(SystemSettingRepository systemSettingRepository,
                         UserRepository userRepository,
                         StudentProfileRepository studentProfileRepository,
                         TeacherProfileRepository teacherProfileRepository,
                         SchoolClassRepository schoolClassRepository,
                         CourseRepository courseRepository,
                         CourseOfferingRepository courseOfferingRepository,
                         EnrollmentRepository enrollmentRepository,
                         AttendanceSessionRepository attendanceSessionRepository,
                         AttendanceRecordRepository attendanceRecordRepository,
                         AcademicYearRepository academicYearRepository,
                         TermRepository termRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.academicYearRepository = academicYearRepository;
        this.termRepository = termRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllSettings() {
        List<SystemSetting> settings = systemSettingRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        for (SystemSetting setting : settings) {
            result.put(setting.getKey(), setting.getValue());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public SystemSetting getSetting(String key) {
        return systemSettingRepository.findByKey(key)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "System setting '" + key + "' not found"));
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> updates) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            SystemSetting setting = systemSettingRepository.findByKey(key)
                    .orElseGet(() -> {
                        SystemSetting s = new SystemSetting();
                        s.setKey(key);
                        s.setDescription("System configuration setting");
                        return s;
                    });
            setting.setValue(value);
            systemSettingRepository.save(setting);
        }
        return getAllSettings();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalStudents", studentProfileRepository.count());
        stats.put("totalTeachers", teacherProfileRepository.count());
        stats.put("totalClasses", schoolClassRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("totalOfferings", courseOfferingRepository.count());
        stats.put("totalEnrollments", enrollmentRepository.count());
        stats.put("totalAttendanceSessions", attendanceSessionRepository.count());
        stats.put("totalAttendanceRecords", attendanceRecordRepository.count());
        stats.put("currentAcademicYear", academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).map(a -> a.getName()).orElse(null));

        LocalDate today = LocalDate.now();
        stats.put("currentTerm", termRepository.findAll().stream()
                .filter(t -> !today.isBefore(t.getStartDate()) && !today.isAfter(t.getEndDate()))
                .map(t -> t.getName())
                .findFirst()
                .orElse(null));
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherDashboardStats(UUID teacherId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        long assignedOfferings = courseOfferingRepository.findAll().stream()
                .filter(o -> Objects.equals(o.getTeacherId(), teacherId))
                .count();

        Set<UUID> offeringIds = new HashSet<>();
        courseOfferingRepository.findAll().stream()
                .filter(o -> Objects.equals(o.getTeacherId(), teacherId))
                .forEach(o -> offeringIds.add(o.getId()));

        long heldSessions = attendanceSessionRepository.findAll().stream()
                .filter(s -> offeringIds.contains(s.getOfferingId()) || Objects.equals(s.getTakenBy(), teacherId))
                .count();

        stats.put("assignedOfferingsCount", assignedOfferings);
        stats.put("conductedAttendanceSessionsCount", heldSessions);
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentDashboardStats(UUID studentId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        long enrollmentsCount = enrollmentRepository.findAll().stream()
                .filter(e -> Objects.equals(e.getStudentId(), studentId))
                .count();
        long attendanceRecordsCount = attendanceRecordRepository.findAll().stream()
                .filter(r -> Objects.equals(r.getStudentId(), studentId))
                .count();
        long presentCount = attendanceRecordRepository.findAll().stream()
                .filter(r -> Objects.equals(r.getStudentId(), studentId)
                        && (r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE))
                .count();

        double attendanceRate = attendanceRecordsCount > 0 ? ((double) presentCount / attendanceRecordsCount) * 100.0 : 100.0;

        stats.put("activeEnrollmentsCount", enrollmentsCount);
        stats.put("totalAttendanceRecords", attendanceRecordsCount);
        stats.put("attendancePercentage", Math.round(attendanceRate * 10.0) / 10.0);
        return stats;
    }
}
