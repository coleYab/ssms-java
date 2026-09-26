package com.example.ssms.offering.repository;

import com.example.ssms.offering.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, UUID> {

	List<ScheduleSlot> findByOfferingId(UUID offeringId);

	void deleteByOfferingId(UUID offeringId);
}
