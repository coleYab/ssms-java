package com.example.ssms.academic.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "terms", indexes = {
    @Index(name = "idx_terms_year_id", columnList = "academic_year_id"),
    @Index(name = "idx_terms_year_name", columnList = "academic_year_id,name", unique = true),
    @Index(name = "idx_terms_year_seq", columnList = "academic_year_id,sequence", unique = true)
})
public class Term extends AuditableEntity {

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(UUID academicYearId) {
        this.academicYearId = academicYearId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
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
}
