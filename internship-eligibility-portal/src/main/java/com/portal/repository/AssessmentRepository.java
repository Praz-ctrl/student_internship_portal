package com.portal.repository;

import com.portal.entity.Assessment;
import com.portal.entity.InternshipApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByApplication(InternshipApplication application);
}
