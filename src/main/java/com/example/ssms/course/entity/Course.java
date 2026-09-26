package com.example.ssms.course.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "courses", indexes = {@Index(name = "idx_courses_code", columnList = "course_code", unique = true),
		@Index(name = "idx_courses_grade_id", columnList = "grade_level_id")})
public class Course extends AuditableEntity {

	@Column(name = "course_code", nullable = false, unique = true, length = 20)
	private String courseCode;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "department", nullable = false, length = 100)
	private String department;

	@Column(name = "grade_level_id", nullable = false)
	private UUID gradeLevelId;

	@Column(name = "credit_hours")
	private Double creditHours;

	@Column(name = "is_elective", nullable = false)
	private boolean isElective = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CourseStatus status = CourseStatus.ACTIVE;

	public String getCourseCode() {
		return courseCode;
	}

	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode != null ? courseCode.toUpperCase().trim() : null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public UUID getGradeLevelId() {
		return gradeLevelId;
	}

	public void setGradeLevelId(UUID gradeLevelId) {
		this.gradeLevelId = gradeLevelId;
	}

	public Double getCreditHours() {
		return creditHours;
	}

	public void setCreditHours(Double creditHours) {
		this.creditHours = creditHours;
	}

	public boolean isElective() {
		return isElective;
	}

	public void setElective(boolean elective) {
		isElective = elective;
	}

	public CourseStatus getStatus() {
		return status;
	}

	public void setStatus(CourseStatus status) {
		this.status = status;
	}
}
