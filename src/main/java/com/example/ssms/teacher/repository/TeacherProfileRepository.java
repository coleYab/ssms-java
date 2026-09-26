package com.example.ssms.teacher.repository;

import com.example.ssms.teacher.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID>, JpaSpecificationExecutor<TeacherProfile> {

    Optional<TeacherProfile> findByUserId(UUID userId);

    Optional<TeacherProfile> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);
}
