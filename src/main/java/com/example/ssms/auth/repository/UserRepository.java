package com.example.ssms.auth.repository;

import com.example.ssms.auth.entity.User;
import com.example.ssms.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	@Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
	long countActiveUsersByRole(Role role);
}
