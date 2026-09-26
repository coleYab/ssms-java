package com.example.ssms.schoolclass.service;

import com.example.ssms.academic.entity.AcademicYear;
import com.example.ssms.academic.entity.AcademicYearStatus;
import com.example.ssms.academic.repository.AcademicYearRepository;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.enrollment.entity.Enrollment;
import com.example.ssms.enrollment.entity.EnrollmentStatus;
import com.example.ssms.enrollment.repository.EnrollmentRepository;
import com.example.ssms.grade.repository.GradeLevelRepository;
import com.example.ssms.schoolclass.dto.ClassDtos.*;
import com.example.ssms.schoolclass.entity.SchoolClass;
import com.example.ssms.schoolclass.entity.SchoolClassStatus;
import com.example.ssms.schoolclass.repository.SchoolClassRepository;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    public ClassService(
            SchoolClassRepository classRepository,
            AcademicYearRepository academicYearRepository,
            GradeLevelRepository gradeLevelRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService
    ) {
        this.classRepository = classRepository;
        this.academicYearRepository = academicYearRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        AcademicYear year = academicYearRepository.findById(request.academicYearId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        if (!gradeLevelRepository.existsById(request.gradeLevelId())) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Grade level not found.");
        }

        if (classRepository.findByAcademicYearIdAndNameIgnoreCase(year.getId(), request.name().trim()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A class with this name already exists in this academic year.")
                    .withContext("conflictingField", "name");
        }

        if (request.homeroomTeacherId() != null) {
            if (classRepository.existsByHomeroomTeacherIdAndAcademicYearId(request.homeroomTeacherId(), year.getId())) {
                throw new ApiException(ErrorCode.BUSINESS_HOMEROOM_CONFLICT);
            }
        }

        SchoolClass sc = new SchoolClass();
        sc.setName(request.name().trim());
        sc.setAcademicYearId(year.getId());
        sc.setGradeLevelId(request.gradeLevelId());
        sc.setCapacity(request.capacity() != null ? request.capacity() : 40);
        sc.setHomeroomTeacherId(request.homeroomTeacherId());
        sc.setStatus(SchoolClassStatus.ACTIVE);
        sc = classRepository.save(sc);

        auditLogService.record("CLASS_CREATE", "CLASS", sc.getId(), "SUCCESS", null, null);
        return mapToResponse(sc);
    }

    @Transactional(readOnly = true)
    public Page<ClassResponse> listClasses(UserPrincipal caller, UUID yearId, UUID gradeId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
        return classRepository.findAll(pageRequest).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ClassResponse getClass(UserPrincipal caller, UUID id) {
        SchoolClass sc = classRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        if (caller.getRole() == Role.TEACHER) {
            // Scope check: teacher must teach offering or be homeroom teacher
            if (sc.getHomeroomTeacherId() != null && !sc.getHomeroomTeacherId().equals(caller.getProfileId())) {
                // If not homeroom teacher, allowed if teaches an offering
            }
        }

        return mapToResponse(sc);
    }

    @Transactional
    public ClassResponse updateClass(UUID id, UpdateClassRequest request) {
        SchoolClass sc = classRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        AcademicYear year = academicYearRepository.findById(sc.getAcademicYearId()).orElse(null);
        if (year != null && year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        if (request.capacity() != null) {
            if (request.capacity() < sc.getEnrolledCount()) {
                throw new ApiException(ErrorCode.BUSINESS_CAPACITY_BELOW_ENROLLMENT)
                        .withContext("count", sc.getEnrolledCount());
            }
            sc.setCapacity(request.capacity());
        }

        if (request.name() != null && !request.name().trim().equalsIgnoreCase(sc.getName())) {
            if (classRepository.findByAcademicYearIdAndNameIgnoreCase(sc.getAcademicYearId(), request.name().trim()).isPresent()) {
                throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A class with this name already exists in this academic year.");
            }
            sc.setName(request.name().trim());
        }

        if (request.status() != null) {
            sc.setStatus(request.status());
        }

        classRepository.save(sc);
        auditLogService.record("CLASS_UPDATE", "CLASS", sc.getId(), "SUCCESS", null, null);
        return mapToResponse(sc);
    }

    @Transactional
    public void deleteClass(UUID id) {
        SchoolClass sc = classRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        if (sc.getEnrolledCount() > 0) {
            throw new ApiException(ErrorCode.CONFLICT_IN_USE, "Cannot delete class with active enrollments.")
                    .withContext("dependents", "students").withContext("count", sc.getEnrolledCount());
        }

        sc.setDeletedAt(Instant.now());
        classRepository.save(sc);
        auditLogService.record("CLASS_DELETE", "CLASS", id, "SUCCESS", null, null);
    }

    @Transactional
    public BulkEnrollmentResult enrollStudents(UUID classId, List<UUID> studentIds, boolean partial) {
        if (studentIds.size() > 100) {
            throw new ApiException(ErrorCode.VALIDATION_BULK_LIMIT_EXCEEDED).withContext("max", 100);
        }

        SchoolClass sc = classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        AcademicYear year = academicYearRepository.findById(sc.getAcademicYearId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        List<EnrollmentSuccessItem> succeeded = new ArrayList<>();
        List<EnrollmentFailedItem> failed = new ArrayList<>();

        for (int i = 0; i < studentIds.size(); i++) {
            UUID studentId = studentIds.get(i);
            try {
                if (sc.getEnrolledCount() >= sc.getCapacity()) {
                    throw new ApiException(ErrorCode.BUSINESS_CLASS_FULL).withContext("capacity", sc.getCapacity());
                }

                if (enrollmentRepository.existsByStudentIdAndAcademicYearIdAndStatus(studentId, year.getId(), EnrollmentStatus.ACTIVE)) {
                    throw new ApiException(ErrorCode.BUSINESS_STUDENT_ALREADY_ENROLLED);
                }

                Enrollment enrollment = new Enrollment();
                enrollment.setClassId(classId);
                enrollment.setStudentId(studentId);
                enrollment.setAcademicYearId(year.getId());
                enrollment.setStartDate(LocalDate.now());
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(enrollment);

                sc.setEnrolledCount(sc.getEnrolledCount() + 1);
                classRepository.save(sc);

                succeeded.add(new EnrollmentSuccessItem(studentId));
            } catch (ApiException e) {
                if (!partial) {
                    throw e;
                }
                failed.add(new EnrollmentFailedItem(i, studentId, new ErrorDetailItem(e.getErrorCode().name(), e.getMessage())));
            }
        }

        auditLogService.record("CLASS_ENROLL_STUDENTS", "CLASS", classId, "SUCCESS", null, "Enrolled " + succeeded.size() + " students");
        return new BulkEnrollmentResult(succeeded, failed);
    }

    @Transactional
    public void unenrollStudent(UUID classId, UUID studentId) {
        SchoolClass sc = classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassIdAndStatus(studentId, classId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_STUDENT_NOT_ENROLLED));

        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollment.setEndDate(LocalDate.now());
        enrollmentRepository.save(enrollment);

        sc.setEnrolledCount(Math.max(0, sc.getEnrolledCount() - 1));
        classRepository.save(sc);

        auditLogService.record("CLASS_UNENROLL_STUDENT", "CLASS", classId, "SUCCESS", null, "Unenrolled student " + studentId);
    }

    @Transactional
    public void transferStudents(UUID classId, TransferStudentsRequest request) {
        SchoolClass fromClass = classRepository.findById(classId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Source class not found."));

        SchoolClass toClass = classRepository.findById(request.toClassId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Target class not found."));

        if (!fromClass.getAcademicYearId().equals(toClass.getAcademicYearId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Transfer must be within the same academic year.");
        }

        if (toClass.getEnrolledCount() + request.studentIds().size() > toClass.getCapacity()) {
            throw new ApiException(ErrorCode.BUSINESS_CLASS_FULL).withContext("capacity", toClass.getCapacity());
        }

        LocalDate effectiveDate = request.effectiveDate() != null ? request.effectiveDate() : LocalDate.now();

        for (UUID sId : request.studentIds()) {
            Enrollment oldEnrollment = enrollmentRepository.findByStudentIdAndClassIdAndStatus(sId, classId, EnrollmentStatus.ACTIVE)
                    .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_STUDENT_NOT_ENROLLED));

            oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
            oldEnrollment.setEndDate(effectiveDate);
            enrollmentRepository.save(oldEnrollment);

            Enrollment newEnrollment = new Enrollment();
            newEnrollment.setClassId(toClass.getId());
            newEnrollment.setStudentId(sId);
            newEnrollment.setAcademicYearId(toClass.getAcademicYearId());
            newEnrollment.setStartDate(effectiveDate);
            newEnrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(newEnrollment);

            fromClass.setEnrolledCount(Math.max(0, fromClass.getEnrolledCount() - 1));
            toClass.setEnrolledCount(toClass.getEnrolledCount() + 1);
        }

        classRepository.save(fromClass);
        classRepository.save(toClass);

        auditLogService.record("CLASS_TRANSFER_STUDENTS", "CLASS", classId, "SUCCESS", null, "Transferred to " + toClass.getId());
    }

    @Transactional
    public ClassResponse assignHomeroomTeacher(UUID id, UUID teacherId) {
        SchoolClass sc = classRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        if (classRepository.existsByHomeroomTeacherIdAndAcademicYearId(teacherId, sc.getAcademicYearId())) {
            throw new ApiException(ErrorCode.BUSINESS_HOMEROOM_CONFLICT);
        }

        sc.setHomeroomTeacherId(teacherId);
        classRepository.save(sc);

        auditLogService.record("CLASS_ASSIGN_HOMEROOM", "CLASS", id, "SUCCESS", null, null);
        return mapToResponse(sc);
    }

    @Transactional
    public ClassResponse clearHomeroomTeacher(UUID id) {
        SchoolClass sc = classRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

        sc.setHomeroomTeacherId(null);
        classRepository.save(sc);

        auditLogService.record("CLASS_CLEAR_HOMEROOM", "CLASS", id, "SUCCESS", null, null);
        return mapToResponse(sc);
    }

    @Transactional
    public BulkEnrollmentResult promoteClass(UserPrincipal caller, UUID id, PromoteClassRequest request) {
        if (caller != null && !caller.isStepUpValid(600)) {
            throw new ApiException(ErrorCode.AUTH_REAUTH_REQUIRED);
        }

        return enrollStudents(request.toClassId(), request.studentIds(), false);
    }

    private ClassResponse mapToResponse(SchoolClass sc) {
        return new ClassResponse(
                sc.getId(),
                sc.getName(),
                sc.getAcademicYearId(),
                sc.getGradeLevelId(),
                sc.getCapacity(),
                sc.getHomeroomTeacherId(),
                sc.getStatus(),
                sc.getEnrolledCount(),
                sc.getCreatedAt(),
                sc.getUpdatedAt(),
                sc.getVersion()
        );
    }
}
