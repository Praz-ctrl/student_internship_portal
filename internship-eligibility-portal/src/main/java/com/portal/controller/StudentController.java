package com.portal.controller;

import com.portal.dto.InternshipDtos.StudentInternshipView;
import com.portal.dto.StudentDtos.AcademicRecordRequest;
import com.portal.dto.StudentDtos.ProfileUpdateRequest;
import com.portal.dto.StudentDtos.ResumeUploadRequest;
import com.portal.dto.StudentDtos.SkillsUpdateRequest;
import com.portal.entity.*;
import com.portal.security.UserPrincipal;
import com.portal.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final InternshipService internshipService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;
    private final OfferService offerService;
    private final FileStorageService fileStorageService;

    // ---- profile ----
    @GetMapping("/me")
    public Student myProfile(@AuthenticationPrincipal UserPrincipal p) {
        return studentService.getByAppUser(p.getAppUser());
    }

    @PutMapping("/me")
    public Student updateProfile(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ProfileUpdateRequest req) {
        return studentService.updateProfile(p.getAppUser(), req);
    }

    // ---- semester-wise academic records ----
    @GetMapping("/me/academic-records")
    public List<AcademicRecord> records(@AuthenticationPrincipal UserPrincipal p) {
        return studentService.records(p.getAppUser());
    }

    @PostMapping("/me/academic-records")
    public AcademicRecord addRecord(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody AcademicRecordRequest req) {
        return studentService.addRecord(p.getAppUser(), req);
    }

    @DeleteMapping("/me/academic-records/{id}")
    public void deleteRecord(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        studentService.deleteRecord(p.getAppUser(), id);
    }

    @PostMapping("/me/academic-records/recalculate-cgpa")
    public Student recalculateCgpa(@AuthenticationPrincipal UserPrincipal p) {
        return studentService.recalculateCgpa(p.getAppUser());
    }

    // ---- skills ----
    @GetMapping("/me/skills")
    public List<StudentSkill> mySkills(@AuthenticationPrincipal UserPrincipal p) {
        return studentService.getSkills(p.getAppUser());
    }

    @PutMapping("/me/skills")
    public List<StudentSkill> updateSkills(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody SkillsUpdateRequest req) {
        return studentService.replaceSkills(p.getAppUser(), req.getSkills());
    }

    // ---- resume ----
    @GetMapping("/me/resume")
    public Resume myResume(@AuthenticationPrincipal UserPrincipal p) {
        return studentService.getResume(p.getAppUser());
    }

    @PostMapping("/me/resume")
    public Resume resumeText(@AuthenticationPrincipal UserPrincipal p, @RequestBody ResumeUploadRequest req) {
        return studentService.saveResumeText(p.getAppUser(), req.getRawText());
    }

    @PostMapping(value = "/me/resume/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resume resumeFile(@AuthenticationPrincipal UserPrincipal p, @RequestParam("file") MultipartFile file) {
        return studentService.saveResumeFile(p.getAppUser(), file);
    }

    @GetMapping("/me/resume/file")
    public ResponseEntity<Resource> downloadResume(@AuthenticationPrincipal UserPrincipal p) {
        Resume resume = studentService.getResume(p.getAppUser());
        return fileStorageService.asResponse(resume == null ? null : resume.getFilePath());
    }

    // ---- documents ----
    @GetMapping("/me/documents")
    public List<StudentDocument> myDocuments(@AuthenticationPrincipal UserPrincipal p) {
        return studentService.getDocuments(p.getAppUser());
    }

    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentDocument uploadDocument(@AuthenticationPrincipal UserPrincipal p,
                                          @RequestParam String documentType,
                                          @RequestParam("file") MultipartFile file) {
        return studentService.uploadDocument(p.getAppUser(), documentType, file);
    }

    @GetMapping("/me/documents/{id}/file")
    public ResponseEntity<Resource> downloadDocument(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return fileStorageService.asResponse(studentService.getOwnDocument(p.getAppUser(), id).getFilePath());
    }

    // ---- internships & applications ----
    @GetMapping("/me/internships")
    public List<StudentInternshipView> browse(@AuthenticationPrincipal UserPrincipal p) {
        return internshipService.viewsFor(studentService.getByAppUser(p.getAppUser()));
    }

    @GetMapping("/me/eligible-internships")
    public List<Internship> eligibleInternships(@AuthenticationPrincipal UserPrincipal p) {
        return internshipService.eligibleFor(studentService.getByAppUser(p.getAppUser()));
    }

    @PostMapping("/me/applications/{internshipId}")
    public InternshipApplication apply(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long internshipId) {
        Student student = studentService.getByAppUser(p.getAppUser());
        return applicationService.apply(student, internshipService.getById(internshipId));
    }

    @GetMapping("/me/applications")
    public List<InternshipApplication> myApplications(@AuthenticationPrincipal UserPrincipal p) {
        return applicationService.myApplications(studentService.getByAppUser(p.getAppUser()));
    }

    @DeleteMapping("/me/applications/{applicationId}")
    public void withdraw(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId) {
        applicationService.withdraw(studentService.getByAppUser(p.getAppUser()), applicationId);
    }

    @GetMapping("/me/applications/{applicationId}/interviews")
    public List<Interview> interviews(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId) {
        return interviewService.forApplication(p.getAppUser(), applicationId);
    }

    // ---- offers ----
    @GetMapping("/me/offers")
    public List<Offer> myOffers(@AuthenticationPrincipal UserPrincipal p) {
        return offerService.forStudent(studentService.getByAppUser(p.getAppUser()));
    }

    @PostMapping("/me/offers/{offerId}/respond")
    public Offer respond(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long offerId,
                         @RequestParam OfferStatus status) {
        return offerService.respond(studentService.getByAppUser(p.getAppUser()), offerId, status);
    }
}
