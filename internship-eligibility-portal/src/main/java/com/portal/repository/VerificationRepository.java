package com.portal.repository;

import com.portal.entity.Student;
import com.portal.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    List<Verification> findByStudent(Student student);
    List<Verification> findByStatus(com.portal.entity.VerificationStatus status);
}
