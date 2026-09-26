package com.example.ssms.schoolclass.repository;

import com.example.ssms.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID>, JpaSpecificationExecutor<SchoolClass> {

	Optional<SchoolClass> findByAcademicYearIdAndNameIgnoreCase(UUID academicYearId, String name);

	List<SchoolClass> findByAcademicYearId(UUID academicYearId);

	List<SchoolClass> findByHomeroomTeacherId(UUID homeroomTeacherId);

	boolean existsByHomeroomTeacherIdAndAcademicYearId(UUID homeroomTeacherId, UUID academicYearId);
}
