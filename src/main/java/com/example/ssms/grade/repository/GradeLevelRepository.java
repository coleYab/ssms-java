package com.example.ssms.grade.repository;

import com.example.ssms.grade.entity.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, UUID>, JpaSpecificationExecutor<GradeLevel> {

	List<GradeLevel> findAllByOrderBySequenceAsc();

	Optional<GradeLevel> findByNameIgnoreCase(String name);

	Optional<GradeLevel> findByCodeIgnoreCase(String code);

	Optional<GradeLevel> findBySequence(int sequence);
}
