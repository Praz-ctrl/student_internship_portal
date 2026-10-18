package com.portal.repository;

import com.portal.entity.InternshipApplication;
import com.portal.entity.Offer;
import com.portal.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    Optional<Offer> findByApplication(InternshipApplication application);
    List<Offer> findByApplicationStudent(Student student);
}
