package com.portal.service;

import com.portal.dto.InternshipDtos.EligibilityResultResponse;
import com.portal.dto.InternshipDtos.StudentInternshipView;
import com.portal.entity.EligibilityCriteria;
import com.portal.entity.Internship;
import com.portal.entity.InternshipStatus;
import com.portal.entity.Student;
import com.portal.exception.ApiException;
import com.portal.repository.EligibilityCriteriaRepository;
import com.portal.repository.InternshipApplicationRepository;
import com.portal.repository.InternshipRepository;
import com.portal.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InternshipService {

    private final InternshipRepository internshipRepository;
    private final EligibilityCriteriaRepository eligibilityCriteriaRepository;
    private final InternshipApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final EligibilityEngine eligibilityEngine;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Internship> listOpen() {
        return internshipRepository.findByStatus(InternshipStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public Internship getById(Long id) {
        return internshipRepository.findById(id)
                .orElseThrow(() -> new ApiException("Internship not found", HttpStatus.NOT_FOUND));
    }

    /** Student profile vs internship requirements -> ELIGIBLE / NOT ELIGIBLE with reasons. */
    @Transactional(readOnly = true)
    public EligibilityResultResponse checkEligibility(Student student, Internship internship) {
        EligibilityCriteria criteria = eligibilityCriteriaRepository.findByInternship(internship).orElse(null);
        EligibilityEngine.EligibilityResult result = eligibilityEngine.evaluate(student, criteria);
        EligibilityResultResponse response = new EligibilityResultResponse();
        response.setEligible(result.eligible);
        response.setReasons(result.reasons);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Internship> eligibleFor(Student student) {
        return listOpen().stream()
                .filter(i -> checkEligibility(student, i).isEligible())
                .toList();
    }

    /** All open internships, each tagged ELIGIBLE / NOT ELIGIBLE (with reasons) for this student. */
    @Transactional(readOnly = true)
    public List<StudentInternshipView> viewsFor(Student student) {
        return listOpen().stream().map(i -> {
            EligibilityResultResponse r = checkEligibility(student, i);
            boolean applied = applicationRepository.findByStudentAndInternship(student, i).isPresent();
            return new StudentInternshipView(i, r.isEligible(), r.getReasons(), applied);
        }).toList();
    }

    /** Dynamic preference engine: which students satisfy the internship's *current* criteria. */
    @Transactional(readOnly = true)
    public List<Student> eligibleStudents(Internship internship) {
        EligibilityCriteria criteria = eligibilityCriteriaRepository.findByInternship(internship).orElse(null);
        return studentRepository.findAll().stream()
                .filter(s -> eligibilityEngine.evaluate(s, criteria).eligible)
                .toList();
    }

    public void notifyEligibleStudents(Internship internship) {
        for (Student s : eligibleStudents(internship)) {
            notificationService.notify(s.getAppUser(), "NEW_INTERNSHIP",
                    "New internship you are eligible for: " + internship.getRole()
                            + " at " + internship.getCompany().getCompanyName());
        }
    }

    // ---------------- faculty approval ----------------
    @Transactional(readOnly = true)
    public List<Internship> pendingApproval() {
        return internshipRepository.findByStatus(InternshipStatus.PENDING_APPROVAL);
    }

    public Internship approve(Long id) {
        Internship internship = getById(id);
        if (internship.getStatus() != InternshipStatus.PENDING_APPROVAL) {
            throw new ApiException("Only internships pending approval can be approved", HttpStatus.BAD_REQUEST);
        }
        internship.setStatus(InternshipStatus.OPEN);
        internshipRepository.save(internship);
        notificationService.notify(internship.getCompany().getAppUser(), "APPROVAL",
                "Your internship \"" + internship.getRole() + "\" was approved and is now live");
        notifyEligibleStudents(internship);
        return internship;
    }

    public Internship rejectApproval(Long id) {
        Internship internship = getById(id);
        if (internship.getStatus() != InternshipStatus.PENDING_APPROVAL) {
            throw new ApiException("Only internships pending approval can be rejected", HttpStatus.BAD_REQUEST);
        }
        internship.setStatus(InternshipStatus.CLOSED);
        internshipRepository.save(internship);
        notificationService.notify(internship.getCompany().getAppUser(), "APPROVAL",
                "Your internship \"" + internship.getRole() + "\" was not approved by faculty");
        return internship;
    }
}
