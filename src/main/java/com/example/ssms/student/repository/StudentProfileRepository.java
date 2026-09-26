package com.example.ssms.student.repository;

import com.example.ssms.student.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID>, JpaSpecificationExecutor<StudentProfile> {

    Optional<StudentProfile> findByUserId(UUID userId);

    Optional<StudentProfile> findByStudentNumber(String studentNumber);

    boolean existsByStudentNumber(String studentNumber);

    List<StudentProfile> findByCurrentClassId(UUID currentClassId);
}
