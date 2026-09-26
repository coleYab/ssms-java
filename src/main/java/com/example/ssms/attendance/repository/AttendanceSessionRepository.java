package com.example.ssms.attendance.repository;

import com.example.ssms.attendance.entity.AttendanceSession;
import com.example.ssms.attendance.entity.AttendanceSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceSessionRepository
		extends
			JpaRepository<AttendanceSession, UUID>,
			JpaSpecificationExecutor<AttendanceSession> {

	Optional<AttendanceSession> findByOfferingIdAndDateAndPeriodNumber(UUID offeringId, LocalDate date,
			int periodNumber);

	List<AttendanceSession> findByOfferingId(UUID offeringId);

	List<AttendanceSession> findByDateAndStatus(LocalDate date, AttendanceSessionStatus status);
}
