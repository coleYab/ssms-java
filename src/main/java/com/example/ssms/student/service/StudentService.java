package com.example.ssms.student.service;

import com.example.ssms.academic.entity.AcademicYear;
import com.example.ssms.academic.entity.AcademicYearStatus;
import com.example.ssms.academic.repository.AcademicYearRepository;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.auth.service.PasswordService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.enrollment.entity.Enrollment;
import com.example.ssms.enrollment.entity.EnrollmentStatus;
import com.example.ssms.enrollment.repository.EnrollmentRepository;
import com.example.ssms.guardian.entity.Guardian;
import com.example.ssms.guardian.repository.GuardianRepository;
import com.example.ssms.schoolclass.entity.SchoolClass;
import com.example.ssms.schoolclass.repository.SchoolClassRepository;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.ProfileType;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.student.dto.StudentDtos.*;
import com.example.ssms.student.entity.StudentProfile;
import com.example.ssms.student.entity.StudentStatus;
import com.example.ssms.student.repository.StudentProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StudentService {

    private final StudentProfileRepository studentRepository;
    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordService passwordService;
    private final AuditLogService auditLogService;

    public StudentService(
            StudentProfileRepository studentRepository,
            UserRepository userRepository,
            GuardianRepository guardianRepository,
            SchoolClassRepository classRepository,
            AcademicYearRepository academicYearRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordService passwordService,
            AuditLogService auditLogService
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.guardianRepository = guardianRepository;
        this.classRepository = classRepository;
        this.academicYearRepository = academicYearRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordService = passwordService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A student with this email already exists.")
                    .withContext("conflictingField", "email");
        }

        if (request.guardians() != null && !request.guardians().isEmpty()) {
            long primaryCount = request.guardians().stream().filter(CreateGuardianSubRequest::isPrimary).count();
            if (primaryCount != 1) {
                throw new ApiException(ErrorCode.BUSINESS_GUARDIAN_PRIMARY_REQUIRED);
            }
        }

        // Generate studentNumber
        String studentNumber = generateStudentNumber();

        // Create linked User account
        String tempPass = "StudentPass1234!";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordService.hash(tempPass));
        user.setRole(Role.STUDENT);
        user.setStatus(AccountStatus.PENDING);
        user.setMustChangePassword(true);
        user.setPhone(request.phone());
        user.setProfileType(ProfileType.STUDENT);
        user = userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setStudentNumber(studentNumber);
        profile.setFirstName(request.firstName());
        profile.setMiddleName(request.middleName());
        profile.setLastName(request.lastName());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setAdmissionDate(request.admissionDate());
        profile.setStatus(StudentStatus.ACTIVE);
        profile.setNationality(request.nationality() != null ? request.nationality() : "ET");
        profile.setAddressLine1(request.addressLine1());
        profile.setCity(request.city());
        profile.setCountry(request.country() != null ? request.country() : "ET");
        profile.setBloodType(request.bloodType());
        profile.setEmergencyNotes(request.emergencyNotes());
        profile = studentRepository.save(profile);

        user.setProfileId(profile.getId());
        userRepository.save(user);

        // Save guardians
        if (request.guardians() != null) {
            for (CreateGuardianSubRequest gReq : request.guardians()) {
                Guardian guardian = new Guardian();
                guardian.setStudentId(profile.getId());
                guardian.setFirstName(gReq.firstName());
                guardian.setLastName(gReq.lastName());
                guardian.setRelationship(gReq.relationship());
                guardian.setPhone(gReq.phone());
                guardian.setEmail(gReq.email());
                guardian.setAddress(gReq.address());
                guardian.setOccupation(gReq.occupation());
                guardian.setPrimary(gReq.isPrimary());
                guardian.setCanPickUp(gReq.canPickUp() == null || gReq.canPickUp());
                guardianRepository.save(guardian);
            }
        }

        // Initial enrollment if classId given
        if (request.classId() != null) {
            SchoolClass sc = classRepository.findById(request.classId())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

            if (sc.getEnrolledCount() >= sc.getCapacity()) {
                throw new ApiException(ErrorCode.BUSINESS_CLASS_FULL).withContext("capacity", sc.getCapacity());
            }

            AcademicYear year = academicYearRepository.findById(sc.getAcademicYearId())
                    .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_NO_ACTIVE_ACADEMIC_YEAR));

            if (year.getStatus() == AcademicYearStatus.CLOSED) {
                throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
            }

            Enrollment enrollment = new Enrollment();
            enrollment.setStudentId(profile.getId());
            enrollment.setClassId(sc.getId());
            enrollment.setAcademicYearId(year.getId());
            enrollment.setStartDate(request.admissionDate());
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(enrollment);

            profile.setCurrentClassId(sc.getId());
            studentRepository.save(profile);

            sc.setEnrolledCount(sc.getEnrolledCount() + 1);
            classRepository.save(sc);
        }

        auditLogService.record("STUDENT_CREATE", "STUDENT", profile.getId(), "SUCCESS", null, null);
        List<GuardianResponse> guardians = getGuardiansForStudent(profile.getId());
        return mapToResponse(profile, user, guardians, false);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> listStudents(UserPrincipal caller, UUID classId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
        boolean maskSensitive = caller.getRole() == Role.TEACHER;
        return studentRepository.findAll(pageRequest)
                .map(p -> {
                    User user = userRepository.findById(p.getUserId()).orElse(null);
                    return mapToResponse(p, user, null, maskSensitive);
                });
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UserPrincipal caller, UUID id) {
        StudentProfile profile = studentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found."));

        if (caller.getRole() == Role.STUDENT && !profile.getId().equals(caller.getProfileId())) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found.");
        }

        boolean maskSensitive = caller.getRole() == Role.TEACHER;
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        List<GuardianResponse> guardians = getGuardiansForStudent(profile.getId());
        return mapToResponse(profile, user, guardians, maskSensitive);
    }

    @Transactional(readOnly = true)
    public StudentResponse getMe(UserPrincipal caller) {
        if (caller.getProfileId() == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No student profile linked.");
        }
        return getStudent(caller, caller.getProfileId());
    }

    @Transactional
    public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
        StudentProfile profile = studentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found."));

        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.middleName() != null) profile.setMiddleName(request.middleName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.dateOfBirth() != null) profile.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) profile.setGender(request.gender());
        if (request.nationality() != null) profile.setNationality(request.nationality());
        if (request.addressLine1() != null) profile.setAddressLine1(request.addressLine1());
        if (request.city() != null) profile.setCity(request.city());
        if (request.country() != null) profile.setCountry(request.country());
        if (request.bloodType() != null) profile.setBloodType(request.bloodType());
        if (request.emergencyNotes() != null) profile.setEmergencyNotes(request.emergencyNotes());

        studentRepository.save(profile);
        auditLogService.record("STUDENT_UPDATE", "STUDENT", profile.getId(), "SUCCESS", null, null);

        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return mapToResponse(profile, user, getGuardiansForStudent(profile.getId()), false);
    }

    @Transactional
    public void deleteStudent(UUID id) {
        StudentProfile profile = studentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found."));

        profile.setDeletedAt(java.time.Instant.now());
        studentRepository.save(profile);

        userRepository.findById(profile.getUserId()).ifPresent(u -> {
            u.setDeletedAt(java.time.Instant.now());
            u.setStatus(AccountStatus.INACTIVE);
            userRepository.save(u);
        });

        auditLogService.record("STUDENT_DELETE", "STUDENT", id, "SUCCESS", null, "Soft deleted student");
    }

    @Transactional
    public StudentResponse changeStatus(UUID id, ChangeStudentStatusRequest request) {
        StudentProfile profile = studentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found."));

        StudentStatus current = profile.getStatus();
        StudentStatus next = request.status();

        validateStatusTransition(current, next);

        profile.setStatus(next);
        studentRepository.save(profile);

        // Update enrollment if non-active
        if (next != StudentStatus.ACTIVE && next != StudentStatus.SUSPENDED) {
            enrollmentRepository.findByClassIdAndStatus(profile.getCurrentClassId(), EnrollmentStatus.ACTIVE).stream()
                    .filter(e -> e.getStudentId().equals(profile.getId()))
                    .findFirst()
                    .ifPresent(e -> {
                        e.setStatus(EnrollmentStatus.WITHDRAWN);
                        e.setEndDate(request.effectiveDate() != null ? request.effectiveDate() : LocalDate.now());
                        enrollmentRepository.save(e);
                    });
            profile.setCurrentClassId(null);
            studentRepository.save(profile);
        }

        auditLogService.record("STUDENT_STATUS_CHANGE", "STUDENT", id, "SUCCESS", null, request.reason());
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return mapToResponse(profile, user, getGuardiansForStudent(profile.getId()), false);
    }

    // --- Guardians ---

    @Transactional(readOnly = true)
    public List<GuardianResponse> listGuardians(UUID studentId) {
        return getGuardiansForStudent(studentId);
    }

    @Transactional
    public GuardianResponse addGuardian(UUID studentId, CreateGuardianRequest request) {
        if (!studentRepository.existsById(studentId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found.");
        }

        if (guardianRepository.countByStudentId(studentId) >= 4) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A student cannot have more than 4 guardians.");
        }

        if (request.isPrimary()) {
            guardianRepository.findByStudentIdAndIsPrimaryTrue(studentId).ifPresent(g -> {
                g.setPrimary(false);
                guardianRepository.save(g);
            });
        }

        Guardian guardian = new Guardian();
        guardian.setStudentId(studentId);
        guardian.setFirstName(request.firstName());
        guardian.setLastName(request.lastName());
        guardian.setRelationship(request.relationship());
        guardian.setPhone(request.phone());
        guardian.setEmail(request.email());
        guardian.setAddress(request.address());
        guardian.setOccupation(request.occupation());
        guardian.setPrimary(request.isPrimary());
        guardian.setCanPickUp(request.canPickUp() == null || request.canPickUp());
        guardian = guardianRepository.save(guardian);

        auditLogService.record("GUARDIAN_ADD", "GUARDIAN", guardian.getId(), "SUCCESS", null, null);
        return mapToGuardianResponse(guardian);
    }

    @Transactional
    public GuardianResponse updateGuardian(UUID studentId, UUID guardianId, UpdateGuardianRequest request) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Guardian not found."));

        if (!guardian.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Guardian not found.");
        }

        if (request.firstName() != null) guardian.setFirstName(request.firstName());
        if (request.lastName() != null) guardian.setLastName(request.lastName());
        if (request.relationship() != null) guardian.setRelationship(request.relationship());
        if (request.phone() != null) guardian.setPhone(request.phone());
        if (request.email() != null) guardian.setEmail(request.email());
        if (request.address() != null) guardian.setAddress(request.address());
        if (request.occupation() != null) guardian.setOccupation(request.occupation());
        if (request.canPickUp() != null) guardian.setCanPickUp(request.canPickUp());

        if (Boolean.TRUE.equals(request.isPrimary())) {
            guardianRepository.findByStudentIdAndIsPrimaryTrue(studentId).ifPresent(other -> {
                if (!other.getId().equals(guardianId)) {
                    other.setPrimary(false);
                    guardianRepository.save(other);
                }
            });
            guardian.setPrimary(true);
        }

        guardianRepository.save(guardian);
        auditLogService.record("GUARDIAN_UPDATE", "GUARDIAN", guardian.getId(), "SUCCESS", null, null);
        return mapToGuardianResponse(guardian);
    }

    @Transactional
    public void deleteGuardian(UUID studentId, UUID guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Guardian not found."));

        if (!guardian.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Guardian not found.");
        }

        long total = guardianRepository.countByStudentId(studentId);
        if (guardian.isPrimary() && total > 1) {
            throw new ApiException(ErrorCode.BUSINESS_GUARDIAN_PRIMARY_REQUIRED, "Cannot delete primary guardian while other guardians exist.");
        }

        guardianRepository.delete(guardian);
        auditLogService.record("GUARDIAN_DELETE", "GUARDIAN", guardianId, "SUCCESS", null, null);
    }

    private void validateStatusTransition(StudentStatus from, StudentStatus to) {
        if (from == to) return;
        boolean valid = switch (from) {
            case ACTIVE -> to == StudentStatus.SUSPENDED || to == StudentStatus.TRANSFERRED || to == StudentStatus.WITHDRAWN || to == StudentStatus.GRADUATED;
            case SUSPENDED -> to == StudentStatus.ACTIVE || to == StudentStatus.WITHDRAWN || to == StudentStatus.TRANSFERRED;
            case WITHDRAWN -> to == StudentStatus.ACTIVE;
            case TRANSFERRED, GRADUATED -> false; // Terminal
        };

        if (!valid) {
            throw new ApiException(ErrorCode.BUSINESS_INVALID_STATUS_TRANSITION, "Cannot change status from " + from + " to " + to + ".")
                    .withContext("from", from.name()).withContext("to", to.name());
        }
    }

    private String generateStudentNumber() {
        int year = Year.now().getValue();
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.format("STU-%d-%06d", year, random);
    }

    private List<GuardianResponse> getGuardiansForStudent(UUID studentId) {
        return guardianRepository.findByStudentId(studentId).stream()
                .map(this::mapToGuardianResponse)
                .toList();
    }

    private StudentResponse mapToResponse(StudentProfile profile, User user, List<GuardianResponse> guardians, boolean maskSensitive) {
        return new StudentResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getStudentNumber(),
                profile.getFirstName(),
                profile.getMiddleName(),
                profile.getLastName(),
                user != null ? user.getEmail() : null,
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getNationality(),
                profile.getAdmissionDate(),
                profile.getStatus(),
                profile.getCurrentClassId(),
                user != null ? user.getPhone() : null,
                maskSensitive ? null : profile.getAddressLine1(),
                maskSensitive ? null : profile.getCity(),
                maskSensitive ? null : profile.getCountry(),
                maskSensitive ? null : profile.getBloodType(),
                maskSensitive ? null : profile.getEmergencyNotes(),
                guardians,
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion()
        );
    }

    private GuardianResponse mapToGuardianResponse(Guardian g) {
        return new GuardianResponse(
                g.getId(),
                g.getStudentId(),
                g.getFirstName(),
                g.getLastName(),
                g.getRelationship(),
                g.getPhone(),
                g.getEmail(),
                g.getAddress(),
                g.getOccupation(),
                g.isPrimary(),
                g.isCanPickUp(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getVersion()
        );
    }
}
