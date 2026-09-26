package com.example.ssms.admin.repository;

import com.example.ssms.admin.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID>, JpaSpecificationExecutor<AdminProfile> {
    Optional<AdminProfile> findByUserId(UUID userId);
}
