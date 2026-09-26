package com.example.ssms.grade.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "grade_levels", indexes = {@Index(name = "idx_grade_levels_name", columnList = "name", unique = true),
		@Index(name = "idx_grade_levels_code", columnList = "code", unique = true),
		@Index(name = "idx_grade_levels_seq", columnList = "sequence", unique = true)})
public class GradeLevel extends AuditableEntity {

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "code", nullable = false, unique = true, length = 10)
	private String code;

	@Column(name = "sequence", nullable = false, unique = true)
	private int sequence;

	@Column(name = "description", length = 500)
	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
