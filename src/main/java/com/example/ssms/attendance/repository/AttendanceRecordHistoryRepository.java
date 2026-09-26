package com.example.ssms.attendance.repository;

import com.example.ssms.attendance.entity.AttendanceRecordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRecordHistoryRepository extends JpaRepository<AttendanceRecordHistory, UUID> {
    List<AttendanceRecordHistory> findByRecordIdOrderByCreatedAtDesc(UUID recordId);
}
