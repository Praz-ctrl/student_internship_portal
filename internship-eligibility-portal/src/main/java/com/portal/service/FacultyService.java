package com.portal.service;

import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final StudentDocumentRepository documentRepository;
    private final VerificationRepository verificationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Faculty getByAppUser(AppUser user) {
        return facultyRepository.findByAppUser(user)
                .orElseThrow(() -> new ApiException("Faculty profile not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Student> allStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<StudentDocument> documentsOf(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ApiException("Student not found", HttpStatus.NOT_FOUND));
        return documentRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public StudentDocument getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException("Document not found", HttpStatus.NOT_FOUND));
    }

    public StudentDocument verifyDocument(Long documentId, VerificationStatus status) {
        StudentDocument doc = getDocument(documentId);
        doc.setVerificationStatus(status);
        documentRepository.save(doc);
        notificationService.notify(doc.getStudent().getAppUser(), "ELIGIBILITY",
                "Your " + doc.getDocumentType() + " document was " + status.name().toLowerCase());
        auditService.logCurrentAction("VERIFY_DOCUMENT_" + status.name(), "DOCUMENT", documentId,
                "DocumentType: " + doc.getDocumentType() + " for student ID " + doc.getStudent().getId());
        return doc;
    }

    /**
     * Records a verification decision. A PROFILE verification flips Student.verified, which is
     * what the Eligibility Engine checks before a student may apply anywhere.
     */
    public Verification recordVerification(AppUser facultyUser, Long studentId, String type,
                                           VerificationStatus status, String remarks) {
        Faculty faculty = getByAppUser(facultyUser);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ApiException("Student not found", HttpStatus.NOT_FOUND));

        Verification v = verificationRepository.save(Verification.builder()
                .student(student)
                .faculty(faculty)
                .verificationType(type.toUpperCase())
                .status(status)
                .remarks(remarks)
                .build());

        if ("PROFILE".equalsIgnoreCase(type)) {
            student.setVerified(status == VerificationStatus.VERIFIED);
            studentRepository.save(student);
        }
        notificationService.notify(student.getAppUser(), "ELIGIBILITY",
                type.toUpperCase() + " verification marked " + status.name().toLowerCase() + " by faculty"
                        + (remarks != null && !remarks.isBlank() ? " (" + remarks + ")" : ""));

        auditService.logEvent(facultyUser.getId(), facultyUser.getEmail(), facultyUser.getRole().name(),
                "VERIFY_STUDENT_" + status.name(), "STUDENT", studentId,
                "Type: " + type + ", Remarks: " + remarks, null);

        return v;
    }
}
