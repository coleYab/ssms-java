package com.example.ssms.offering.repository;

import com.example.ssms.offering.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseOfferingRepository
		extends
			JpaRepository<CourseOffering, UUID>,
			JpaSpecificationExecutor<CourseOffering> {

	Optional<CourseOffering> findByCourseIdAndClassIdAndTermId(UUID courseId, UUID classId, UUID termId);

	List<CourseOffering> findByClassId(UUID classId);

	List<CourseOffering> findByTeacherId(UUID teacherId);

	List<CourseOffering> findByCourseId(UUID courseId);

	List<CourseOffering> findByTermId(UUID termId);

	boolean existsByCourseId(UUID courseId);
}
