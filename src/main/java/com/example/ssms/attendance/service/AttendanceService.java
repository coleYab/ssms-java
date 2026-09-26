package com.example.ssms.attendance.service;

import com.example.ssms.attendance.dto.AttendanceDtos.*;
import com.example.ssms.attendance.entity.*;
import com.example.ssms.attendance.repository.*;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.enrollment.entity.Enrollment;
import com.example.ssms.enrollment.entity.EnrollmentStatus;
import com.example.ssms.enrollment.repository.EnrollmentRepository;
import com.example.ssms.offering.entity.CourseOffering;
import com.example.ssms.offering.repository.CourseOfferingRepository;
import com.example.ssms.security.Role;
import com.example.ssms.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceService {

	private final AttendanceSessionRepository sessionRepository;
	private final AttendanceRecordRepository recordRepository;
	private final AttendanceRecordHistoryRepository historyRepository;
	private final AbsenceExcuseRepository excuseRepository;
	private final CourseOfferingRepository offeringRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final AuditLogService auditLogService;

	public AttendanceService(AttendanceSessionRepository sessionRepository, AttendanceRecordRepository recordRepository,
			AttendanceRecordHistoryRepository historyRepository, AbsenceExcuseRepository excuseRepository,
			CourseOfferingRepository offeringRepository, EnrollmentRepository enrollmentRepository,
			AuditLogService auditLogService) {
		this.sessionRepository = sessionRepository;
		this.recordRepository = recordRepository;
		this.historyRepository = historyRepository;
		this.excuseRepository = excuseRepository;
		this.offeringRepository = offeringRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public SessionResponse openSession(UserPrincipal caller, OpenSessionRequest request) {
		if (request.date().isAfter(LocalDate.now())) {
			throw new ApiException(ErrorCode.BUSINESS_ATTENDANCE_DATE_INVALID,
					"Attendance cannot be recorded for a future date.");
		}

		CourseOffering offering = offeringRepository.findById(request.offeringId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Offering not found."));

		if (sessionRepository
				.findByOfferingIdAndDateAndPeriodNumber(offering.getId(), request.date(), request.periodNumber())
				.isPresent()) {
			throw new ApiException(ErrorCode.BUSINESS_ATTENDANCE_SESSION_EXISTS);
		}

		AttendanceSession session = new AttendanceSession();
		session.setOfferingId(offering.getId());
		session.setDate(request.date());
		session.setPeriodNumber(request.periodNumber());
		session.setStatus(AttendanceSessionStatus.OPEN);
		session.setTakenBy(caller != null ? caller.getId() : null);
		session = sessionRepository.save(session);

		// Pre-create UNMARKED records for all enrolled students
		List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(offering.getClassId(),
				EnrollmentStatus.ACTIVE);
		List<RecordResponse> records = new ArrayList<>();
		for (Enrollment enrollment : enrollments) {
			AttendanceRecord record = new AttendanceRecord();
			record.setSessionId(session.getId());
			record.setStudentId(enrollment.getStudentId());
			record.setStatus(AttendanceStatus.UNMARKED);
			record = recordRepository.save(record);
			records.add(mapToRecordResponse(record));
		}

		auditLogService.record("ATTENDANCE_SESSION_OPEN", "ATTENDANCE_SESSION", session.getId(), "SUCCESS", null, null);
		return mapToSessionResponse(session, records);
	}

	@Transactional(readOnly = true)
	public Page<SessionResponse> listSessions(UserPrincipal caller, UUID offeringId, LocalDate date, int page,
			int limit) {
		PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
		return sessionRepository.findAll(pageRequest).map(s -> mapToSessionResponse(s, null));
	}

	@Transactional(readOnly = true)
	public SessionResponse getSession(UserPrincipal caller, UUID id) {
		AttendanceSession session = sessionRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		List<RecordResponse> records = recordRepository.findBySessionId(session.getId()).stream().filter(r -> {
			if (caller.getRole() == Role.STUDENT) {
				return r.getStudentId().equals(caller.getProfileId());
			}
			return true;
		}).map(this::mapToRecordResponse).toList();

		return mapToSessionResponse(session, records);
	}

	@Transactional
	public void deleteSession(UserPrincipal caller, UUID id) {
		if (caller != null && !caller.isStepUpValid(600)) {
			throw new ApiException(ErrorCode.AUTH_REAUTH_REQUIRED);
		}

		AttendanceSession session = sessionRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		sessionRepository.delete(session);
		auditLogService.record("ATTENDANCE_SESSION_DELETE", "ATTENDANCE_SESSION", id, "SUCCESS", null,
				"Session deleted");
	}

	@Transactional
	public BulkMarkResponse bulkMark(UserPrincipal caller, UUID sessionId, BulkMarkRequest request) {
		AttendanceSession session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		if (session.getStatus() == AttendanceSessionStatus.SUBMITTED) {
			throw new ApiException(ErrorCode.BUSINESS_ATTENDANCE_SESSION_SUBMITTED);
		}

		int updated = 0;
		for (MarkRecordItem item : request.records()) {
			AttendanceRecord record = recordRepository.findBySessionIdAndStudentId(sessionId, item.studentId())
					.orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_ATTENDANCE_STUDENT_NOT_IN_SESSION));

			record.setStatus(item.status());
			record.setArrivalTime(item.arrivalTime());
			record.setNote(item.note());
			recordRepository.save(record);
			updated++;
		}

		Map<String, Long> counts = new HashMap<>();
		for (AttendanceStatus st : AttendanceStatus.values()) {
			counts.put(st.name(), recordRepository.countBySessionIdAndStatus(sessionId, st));
		}

		auditLogService.record("ATTENDANCE_BULK_MARK", "ATTENDANCE_SESSION", sessionId, "SUCCESS", null,
				"Updated " + updated + " records");
		return new BulkMarkResponse(sessionId, session.getStatus(), counts, updated);
	}

	@Transactional
	public RecordResponse correctRecord(UserPrincipal caller, UUID recordId, CorrectRecordRequest request) {
		AttendanceRecord record = recordRepository.findById(recordId)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Record not found."));

		AttendanceSession session = sessionRepository.findById(record.getSessionId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		if (session.getStatus() == AttendanceSessionStatus.SUBMITTED
				&& (request.reason() == null || request.reason().isBlank())) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED,
					"Reason is required when correcting a submitted session.");
		}

		AttendanceStatus oldStatus = record.getStatus();
		record.setStatus(request.status());
		record.setArrivalTime(request.arrivalTime());
		record.setNote(request.note());
		recordRepository.save(record);

		if (oldStatus != request.status()) {
			AttendanceRecordHistory history = new AttendanceRecordHistory();
			history.setRecordId(record.getId());
			history.setPreviousStatus(oldStatus);
			history.setNewStatus(request.status());
			history.setReason(request.reason() != null ? request.reason() : "Correction");
			history.setChangedBy(caller != null ? caller.getId() : null);
			historyRepository.save(history);
		}

		auditLogService.record("ATTENDANCE_RECORD_CORRECT", "ATTENDANCE_RECORD", recordId, "SUCCESS", null,
				request.reason());
		return mapToRecordResponse(record);
	}

	@Transactional
	public SessionResponse submitSession(UserPrincipal caller, UUID id) {
		AttendanceSession session = sessionRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		if (session.getStatus() == AttendanceSessionStatus.SUBMITTED) {
			throw new ApiException(ErrorCode.BUSINESS_ATTENDANCE_SESSION_SUBMITTED);
		}

		long unmarkedCount = recordRepository.countBySessionIdAndStatus(id, AttendanceStatus.UNMARKED);
		if (unmarkedCount > 0) {
			long total = recordRepository.countBySessionId(id);
			throw new ApiException(ErrorCode.BUSINESS_ATTENDANCE_INCOMPLETE,
					"Attendance must be marked for all " + total + " students before submitting.")
					.withContext("count", total);
		}

		session.setStatus(AttendanceSessionStatus.SUBMITTED);
		session.setSubmittedAt(Instant.now());
		sessionRepository.save(session);

		auditLogService.record("ATTENDANCE_SESSION_SUBMIT", "ATTENDANCE_SESSION", id, "SUCCESS", null, null);
		return mapToSessionResponse(session, null);
	}

	@Transactional
	public SessionResponse reopenSession(UserPrincipal caller, UUID id, ReopenSessionRequest request) {
		AttendanceSession session = sessionRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found."));

		if (session.getStatus() != AttendanceSessionStatus.SUBMITTED) {
			throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Session is not submitted.");
		}

		session.setStatus(AttendanceSessionStatus.OPEN);
		session.setSubmittedAt(null);
		sessionRepository.save(session);

		auditLogService.record("ATTENDANCE_SESSION_REOPEN", "ATTENDANCE_SESSION", id, "SUCCESS", null,
				request.reason());
		return mapToSessionResponse(session, null);
	}

	@Transactional(readOnly = true)
	public Page<RecordResponse> queryRecords(UserPrincipal caller, UUID studentId, AttendanceStatus status, int page,
			int limit) {
		PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
		return recordRepository.findAll(pageRequest).map(this::mapToRecordResponse);
	}

	// --- Excuses ---

	@Transactional
	public ExcuseResponse submitExcuse(UserPrincipal caller, CreateExcuseRequest request) {
		AttendanceRecord record = recordRepository.findById(request.recordId())
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Record not found."));

		if (record.getStatus() != AttendanceStatus.ABSENT && record.getStatus() != AttendanceStatus.LATE) {
			throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE,
					"Excuses can only be filed for ABSENT or LATE records.");
		}

		if (excuseRepository.findByRecordId(record.getId()).isPresent()) {
			throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "An excuse has already been filed for this record.");
		}

		UUID studentId = (caller.getRole() == Role.STUDENT)
				? caller.getProfileId()
				: (request.studentId() != null ? request.studentId() : record.getStudentId());

		AbsenceExcuse excuse = new AbsenceExcuse();
		excuse.setRecordId(record.getId());
		excuse.setStudentId(studentId);
		excuse.setReasonCode(request.reasonCode());
		excuse.setDescription(request.description());
		excuse.setStatus(ExcuseStatus.PENDING);
		excuse = excuseRepository.save(excuse);

		auditLogService.record("EXCUSE_SUBMIT", "ABSENCE_EXCUSE", excuse.getId(), "SUCCESS", null, null);
		return mapToExcuseResponse(excuse);
	}

	@Transactional(readOnly = true)
	public Page<ExcuseResponse> listExcuses(UserPrincipal caller, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
		return excuseRepository.findAll(pageRequest).map(this::mapToExcuseResponse);
	}

	@Transactional(readOnly = true)
	public ExcuseResponse getExcuse(UserPrincipal caller, UUID id) {
		AbsenceExcuse excuse = excuseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Excuse not found."));
		return mapToExcuseResponse(excuse);
	}

	@Transactional
	public ExcuseResponse reviewExcuse(UserPrincipal caller, UUID id, ReviewExcuseRequest request) {
		AbsenceExcuse excuse = excuseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Excuse not found."));

		if (excuse.getStatus() != ExcuseStatus.PENDING) {
			throw new ApiException(ErrorCode.BUSINESS_EXCUSE_ALREADY_REVIEWED);
		}

		boolean approved = "APPROVED".equalsIgnoreCase(request.decision());
		excuse.setStatus(approved ? ExcuseStatus.APPROVED : ExcuseStatus.REJECTED);
		excuse.setReviewedBy(caller != null ? caller.getId() : null);
		excuse.setReviewedAt(Instant.now());
		excuse.setReviewComment(request.comment());
		excuseRepository.save(excuse);

		if (approved) {
			recordRepository.findById(excuse.getRecordId()).ifPresent(record -> {
				record.setStatus(AttendanceStatus.EXCUSED);
				recordRepository.save(record);
			});
		}

		auditLogService.record("EXCUSE_REVIEW", "ABSENCE_EXCUSE", id, "SUCCESS", null,
				"Decision: " + request.decision());
		return mapToExcuseResponse(excuse);
	}

	@Transactional
	public void deleteExcuse(UserPrincipal caller, UUID id) {
		AbsenceExcuse excuse = excuseRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Excuse not found."));

		if (excuse.getStatus() != ExcuseStatus.PENDING) {
			throw new ApiException(ErrorCode.BUSINESS_EXCUSE_ALREADY_REVIEWED);
		}

		excuseRepository.delete(excuse);
		auditLogService.record("EXCUSE_DELETE", "ABSENCE_EXCUSE", id, "SUCCESS", null, null);
	}

	private SessionResponse mapToSessionResponse(AttendanceSession session, List<RecordResponse> records) {
		return new SessionResponse(session.getId(), session.getOfferingId(), session.getDate(),
				session.getPeriodNumber(), session.getStatus(), session.getTakenBy(), session.getSubmittedAt(), records,
				session.getCreatedAt(), session.getUpdatedAt(), session.getVersion());
	}

	private RecordResponse mapToRecordResponse(AttendanceRecord record) {
		return new RecordResponse(record.getId(), record.getSessionId(), record.getStudentId(), record.getStatus(),
				record.getArrivalTime(), record.getNote(), record.getCreatedAt(), record.getUpdatedAt(),
				record.getVersion());
	}

	private ExcuseResponse mapToExcuseResponse(AbsenceExcuse excuse) {
		return new ExcuseResponse(excuse.getId(), excuse.getRecordId(), excuse.getStudentId(), excuse.getReasonCode(),
				excuse.getDescription(), excuse.getStatus(), excuse.getReviewedBy(), excuse.getReviewedAt(),
				excuse.getReviewComment(), excuse.getCreatedAt(), excuse.getUpdatedAt(), excuse.getVersion());
	}
}
