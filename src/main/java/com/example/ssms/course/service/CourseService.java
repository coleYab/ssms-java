package com.example.ssms.course.service;

import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.course.dto.CourseDtos.*;
import com.example.ssms.course.entity.Course;
import com.example.ssms.course.entity.CourseStatus;
import com.example.ssms.course.repository.CourseRepository;
import com.example.ssms.offering.repository.CourseOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CourseService {

	private final CourseRepository courseRepository;
	private final CourseOfferingRepository courseOfferingRepository;
	private final AuditLogService auditLogService;

	public CourseService(CourseRepository courseRepository, CourseOfferingRepository courseOfferingRepository,
			AuditLogService auditLogService) {
		this.courseRepository = courseRepository;
		this.courseOfferingRepository = courseOfferingRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public CourseResponse createCourse(CreateCourseRequest request) {
		String code = request.courseCode().toUpperCase().trim();
		if (courseRepository.findByCourseCodeIgnoreCase(code).isPresent()) {
			throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A course with this code already exists.")
					.withContext("conflictingField", "courseCode");
		}

		Course course = new Course();
		course.setCourseCode(code);
		course.setName(request.name().trim());
		course.setDescription(request.description());
		course.setDepartment(request.department());
		course.setGradeLevelId(request.gradeLevelId());
		course.setCreditHours(request.creditHours());
		course.setElective(Boolean.TRUE.equals(request.isElective()));
		course.setStatus(CourseStatus.ACTIVE);
		course = courseRepository.save(course);

		auditLogService.record("COURSE_CREATE", "COURSE", course.getId(), "SUCCESS", null, null);
		return mapToResponse(course);
	}

	@Transactional(readOnly = true)
	public Page<CourseResponse> listCourses(int page, int limit) {
		PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
		return courseRepository.findAll(pageRequest).map(this::mapToResponse);
	}

	@Transactional(readOnly = true)
	public CourseResponse getCourse(UUID id) {
		Course course = courseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found."));
		return mapToResponse(course);
	}

	@Transactional
	public CourseResponse updateCourse(UUID id, UpdateCourseRequest request) {
		Course course = courseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found."));

		boolean hasOfferings = courseOfferingRepository.existsByCourseId(id);

		if (request.courseCode() != null && !request.courseCode().trim().equalsIgnoreCase(course.getCourseCode())) {
			if (hasOfferings) {
				throw new ApiException(ErrorCode.CONFLICT_IN_USE, "Course code is immutable once offerings exist.");
			}
			if (courseRepository.findByCourseCodeIgnoreCase(request.courseCode().trim()).isPresent()) {
				throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A course with this code already exists.");
			}
			course.setCourseCode(request.courseCode().trim().toUpperCase());
		}

		if (request.gradeLevelId() != null && !request.gradeLevelId().equals(course.getGradeLevelId())) {
			if (hasOfferings) {
				throw new ApiException(ErrorCode.CONFLICT_IN_USE, "Grade level is immutable once offerings exist.");
			}
			course.setGradeLevelId(request.gradeLevelId());
		}

		if (request.name() != null)
			course.setName(request.name().trim());
		if (request.description() != null)
			course.setDescription(request.description());
		if (request.department() != null)
			course.setDepartment(request.department());
		if (request.creditHours() != null)
			course.setCreditHours(request.creditHours());
		if (request.isElective() != null)
			course.setElective(request.isElective());

		courseRepository.save(course);
		auditLogService.record("COURSE_UPDATE", "COURSE", course.getId(), "SUCCESS", null, null);
		return mapToResponse(course);
	}

	@Transactional
	public void deleteCourse(UUID id) {
		Course course = courseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found."));

		if (courseOfferingRepository.existsByCourseId(id)) {
			throw new ApiException(ErrorCode.CONFLICT_IN_USE, "Cannot delete course that has offerings.");
		}

		course.setDeletedAt(java.time.Instant.now());
		courseRepository.save(course);
		auditLogService.record("COURSE_DELETE", "COURSE", id, "SUCCESS", null, null);
	}

	@Transactional
	public CourseResponse archiveCourse(UUID id) {
		Course course = courseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found."));

		if (course.getStatus() == CourseStatus.ARCHIVED) {
			throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Course is already archived.");
		}

		course.setStatus(CourseStatus.ARCHIVED);
		courseRepository.save(course);
		auditLogService.record("COURSE_ARCHIVE", "COURSE", id, "SUCCESS", null, null);
		return mapToResponse(course);
	}

	@Transactional
	public CourseResponse unarchiveCourse(UUID id) {
		Course course = courseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found."));

		if (course.getStatus() == CourseStatus.ACTIVE) {
			throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Course is already active.");
		}

		course.setStatus(CourseStatus.ACTIVE);
		courseRepository.save(course);
		auditLogService.record("COURSE_UNARCHIVE", "COURSE", id, "SUCCESS", null, null);
		return mapToResponse(course);
	}

	private CourseResponse mapToResponse(Course course) {
		return new CourseResponse(course.getId(), course.getCourseCode(), course.getName(), course.getDescription(),
				course.getDepartment(), course.getGradeLevelId(), course.getCreditHours(), course.isElective(),
				course.getStatus(), course.getCreatedAt(), course.getUpdatedAt(), course.getVersion());
	}
}
