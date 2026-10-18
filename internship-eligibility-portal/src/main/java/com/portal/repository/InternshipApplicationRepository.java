package com.portal.repository;

import com.portal.entity.ApplicationStatus;
import com.portal.entity.Internship;
import com.portal.entity.InternshipApplication;
import com.portal.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipApplicationRepository extends JpaRepository<InternshipApplication, Long> {

    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    List<InternshipApplication> findByStudent(Student student);

    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    Page<InternshipApplication> findByStudent(Student student, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    List<InternshipApplication> findByInternship(Internship internship);

    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    Page<InternshipApplication> findByInternship(Internship internship, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    List<InternshipApplication> findByInternshipAndStatus(Internship internship, ApplicationStatus status);

    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    Optional<InternshipApplication> findByStudentAndInternship(Student student, Internship internship);

    @Override
    @EntityGraph(attributePaths = {"student", "student.appUser", "internship", "internship.company"})
    Optional<InternshipApplication> findById(Long id);

    long countByStatus(ApplicationStatus status);
}
