package com.portal.controller;

import com.portal.entity.*;
import com.portal.security.UserPrincipal;
import com.portal.service.ApplicationService;
import com.portal.service.FacultyService;
import com.portal.service.FileStorageService;
import com.portal.service.InternshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService facultyService;
    private final ApplicationService applicationService;
    private final InternshipService internshipService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    public Faculty myProfile(@AuthenticationPrincipal UserPrincipal p) {
        return facultyService.getByAppUser(p.getAppUser());
    }

    // ---- student & document verification ----
    @GetMapping("/students")
    public List<Student> allStudents() {
        return facultyService.allStudents();
    }

    @GetMapping("/students/{studentId}/documents")
    public List<StudentDocument> documents(@PathVariable Long studentId) {
        return facultyService.documentsOf(studentId);
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<Resource> documentFile(@PathVariable Long documentId) {
        return fileStorageService.asResponse(facultyService.getDocument(documentId).getFilePath());
    }

    @PostMapping("/documents/{documentId}/verify")
    public StudentDocument verifyDocument(@PathVariable Long documentId, @RequestParam VerificationStatus status) {
        return facultyService.verifyDocument(documentId, status);
    }

    @PostMapping("/students/{studentId}/verify")
    public Verification verifyStudent(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long studentId,
                                      @RequestParam String type, @RequestParam VerificationStatus status,
                                      @RequestParam(required = false) String remarks) {
        return facultyService.recordVerification(p.getAppUser(), studentId, type, status, remarks);
    }

    // ---- application shortlisting ----
    @GetMapping("/applications")
    public List<InternshipApplication> applications() {
        return applicationService.all();
    }

    @PostMapping("/applications/{applicationId}/shortlist")
    public InternshipApplication shortlist(@PathVariable Long applicationId) {
        return applicationService.facultyShortlist(applicationId);
    }

    @PostMapping("/applications/{applicationId}/reject")
    public InternshipApplication reject(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId,
                                        @RequestParam(required = false) String reason) {
        return applicationService.reject(p.getAppUser(), applicationId, reason);
    }

    // ---- internship approval ----
    @GetMapping("/internships/pending")
    public List<Internship> pendingInternships() {
        return internshipService.pendingApproval();
    }

    @PostMapping("/internships/{id}/approve")
    public Internship approve(@PathVariable Long id) {
        return internshipService.approve(id);
    }

    @PostMapping("/internships/{id}/decline")
    public Internship decline(@PathVariable Long id) {
        return internshipService.rejectApproval(id);
    }
}
