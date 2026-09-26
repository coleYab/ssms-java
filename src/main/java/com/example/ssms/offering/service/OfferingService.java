package com.example.ssms.offering.service;

import com.example.ssms.academic.entity.AcademicYear;
import com.example.ssms.academic.entity.AcademicYearStatus;
import com.example.ssms.academic.entity.Term;
import com.example.ssms.academic.repository.AcademicYearRepository;
import com.example.ssms.academic.repository.TermRepository;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.course.entity.Course;
import com.example.ssms.course.repository.CourseRepository;
import com.example.ssms.offering.dto.OfferingDtos.*;
import com.example.ssms.offering.entity.CourseOffering;
import com.example.ssms.offering.entity.OfferingStatus;
import com.example.ssms.offering.entity.ScheduleSlot;
import com.example.ssms.offering.repository.CourseOfferingRepository;
import com.example.ssms.offering.repository.ScheduleSlotRepository;
import com.example.ssms.schoolclass.entity.SchoolClass;
import com.example.ssms.schoolclass.repository.SchoolClassRepository;
import com.example.ssms.teacher.entity.TeacherProfile;
import com.example.ssms.teacher.entity.TeacherStatus;
import com.example.ssms.teacher.repository.TeacherProfileRepository;
import com.example.ssms.teacher.repository.TeacherQualificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OfferingService {

	private final CourseOfferingRepository offeringRepository;
	private final ScheduleSlotRepository slotRepository;
	private final CourseRepository courseRepository;
	private final SchoolClassRepository classRepository;
	private final TermRepository termRepository;
	private final AcademicYearRepository academicYearRepository;
	private final TeacherProfileRepository teacherRepository;
	private final TeacherQualificationRepository qualificationRepository;
	private final AuditLogService auditLogService;

	public OfferingService(CourseOfferingRepository offeringRepository, ScheduleSlotRepository slotRepository,
			CourseRepository courseRepository, SchoolClassRepository classRepository, TermRepository termRepository,
			AcademicYearRepository academicYearRepository, TeacherProfileRepository teacherRepository,
			TeacherQualificationRepository qualificationRepository, AuditLogService auditLogService) {
		this.offeringRepository = offeringRepository;
		this.slotRepository = slotRepository;
		this.courseRepository = courseRepository;
		this.classRepository = classRepository;
		this.termRepository = termRepository;
		this.academicYearRepository = academicYearRepository;
		this.teacherRepository = teacherRepository;
		this.qualificationRepository = qualificationRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public OfferingResponse createOffering(CreateOfferingRequest request) {
		Course course = courseRepository.findById(request.courseId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found."));

		SchoolClass sc = classRepository.findById(request.classId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Class not found."));

		Term term = termRepository.findById(request.termId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Term not found."));

		AcademicYear year = academicYearRepository.findById(term.getAcademicYearId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

		if (year.getStatus() == AcademicYearStatus.CLOSED) {
			throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
		}

		if (offeringRepository.findByCourseIdAndClassIdAndTermId(course.getId(), sc.getId(), term.getId())
				.isPresent()) {
			throw new ApiException(ErrorCode.BUSINESS_OFFERING_DUPLICATE);
		}

		if (!course.isElective() && !course.getGradeLevelId().equals(sc.getGradeLevelId())) {
			throw new ApiException(ErrorCode.BUSINESS_GRADE_MISMATCH);
		}

		TeacherProfile teacher = teacherRepository.findById(request.teacherId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found."));

		if (teacher.getStatus() != TeacherStatus.ACTIVE) {
			throw new ApiException(ErrorCode.BUSINESS_TEACHER_NOT_ACTIVE).withContext("status",
					teacher.getStatus().name());
		}

		// Qualification check
		if (!qualificationRepository.existsByTeacherIdAndCourseId(teacher.getId(), course.getId())) {
			throw new ApiException(ErrorCode.BUSINESS_TEACHER_NOT_QUALIFIED);
		}

		CourseOffering offering = new CourseOffering();
		offering.setCourseId(course.getId());
		offering.setClassId(sc.getId());
		offering.setTermId(term.getId());
		offering.setTeacherId(teacher.getId());
		offering.setWeeklyPeriods(request.weeklyPeriods());
		offering.setStatus(OfferingStatus.ACTIVE);
		offering = offeringRepository.save(offering);

		auditLogService.record("OFFERING_CREATE", "OFFERING", offering.getId(), "SUCCESS", null, null);
		return mapToResponse(offering);
	}

	@Transactional(readOnly = true)
	public Page<OfferingResponse> listOfferings(int page, int limit) {
		PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
		return offeringRepository.findAll(pageRequest).map(this::mapToResponse);
	}

	@Transactional(readOnly = true)
	public OfferingResponse getOffering(UUID id) {
		CourseOffering offering = offeringRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Offering not found."));
		return mapToResponse(offering);
	}

	@Transactional
	public OfferingResponse updateOffering(UUID id, UpdateOfferingRequest request) {
		CourseOffering offering = offeringRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Offering not found."));

		if (request.weeklyPeriods() != null)
			offering.setWeeklyPeriods(request.weeklyPeriods());
		if (request.status() != null)
			offering.setStatus(request.status());

		offeringRepository.save(offering);
		auditLogService.record("OFFERING_UPDATE", "OFFERING", offering.getId(), "SUCCESS", null, null);
		return mapToResponse(offering);
	}

	@Transactional
	public void deleteOffering(UUID id) {
		CourseOffering offering = offeringRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Offering not found."));

		offering.setStatus(OfferingStatus.CANCELLED);
		offeringRepository.save(offering);
		auditLogService.record("OFFERING_DELETE", "OFFERING", id, "SUCCESS", null, null);
	}

	@Transactional(readOnly = true)
	public List<ScheduleSlotDto> getSchedule(UUID offeringId) {
		return slotRepository.findByOfferingId(offeringId).stream()
				.map(s -> new ScheduleSlotDto(s.getDayOfWeek(), s.getPeriodNumber())).toList();
	}

	@Transactional
	public List<ScheduleSlotDto> replaceSchedule(UUID offeringId, ReplaceScheduleRequest request) {
		CourseOffering offering = offeringRepository.findById(offeringId)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Offering not found."));

		slotRepository.deleteByOfferingId(offeringId);
		for (ScheduleSlotDto slotDto : request.slots()) {
			ScheduleSlot slot = new ScheduleSlot();
			slot.setOfferingId(offeringId);
			slot.setDayOfWeek(slotDto.dayOfWeek());
			slot.setPeriodNumber(slotDto.periodNumber());
			slotRepository.save(slot);
		}

		auditLogService.record("OFFERING_SCHEDULE_UPDATE", "OFFERING", offeringId, "SUCCESS", null, null);
		return request.slots();
	}

	@Transactional
	public OfferingResponse reassignTeacher(UUID id, ReassignTeacherRequest request) {
		CourseOffering offering = offeringRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Offering not found."));

		TeacherProfile teacher = teacherRepository.findById(request.teacherId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Teacher not found."));

		if (teacher.getStatus() != TeacherStatus.ACTIVE) {
			throw new ApiException(ErrorCode.BUSINESS_TEACHER_NOT_ACTIVE);
		}

		if (!qualificationRepository.existsByTeacherIdAndCourseId(teacher.getId(), offering.getCourseId())) {
			throw new ApiException(ErrorCode.BUSINESS_TEACHER_NOT_QUALIFIED);
		}

		offering.setTeacherId(teacher.getId());
		offeringRepository.save(offering);

		auditLogService.record("OFFERING_REASSIGN_TEACHER", "OFFERING", id, "SUCCESS", null,
				"Reassigned to " + teacher.getId());
		return mapToResponse(offering);
	}

	private OfferingResponse mapToResponse(CourseOffering offering) {
		List<ScheduleSlotDto> schedule = slotRepository.findByOfferingId(offering.getId()).stream()
				.map(s -> new ScheduleSlotDto(s.getDayOfWeek(), s.getPeriodNumber())).toList();

		return new OfferingResponse(offering.getId(), offering.getCourseId(), offering.getClassId(),
				offering.getTermId(), offering.getTeacherId(), offering.getWeeklyPeriods(), offering.getStatus(),
				schedule, offering.getCreatedAt(), offering.getUpdatedAt(), offering.getVersion());
	}
}
