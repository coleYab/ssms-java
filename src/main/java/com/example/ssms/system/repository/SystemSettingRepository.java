package com.example.ssms.system.repository;

import com.example.ssms.system.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

	Optional<SystemSetting> findBySettingKey(String settingKey);

	boolean existsBySettingKey(String settingKey);

	default Optional<SystemSetting> findByKey(String key) {
		return findBySettingKey(key);
	}

	default boolean existsByKey(String key) {
		return existsBySettingKey(key);
	}
}
