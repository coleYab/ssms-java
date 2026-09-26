package com.example.ssms.teacher.service;

import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.auth.service.PasswordService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.ProfileType;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import com.example.ssms.teacher.dto.TeacherDtos.*;
import com.example.ssms.teacher.entity.TeacherProfile;
import com.example.ssms.teacher.entity.TeacherQualification;
import com.example.ssms.teacher.entity.TeacherStatus;
import com.example.ssms.teacher.repository.TeacherProfileRepository;
import com.example.ssms.teacher.repository.TeacherQualificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TeacherService {

    private final TeacherProfileRepository teacherRepository;
    private final TeacherQualificationRepository qualificationRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final AuditLogService auditLogService;

    public TeacherService(
            TeacherProfileRepository teacherRepository,
            TeacherQualificationRepository qualificationRepository,
            UserRepository userRepository,
            PasswordService passwordService,
            AuditLogService auditLogService
    ) {
        this.teacherRepository = teacherRepository;
        this.qualificationRepository = qualificationRepository;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A teacher with this email already exists.")
                    .withContext("conflictingField", "email");
        }

        String employeeNumber = generateEmployeeNumber();

        String tempPass = "TeacherPass1234!";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordService.hash(tempPass));
        user.setRole(Role.TEACHER);
        user.setStatus(AccountStatus.PENDING);
        user.setMustChangePassword(true);
        user.setPhone(request.phone());
        user.setProfileType(ProfileType.TEACHER);
        user = userRepository.save(user);

        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(user.getId());
        profile.setEmployeeNumber(employeeNumber);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setDepartment(request.department());
        profile.setQualification(request.qualification());
        profile.setHireDate(request.hireDate());
        profile.setMaxWeeklyPeriods(request.maxWeeklyPeriods() != null ? request.maxWeeklyPeriods() : 30);
        profile.setStatus(TeacherStatus.ACTIVE);
        profile = teacherRepository.save(profile);

        user.setProfileId(profile.getId());
        userRepository.save(user);

        auditLogService.record("TEACHER_CREATE", "TEACHER", profile.getId(), "SUCCESS", null, null);
        return mapToResponse(profile, user, false);
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> listTeachers(UserPrincipal caller, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
        boolean basicOnly = caller.getRole() == Role.STUDENT || caller.getRole() == Role.TEACHER;
        return teacherRepository.findAll(pageRequest)
                .map(p -> {
                    User user = userRepository.findById(p.getUserId()).orElse(null);
                    return mapToResponse(p, user, basicOnly);
                });
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacher(UserPrincipal caller, UUID id) {
        TeacherProfile profile = teacherRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found."));

        boolean basicOnly = (caller.getRole() == Role.STUDENT) ||
                (caller.getRole() == Role.TEACHER && !profile.getId().equals(caller.getProfileId()));

        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return mapToResponse(profile, user, basicOnly);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getMe(UserPrincipal caller) {
        if (caller.getProfileId() == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No teacher profile linked.");
        }
        return getTeacher(caller, caller.getProfileId());
    }

    @Transactional
    public TeacherResponse updateTeacher(UUID id, UpdateTeacherRequest request) {
        TeacherProfile profile = teacherRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found."));

        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.dateOfBirth() != null) profile.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) profile.setGender(request.gender());
        if (request.department() != null) profile.setDepartment(request.department());
        if (request.qualification() != null) profile.setQualification(request.qualification());
        if (request.maxWeeklyPeriods() != null) profile.setMaxWeeklyPeriods(request.maxWeeklyPeriods());

        teacherRepository.save(profile);
        auditLogService.record("TEACHER_UPDATE", "TEACHER", profile.getId(), "SUCCESS", null, null);

        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return mapToResponse(profile, user, false);
    }

    @Transactional
    public void deleteTeacher(UUID id) {
        TeacherProfile profile = teacherRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found."));

        profile.setDeletedAt(java.time.Instant.now());
        teacherRepository.save(profile);

        userRepository.findById(profile.getUserId()).ifPresent(u -> {
            u.setDeletedAt(java.time.Instant.now());
            u.setStatus(AccountStatus.INACTIVE);
            userRepository.save(u);
        });

        auditLogService.record("TEACHER_DELETE", "TEACHER", id, "SUCCESS", null, null);
    }

    @Transactional
    public TeacherResponse changeStatus(UUID id, ChangeTeacherStatusRequest request) {
        TeacherProfile profile = teacherRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found."));

        validateTeacherStatusTransition(profile.getStatus(), request.status());
        profile.setStatus(request.status());
        teacherRepository.save(profile);

        auditLogService.record("TEACHER_STATUS_CHANGE", "TEACHER", id, "SUCCESS", null, request.reason());
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return mapToResponse(profile, user, false);
    }

    @Transactional(readOnly = true)
    public List<UUID> listQualifications(UUID teacherId) {
        return qualificationRepository.findByTeacherId(teacherId).stream()
                .map(TeacherQualification::getCourseId)
                .toList();
    }

    @Transactional
    public void replaceQualifications(UUID teacherId, ReplaceQualificationsRequest request) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found.");
        }

        qualificationRepository.deleteByTeacherId(teacherId);
        for (UUID courseId : request.courseIds()) {
            TeacherQualification tq = new TeacherQualification();
            tq.setTeacherId(teacherId);
            tq.setCourseId(courseId);
            qualificationRepository.save(tq);
        }

        auditLogService.record("TEACHER_QUALIFICATIONS_UPDATE", "TEACHER", teacherId, "SUCCESS", null, null);
    }

    private void validateTeacherStatusTransition(TeacherStatus from, TeacherStatus to) {
        if (from == to) return;
        boolean valid = switch (from) {
            case ACTIVE -> to == TeacherStatus.ON_LEAVE || to == TeacherStatus.SUSPENDED || to == TeacherStatus.RESIGNED || to == TeacherStatus.RETIRED;
            case ON_LEAVE -> to == TeacherStatus.ACTIVE || to == TeacherStatus.SUSPENDED || to == TeacherStatus.RESIGNED || to == TeacherStatus.RETIRED;
            case SUSPENDED -> to == TeacherStatus.ACTIVE;
            case RESIGNED, RETIRED -> false; // Terminal
        };

        if (!valid) {
            throw new ApiException(ErrorCode.BUSINESS_INVALID_STATUS_TRANSITION, "Cannot change status from " + from + " to " + to + ".")
                    .withContext("from", from.name()).withContext("to", to.name());
        }
    }

    private String generateEmployeeNumber() {
        int year = Year.now().getValue();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("TCH-%d-%04d", year, random);
    }

    private TeacherResponse mapToResponse(TeacherProfile profile, User user, boolean basicOnly) {
        return new TeacherResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getEmployeeNumber(),
                profile.getFirstName(),
                profile.getLastName(),
                user != null ? user.getEmail() : null,
                basicOnly ? null : (user != null ? user.getPhone() : null),
                basicOnly ? null : profile.getDateOfBirth(),
                basicOnly ? null : profile.getGender(),
                profile.getDepartment(),
                basicOnly ? null : profile.getQualification(),
                basicOnly ? null : profile.getHireDate(),
                basicOnly ? 0 : profile.getMaxWeeklyPeriods(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion()
        );
    }
}
