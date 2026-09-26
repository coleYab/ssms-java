package com.example.ssms.attendance.repository;

import com.example.ssms.attendance.entity.AttendanceRecord;
import com.example.ssms.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository
		extends
			JpaRepository<AttendanceRecord, UUID>,
			JpaSpecificationExecutor<AttendanceRecord> {

	List<AttendanceRecord> findBySessionId(UUID sessionId);

	Optional<AttendanceRecord> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);

	List<AttendanceRecord> findByStudentId(UUID studentId);

	long countBySessionIdAndStatus(UUID sessionId, AttendanceStatus status);

	long countBySessionId(UUID sessionId);
}
