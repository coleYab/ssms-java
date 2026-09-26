package com.example.ssms.schoolclass.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "classes", indexes = {
    @Index(name = "idx_classes_year_name", columnList = "academic_year_id,name", unique = true),
    @Index(name = "idx_classes_year_id", columnList = "academic_year_id"),
    @Index(name = "idx_classes_grade_id", columnList = "grade_level_id"),
    @Index(name = "idx_classes_teacher_id", columnList = "homeroom_teacher_id")
})
public class SchoolClass extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "grade_level_id", nullable = false)
    private UUID gradeLevelId;

    @Column(name = "capacity", nullable = false)
    private int capacity = 40;

    @Column(name = "homeroom_teacher_id")
    private UUID homeroomTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SchoolClassStatus status = SchoolClassStatus.ACTIVE;

    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(UUID academicYearId) {
        this.academicYearId = academicYearId;
    }

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public UUID getHomeroomTeacherId() {
        return homeroomTeacherId;
    }

    public void setHomeroomTeacherId(UUID homeroomTeacherId) {
        this.homeroomTeacherId = homeroomTeacherId;
    }

    public SchoolClassStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolClassStatus status) {
        this.status = status;
    }

    public int getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(int enrolledCount) {
        this.enrolledCount = enrolledCount;
    }
}
