package com.example.ssms.teacher.entity;

import com.example.ssms.common.constant.Gender;
import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "teacher_profiles", indexes = {
    @Index(name = "idx_teachers_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_teachers_employee_num", columnList = "employee_number", unique = true),
    @Index(name = "idx_teachers_department", columnList = "department")
})
public class TeacherProfile extends AuditableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "employee_number", nullable = false, unique = true, length = 30)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "qualification", length = 200)
    private String qualification;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "max_weekly_periods", nullable = false)
    private int maxWeeklyPeriods = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeacherStatus status = TeacherStatus.ACTIVE;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public int getMaxWeeklyPeriods() {
        return maxWeeklyPeriods;
    }

    public void setMaxWeeklyPeriods(int maxWeeklyPeriods) {
        this.maxWeeklyPeriods = maxWeeklyPeriods;
    }

    public TeacherStatus getStatus() {
        return status;
    }

    public void setStatus(TeacherStatus status) {
        this.status = status;
    }
}
