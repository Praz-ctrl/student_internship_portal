package com.portal.repository;

import com.portal.entity.VerificationStatus;
import com.portal.entity.Student;
import com.portal.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {
    List<StudentDocument> findByStudent(Student student);
    List<StudentDocument> findByVerificationStatus(VerificationStatus status);
}
