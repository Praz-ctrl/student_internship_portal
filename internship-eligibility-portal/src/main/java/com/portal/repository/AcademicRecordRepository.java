package com.portal.repository;

import com.portal.entity.AcademicRecord;
import com.portal.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    List<AcademicRecord> findByStudent(Student student);
}
