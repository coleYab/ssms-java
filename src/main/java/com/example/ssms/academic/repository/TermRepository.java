package com.example.ssms.academic.repository;

import com.example.ssms.academic.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermRepository extends JpaRepository<Term, UUID>, JpaSpecificationExecutor<Term> {

    List<Term> findByAcademicYearIdOrderBySequenceAsc(UUID academicYearId);

    Optional<Term> findByAcademicYearIdAndNameIgnoreCase(UUID academicYearId, String name);

    Optional<Term> findByAcademicYearIdAndSequence(UUID academicYearId, int sequence);

    @Query("SELECT t FROM Term t WHERE t.academicYearId = :yearId AND t.deletedAt IS NULL AND ((t.startDate <= :end AND t.endDate >= :start)) AND (:excludeId IS NULL OR t.id != :excludeId)")
    List<Term> findOverlapping(UUID yearId, LocalDate start, LocalDate end, UUID excludeId);
}
