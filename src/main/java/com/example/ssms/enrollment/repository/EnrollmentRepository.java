package com.example.ssms.enrollment.repository;

import com.example.ssms.enrollment.entity.Enrollment;
import com.example.ssms.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {

	List<Enrollment> findByClassIdAndStatus(UUID classId, EnrollmentStatus status);

	List<Enrollment> findByStudentIdOrderByStartDateDesc(UUID studentId);

	Optional<Enrollment> findByStudentIdAndAcademicYearIdAndStatus(UUID studentId, UUID academicYearId,
			EnrollmentStatus status);

	Optional<Enrollment> findByStudentIdAndClassIdAndStatus(UUID studentId, UUID classId, EnrollmentStatus status);

	long countByClassIdAndStatus(UUID classId, EnrollmentStatus status);

	boolean existsByStudentIdAndAcademicYearIdAndStatus(UUID studentId, UUID academicYearId, EnrollmentStatus status);
}
