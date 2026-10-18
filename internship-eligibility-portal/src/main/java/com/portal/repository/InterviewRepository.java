package com.portal.repository;

import com.portal.entity.Student;
import com.portal.entity.InternshipApplication;
import com.portal.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplication(InternshipApplication application);
    List<Interview> findByApplicationStudent(Student student);
    List<Interview> findByApplicationOrderByRoundAsc(InternshipApplication application);
}
