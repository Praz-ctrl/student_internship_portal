package com.portal.service;

import com.portal.dto.RankingDtos.RankedApplicant;
import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application lifecycle. Pipeline enforced here (mirrors the HLD):
 *   Eligible student applies -> resume match score -> ATR/assessment -> FACULTY shortlist
 *   -> COMPANY shortlist -> interview stages -> SELECTED -> offer.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final InternshipApplicationRepository applicationRepository;
    private final EligibilityCriteriaRepository eligibilityCriteriaRepository;
    private final ResumeRepository resumeRepository;
    private final AssessmentRepository assessmentRepository;
    private final InterviewRepository interviewRepository;
    private final EligibilityEngine eligibilityEngine;
    private final ResumeAnalysisService resumeAnalysisService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ---------------- helpers ----------------
    public static List<String> parseStages(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Transactional(readOnly = true)
    public InternshipApplication getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Application not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public InternshipApplication getForCompany(AppUser companyUser, Long id) {
        InternshipApplication app = getById(id);
        if (companyUser.getRole() != Role.COMPANY
                || !app.getInternship().getCompany().getAppUser().getId().equals(companyUser.getId())) {
            throw new ApiException("This application is not for one of your internships", HttpStatus.FORBIDDEN);
        }
        return app;
    }

    public void assertFacultyOrOwnerCompany(AppUser actor, InternshipApplication app) {
        if (actor.getRole() == Role.FACULTY) return;
        if (actor.getRole() == Role.COMPANY
                && app.getInternship().getCompany().getAppUser().getId().equals(actor.getId())) return;
        throw new ApiException("You are not allowed to do this for this application", HttpStatus.FORBIDDEN);
    }

    public void assertCanView(AppUser actor, InternshipApplication app) {
        if (actor.getRole() == Role.FACULTY || actor.getRole() == Role.ADMIN) return;
        if (app.getStudent().getAppUser().getId().equals(actor.getId())) return;
        if (app.getInternship().getCompany().getAppUser().getId().equals(actor.getId())) return;
        throw new ApiException("You are not allowed to view this application", HttpStatus.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public Set<String> passedStages(InternshipApplication app) {
        return interviewRepository.findByApplication(app).stream()
                .filter(i -> i.getResult() == InterviewResult.PASS)
                .map(i -> i.getType().name())
                .collect(Collectors.toSet());
    }

    // ---------------- student side ----------------
    public InternshipApplication apply(Student student, Internship internship) {
        if (internship.getStatus() != InternshipStatus.OPEN) {
            throw new ApiException("This internship is not open for applications", HttpStatus.BAD_REQUEST);
        }
        if (internship.getDeadline() != null && LocalDate.now().isAfter(internship.getDeadline())) {
            throw new ApiException("The application deadline has passed", HttpStatus.BAD_REQUEST);
        }
        Optional<InternshipApplication> existing = applicationRepository.findByStudentAndInternship(student, internship);
        if (existing.isPresent()) {
            throw new ApiException("You have already applied to this internship (status: "
                    + existing.get().getStatus() + ")", HttpStatus.CONFLICT);
        }

        EligibilityCriteria criteria = eligibilityCriteriaRepository.findByInternship(internship).orElse(null);
        EligibilityEngine.EligibilityResult eligibility = eligibilityEngine.evaluate(student, criteria);
        if (!eligibility.eligible) {
            throw new ApiException("Not eligible: " + String.join("; ", eligibility.reasons), HttpStatus.FORBIDDEN);
        }

        Resume resume = resumeRepository.findByStudent(student).orElse(null);
        Double matchScore = null;
        if (resume != null && resume.getRawText() != null) {
            matchScore = resumeAnalysisService.computeMatchScore(resume.getRawText(),
                    criteria != null ? criteria.getRequiredSkills() : null);
        }
        if (criteria != null && criteria.getMinResumeScore() != null) {
            if (matchScore == null) {
                throw new ApiException("This internship requires a resume - upload yours first", HttpStatus.BAD_REQUEST);
            }
            if (matchScore < criteria.getMinResumeScore()) {
                throw new ApiException(String.format("Resume match score %.0f%% is below the required %.0f%%",
                        matchScore, criteria.getMinResumeScore()), HttpStatus.FORBIDDEN);
            }
        }

        InternshipApplication application = applicationRepository.save(InternshipApplication.builder()
                .student(student)
                .internship(internship)
                .status(matchScore != null ? ApplicationStatus.RESUME_SCORED : ApplicationStatus.APPLIED)
                .resumeMatchScore(matchScore)
                .build());

        notificationService.notify(internship.getCompany().getAppUser(), "APPLICATION",
                student.getName() + " applied for " + internship.getRole());
        notificationService.notify(student.getAppUser(), "APPLICATION",
                "Your application for " + internship.getRole() + " at "
                        + internship.getCompany().getCompanyName() + " was submitted");

        auditService.logEvent(student.getAppUser().getId(), student.getAppUser().getEmail(), "STUDENT",
                "APPLY_INTERNSHIP", "APPLICATION", application.getId(),
                "Applied for " + internship.getRole() + " at " + internship.getCompany().getCompanyName(), null);

        return application;
    }

    @Transactional(readOnly = true)
    public List<InternshipApplication> myApplications(Student student) {
        return applicationRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public List<InternshipApplication> applicationsFor(Internship internship) {
        return applicationRepository.findByInternship(internship);
    }

    @Transactional(readOnly = true)
    public List<InternshipApplication> all() {
        return applicationRepository.findAll();
    }

    public void withdraw(Student student, Long applicationId) {
        InternshipApplication app = getById(applicationId);
        if (!app.getStudent().getId().equals(student.getId())) {
            throw new ApiException("This application does not belong to you", HttpStatus.FORBIDDEN);
        }
        if (app.getStatus() == ApplicationStatus.SELECTED || app.getStatus() == ApplicationStatus.REJECTED
                || app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ApiException("This application can no longer be withdrawn", HttpStatus.BAD_REQUEST);
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(app);
    }

    // ---------------- faculty side ----------------
    /** Faculty filter step. Enforces the internship's minimum ATR score when one is configured. */
    public InternshipApplication facultyShortlist(Long applicationId) {
        InternshipApplication app = getById(applicationId);
        if (app.getStatus() != ApplicationStatus.APPLIED && app.getStatus() != ApplicationStatus.RESUME_SCORED) {
            throw new ApiException("Only newly applied candidates can be shortlisted by faculty (current status: "
                    + app.getStatus() + ")", HttpStatus.BAD_REQUEST);
        }
        EligibilityCriteria criteria = eligibilityCriteriaRepository.findByInternship(app.getInternship()).orElse(null);
        if (criteria != null && criteria.getMinAtrScore() != null) {
            Double atr = assessmentRepository.findByApplication(app).map(Assessment::getAtrScore).orElse(null);
            if (atr == null) {
                throw new ApiException("Record the ATR score before shortlisting (minimum required: "
                        + criteria.getMinAtrScore() + ")", HttpStatus.BAD_REQUEST);
            }
            if (atr < criteria.getMinAtrScore()) {
                throw new ApiException("ATR score " + atr + " is below the required " + criteria.getMinAtrScore(),
                        HttpStatus.BAD_REQUEST);
            }
        }
        app.setStatus(ApplicationStatus.FACULTY_SHORTLISTED);
        applicationRepository.save(app);
        notificationService.notify(app.getStudent().getAppUser(), "SHORTLIST",
                "Faculty shortlisted you for " + app.getInternship().getRole());
        auditService.logCurrentAction("FACULTY_SHORTLIST", "APPLICATION", applicationId,
                "Shortlisted student " + app.getStudent().getName() + " for " + app.getInternship().getRole());
        return app;
    }

    // ---------------- company side ----------------
    public InternshipApplication companyShortlist(AppUser companyUser, Long applicationId) {
        InternshipApplication app = getForCompany(companyUser, applicationId);
        if (app.getStatus() != ApplicationStatus.FACULTY_SHORTLISTED) {
            throw new ApiException("Faculty must shortlist this candidate before the company can (current status: "
                    + app.getStatus() + ")", HttpStatus.BAD_REQUEST);
        }
        app.setStatus(ApplicationStatus.COMPANY_SHORTLISTED);
        applicationRepository.save(app);
        notificationService.notify(app.getStudent().getAppUser(), "SHORTLIST",
                app.getInternship().getCompany().getCompanyName() + " shortlisted you for " + app.getInternship().getRole());
        auditService.logEvent(companyUser.getId(), companyUser.getEmail(), "COMPANY",
                "COMPANY_SHORTLIST", "APPLICATION", applicationId,
                "Company shortlisted student " + app.getStudent().getName() + " for " + app.getInternship().getRole(), null);
        return app;
    }

    /** Final selection: every configured interview stage must have been passed. */
    public InternshipApplication select(AppUser companyUser, Long applicationId) {
        InternshipApplication app = getForCompany(companyUser, applicationId);
        if (app.getStatus() != ApplicationStatus.COMPANY_SHORTLISTED
                && app.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new ApiException("Only company-shortlisted candidates can be selected (current status: "
                    + app.getStatus() + ")", HttpStatus.BAD_REQUEST);
        }
        List<String> stages = parseStages(app.getInternship().getInterviewStages());
        if (!stages.isEmpty()) {
            Set<String> passed = passedStages(app);
            List<String> pending = stages.stream().filter(s -> !passed.contains(s)).toList();
            if (!pending.isEmpty()) {
                throw new ApiException("Interview stages still to be passed: " + String.join(", ", pending),
                        HttpStatus.BAD_REQUEST);
            }
        }
        app.setStatus(ApplicationStatus.SELECTED);
        applicationRepository.save(app);
        notificationService.notify(app.getStudent().getAppUser(), "SELECTION",
                "Congratulations! You have been selected for " + app.getInternship().getRole());
        auditService.logEvent(companyUser.getId(), companyUser.getEmail(), "COMPANY",
                "SELECT_CANDIDATE", "APPLICATION", applicationId,
                "Selected student " + app.getStudent().getName() + " for " + app.getInternship().getRole(), null);
        return app;
    }

    /** Faculty (any application) or the owning company can reject a candidate. */
    public InternshipApplication reject(AppUser actor, Long applicationId, String reason) {
        InternshipApplication app = getById(applicationId);
        assertFacultyOrOwnerCompany(actor, app);
        if (app.getStatus() == ApplicationStatus.SELECTED || app.getStatus() == ApplicationStatus.REJECTED
                || app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ApiException("This application is already closed (status: " + app.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }
        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
        notificationService.notify(app.getStudent().getAppUser(), "REJECTION",
                "Your application for " + app.getInternship().getRole() + " was not selected"
                        + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        auditService.logEvent(actor.getId(), actor.getEmail(), actor.getRole().name(),
                "REJECT_CANDIDATE", "APPLICATION", applicationId,
                "Rejected student " + app.getStudent().getName() + " for " + app.getInternship().getRole() + ". Reason: " + reason, null);
        return app;
    }

    /** Candidate ranking: composite = 0.6 * resume match + 0.4 * ATR score. */
    @Transactional(readOnly = true)
    public List<RankedApplicant> rank(Internship internship) {
        return applicationRepository.findByInternship(internship).stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN)
                .map(a -> {
                    Double atr = assessmentRepository.findByApplication(a).map(Assessment::getAtrScore).orElse(null);
                    double resume = a.getResumeMatchScore() != null ? a.getResumeMatchScore() : 0.0;
                    double composite = resume * 0.6 + (atr != null ? atr : 0.0) * 0.4;
                    return new RankedApplicant(a, a.getResumeMatchScore(), atr, Math.round(composite * 10.0) / 10.0);
                })
                .sorted(Comparator.comparingDouble(RankedApplicant::compositeScore).reversed())
                .toList();
    }
}
