package com.example.ssms.academic.service;

import com.example.ssms.academic.dto.AcademicDtos.*;
import com.example.ssms.academic.entity.AcademicYear;
import com.example.ssms.academic.entity.AcademicYearStatus;
import com.example.ssms.academic.entity.Term;
import com.example.ssms.academic.repository.AcademicYearRepository;
import com.example.ssms.academic.repository.TermRepository;
import com.example.ssms.audit.service.AuditLogService;
import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.exception.ApiException;
import com.example.ssms.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AcademicService {

    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final AuditLogService auditLogService;

    public AcademicService(
            AcademicYearRepository academicYearRepository,
            TermRepository termRepository,
            AuditLogService auditLogService
    ) {
        this.academicYearRepository = academicYearRepository;
        this.termRepository = termRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AcademicYearResponse createAcademicYear(CreateAcademicYearRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "End date must be after start date.");
        }

        if (academicYearRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "An academic year with this name already exists.")
                    .withContext("conflictingField", "name");
        }

        List<AcademicYear> overlaps = academicYearRepository.findOverlapping(request.startDate(), request.endDate(), null);
        if (!overlaps.isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_OVERLAP, "Academic year dates overlap with " + overlaps.get(0).getName() + ".")
                    .withContext("conflictingYear", overlaps.get(0).getName());
        }

        AcademicYear year = new AcademicYear();
        year.setName(request.name().trim());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(AcademicYearStatus.PLANNED);
        year = academicYearRepository.save(year);

        auditLogService.record("ACADEMIC_YEAR_CREATE", "ACADEMIC_YEAR", year.getId(), "SUCCESS", null, null);
        return mapToYearResponse(year, List.of());
    }

    @Transactional(readOnly = true)
    public Page<AcademicYearResponse> listAcademicYears(int page, int limit) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, limit)));
        return academicYearRepository.findAll(pageRequest)
                .map(y -> mapToYearResponse(y, null));
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrentAcademicYear() {
        AcademicYear year = academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_NO_ACTIVE_ACADEMIC_YEAR));

        List<Term> terms = termRepository.findByAcademicYearIdOrderBySequenceAsc(year.getId());
        return mapToYearResponse(year, terms);
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYear(UUID id, boolean includeTerms) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        List<Term> terms = includeTerms ? termRepository.findByAcademicYearIdOrderBySequenceAsc(year.getId()) : null;
        return mapToYearResponse(year, terms);
    }

    @Transactional
    public AcademicYearResponse updateAcademicYear(UUID id, UpdateAcademicYearRequest request) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        if (request.name() != null && !request.name().trim().equalsIgnoreCase(year.getName())) {
            if (academicYearRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
                throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "An academic year with this name already exists.")
                        .withContext("conflictingField", "name");
            }
            year.setName(request.name().trim());
        }

        LocalDate newStart = request.startDate() != null ? request.startDate() : year.getStartDate();
        LocalDate newEnd = request.endDate() != null ? request.endDate() : year.getEndDate();

        if (!newEnd.isAfter(newStart)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "End date must be after start date.");
        }

        List<AcademicYear> overlaps = academicYearRepository.findOverlapping(newStart, newEnd, year.getId());
        if (!overlaps.isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_OVERLAP, "Academic year dates overlap with " + overlaps.get(0).getName() + ".");
        }

        // Verify terms still fall within new year bounds
        List<Term> terms = termRepository.findByAcademicYearIdOrderBySequenceAsc(year.getId());
        for (Term t : terms) {
            if (t.getStartDate().isBefore(newStart) || t.getEndDate().isAfter(newEnd)) {
                throw new ApiException(ErrorCode.BUSINESS_TERM_OUT_OF_RANGE, "Term dates must fall within the academic year.");
            }
        }

        year.setStartDate(newStart);
        year.setEndDate(newEnd);
        academicYearRepository.save(year);

        auditLogService.record("ACADEMIC_YEAR_UPDATE", "ACADEMIC_YEAR", year.getId(), "SUCCESS", null, null);
        return mapToYearResponse(year, terms);
    }

    @Transactional
    public void deleteAcademicYear(UUID id) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() != AcademicYearStatus.PLANNED) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Only PLANNED academic years can be deleted.");
        }

        List<Term> terms = termRepository.findByAcademicYearIdOrderBySequenceAsc(year.getId());
        if (!terms.isEmpty()) {
            throw new ApiException(ErrorCode.CONFLICT_IN_USE, "Cannot delete academic year with existing terms.")
                    .withContext("dependents", "terms").withContext("count", terms.size());
        }

        academicYearRepository.delete(year);
        auditLogService.record("ACADEMIC_YEAR_DELETE", "ACADEMIC_YEAR", id, "SUCCESS", null, null);
    }

    @Transactional
    public AcademicYearResponse activateAcademicYear(UUID id) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Another academic year is already active.");
        }

        year.setStatus(AcademicYearStatus.ACTIVE);
        academicYearRepository.save(year);

        auditLogService.record("ACADEMIC_YEAR_ACTIVATE", "ACADEMIC_YEAR", year.getId(), "SUCCESS", null, null);
        return mapToYearResponse(year, termRepository.findByAcademicYearIdOrderBySequenceAsc(year.getId()));
    }

    @Transactional
    public AcademicYearResponse closeAcademicYear(UserPrincipal caller, UUID id) {
        if (caller != null && !caller.isStepUpValid(600)) {
            throw new ApiException(ErrorCode.AUTH_REAUTH_REQUIRED);
        }

        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() != AcademicYearStatus.ACTIVE) {
            throw new ApiException(ErrorCode.CONFLICT_INVALID_STATE, "Only ACTIVE academic years can be closed.");
        }

        year.setStatus(AcademicYearStatus.CLOSED);
        academicYearRepository.save(year);

        auditLogService.record("ACADEMIC_YEAR_CLOSE", "ACADEMIC_YEAR", year.getId(), "SUCCESS", null, "Year closed");
        return mapToYearResponse(year, termRepository.findByAcademicYearIdOrderBySequenceAsc(year.getId()));
    }

    // --- Terms ---

    @Transactional(readOnly = true)
    public List<TermResponse> listTerms(UUID yearId) {
        return termRepository.findByAcademicYearIdOrderBySequenceAsc(yearId).stream()
                .map(this::mapToTermResponse)
                .toList();
    }

    @Transactional
    public TermResponse createTerm(UUID yearId, CreateTermRequest request) {
        AcademicYear year = academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        if (!request.endDate().isAfter(request.startDate())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "End date must be after start date.");
        }

        if (request.startDate().isBefore(year.getStartDate()) || request.endDate().isAfter(year.getEndDate())) {
            throw new ApiException(ErrorCode.BUSINESS_TERM_OUT_OF_RANGE);
        }

        if (termRepository.findByAcademicYearIdAndNameIgnoreCase(yearId, request.name().trim()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A term with this name already exists in this academic year.")
                    .withContext("conflictingField", "name");
        }

        if (termRepository.findByAcademicYearIdAndSequence(yearId, request.sequence()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A term with this sequence already exists in this academic year.")
                    .withContext("conflictingField", "sequence");
        }

        List<Term> overlaps = termRepository.findOverlapping(yearId, request.startDate(), request.endDate(), null);
        if (!overlaps.isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_TERM_OVERLAP);
        }

        Term term = new Term();
        term.setAcademicYearId(yearId);
        term.setName(request.name().trim());
        term.setSequence(request.sequence());
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term = termRepository.save(term);

        auditLogService.record("TERM_CREATE", "TERM", term.getId(), "SUCCESS", null, null);
        return mapToTermResponse(term);
    }

    @Transactional(readOnly = true)
    public TermResponse getTerm(UUID id) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Term not found."));
        return mapToTermResponse(term);
    }

    @Transactional
    public TermResponse updateTerm(UUID id, UpdateTermRequest request) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Term not found."));

        AcademicYear year = academicYearRepository.findById(term.getAcademicYearId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        LocalDate newStart = request.startDate() != null ? request.startDate() : term.getStartDate();
        LocalDate newEnd = request.endDate() != null ? request.endDate() : term.getEndDate();

        if (!newEnd.isAfter(newStart)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "End date must be after start date.");
        }

        if (newStart.isBefore(year.getStartDate()) || newEnd.isAfter(year.getEndDate())) {
            throw new ApiException(ErrorCode.BUSINESS_TERM_OUT_OF_RANGE);
        }

        if (request.name() != null && !request.name().trim().equalsIgnoreCase(term.getName())) {
            if (termRepository.findByAcademicYearIdAndNameIgnoreCase(year.getId(), request.name().trim()).isPresent()) {
                throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A term with this name already exists in this academic year.");
            }
            term.setName(request.name().trim());
        }

        if (request.sequence() != null && request.sequence() != term.getSequence()) {
            if (termRepository.findByAcademicYearIdAndSequence(year.getId(), request.sequence()).isPresent()) {
                throw new ApiException(ErrorCode.CONFLICT_DUPLICATE, "A term with this sequence already exists in this academic year.");
            }
            term.setSequence(request.sequence());
        }

        List<Term> overlaps = termRepository.findOverlapping(year.getId(), newStart, newEnd, term.getId());
        if (!overlaps.isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_TERM_OVERLAP);
        }

        term.setStartDate(newStart);
        term.setEndDate(newEnd);
        termRepository.save(term);

        auditLogService.record("TERM_UPDATE", "TERM", term.getId(), "SUCCESS", null, null);
        return mapToTermResponse(term);
    }

    @Transactional
    public void deleteTerm(UUID id) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Term not found."));

        AcademicYear year = academicYearRepository.findById(term.getAcademicYearId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Academic year not found."));

        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw new ApiException(ErrorCode.BUSINESS_ACADEMIC_YEAR_CLOSED);
        }

        termRepository.delete(term);
        auditLogService.record("TERM_DELETE", "TERM", id, "SUCCESS", null, null);
    }

    private AcademicYearResponse mapToYearResponse(AcademicYear year, List<Term> terms) {
        List<TermResponse> termDtos = terms != null ? terms.stream().map(this::mapToTermResponse).toList() : null;
        return new AcademicYearResponse(
                year.getId(),
                year.getName(),
                year.getStartDate(),
                year.getEndDate(),
                year.getStatus(),
                termDtos,
                year.getCreatedAt(),
                year.getUpdatedAt(),
                year.getVersion()
        );
    }

    private TermResponse mapToTermResponse(Term term) {
        return new TermResponse(
                term.getId(),
                term.getAcademicYearId(),
                term.getName(),
                term.getSequence(),
                term.getStartDate(),
                term.getEndDate(),
                term.getCreatedAt(),
                term.getUpdatedAt(),
                term.getVersion()
        );
    }
}
