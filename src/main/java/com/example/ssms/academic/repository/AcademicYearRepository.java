package com.example.ssms.academic.repository;

import com.example.ssms.academic.entity.AcademicYear;
import com.example.ssms.academic.entity.AcademicYearStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID>, JpaSpecificationExecutor<AcademicYear> {

    Optional<AcademicYear> findByNameIgnoreCase(String name);

    Optional<AcademicYear> findByStatus(AcademicYearStatus status);

    @Query("SELECT y FROM AcademicYear y WHERE y.deletedAt IS NULL AND ((y.startDate <= :end AND y.endDate >= :start)) AND (:excludeId IS NULL OR y.id != :excludeId)")
    List<AcademicYear> findOverlapping(LocalDate start, LocalDate end, UUID excludeId);
}
