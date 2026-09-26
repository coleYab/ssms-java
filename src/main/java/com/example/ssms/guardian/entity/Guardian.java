package com.example.ssms.guardian.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "guardians", indexes = {@Index(name = "idx_guardians_student_id", columnList = "student_id")})
public class Guardian extends AuditableEntity {

	@Column(name = "student_id", nullable = false)
	private UUID studentId;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(name = "relationship", nullable = false, length = 20)
	private GuardianRelationship relationship;

	@Column(name = "phone", nullable = false, length = 30)
	private String phone;

	@Column(name = "email", length = 254)
	private String email;

	@Column(name = "address", length = 250)
	private String address;

	@Column(name = "occupation", length = 100)
	private String occupation;

	@Column(name = "is_primary", nullable = false)
	private boolean isPrimary = false;

	@Column(name = "can_pick_up", nullable = false)
	private boolean canPickUp = true;

	public UUID getStudentId() {
		return studentId;
	}

	public void setStudentId(UUID studentId) {
		this.studentId = studentId;
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

	public GuardianRelationship getRelationship() {
		return relationship;
	}

	public void setRelationship(GuardianRelationship relationship) {
		this.relationship = relationship;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(boolean primary) {
		isPrimary = primary;
	}

	public boolean isCanPickUp() {
		return canPickUp;
	}

	public void setCanPickUp(boolean canPickUp) {
		this.canPickUp = canPickUp;
	}
}
