package com.example.ssms.offering.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "schedule_slots", indexes = {
    @Index(name = "idx_slots_offering_id", columnList = "offering_id"),
    @Index(name = "idx_slots_day_period", columnList = "day_of_week,period_number")
})
public class ScheduleSlot extends AuditableEntity {

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek; // 1 (Mon) - 7 (Sun)

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    public UUID getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(UUID offeringId) {
        this.offeringId = offeringId;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(int periodNumber) {
        this.periodNumber = periodNumber;
    }
}
