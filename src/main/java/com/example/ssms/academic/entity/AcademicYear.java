package com.example.ssms.academic.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "academic_years", indexes = {
    @Index(name = "idx_academic_years_name", columnList = "name", unique = true),
    @Index(name = "idx_academic_years_status", columnList = "status")
})
public class AcademicYear extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AcademicYearStatus status = AcademicYearStatus.PLANNED;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public AcademicYearStatus getStatus() {
        return status;
    }

    public void setStatus(AcademicYearStatus status) {
        this.status = status;
    }
}
