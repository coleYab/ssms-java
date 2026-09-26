package com.example.ssms.teacher.repository;

import com.example.ssms.teacher.entity.TeacherQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherQualificationRepository extends JpaRepository<TeacherQualification, UUID> {

	List<TeacherQualification> findByTeacherId(UUID teacherId);

	List<TeacherQualification> findByCourseId(UUID courseId);

	boolean existsByTeacherIdAndCourseId(UUID teacherId, UUID courseId);

	void deleteByTeacherId(UUID teacherId);
}
