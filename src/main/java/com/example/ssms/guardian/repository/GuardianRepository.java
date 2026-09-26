package com.example.ssms.guardian.repository;

import com.example.ssms.guardian.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    List<Guardian> findByStudentId(UUID studentId);

    Optional<Guardian> findByStudentIdAndIsPrimaryTrue(UUID studentId);

    long countByStudentId(UUID studentId);
}
