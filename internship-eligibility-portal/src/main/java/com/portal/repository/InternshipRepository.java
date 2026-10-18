package com.portal.repository;

import com.portal.entity.Company;
import com.portal.entity.Internship;
import com.portal.entity.InternshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {

    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    List<Internship> findByStatus(InternshipStatus status);

    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    Page<Internship> findByStatus(InternshipStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    List<Internship> findByCompany(Company company);

    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    Page<Internship> findByCompany(Company company, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    List<Internship> findAll();

    @Override
    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    Page<Internship> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"company", "eligibilityCriteria"})
    Optional<Internship> findById(Long id);
}
