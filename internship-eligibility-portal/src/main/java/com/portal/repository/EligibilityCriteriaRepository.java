package com.portal.repository;

import com.portal.entity.EligibilityCriteria;
import com.portal.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EligibilityCriteriaRepository extends JpaRepository<EligibilityCriteria, Long> {
    Optional<EligibilityCriteria> findByInternship(Internship internship);
}
