package com.portal.service;

import com.portal.dto.InterviewDtos.AssessmentRequest;
import com.portal.dto.InterviewDtos.ScheduleInterviewRequest;
import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.AssessmentRepository;
import com.portal.repository.InternshipApplicationRepository;
import com.portal.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Configurable interview pipeline: each internship lists its own ordered stages. */
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InternshipApplicationRepository applicationRepository;
    private final AssessmentRepository assessmentRepository;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public Interview schedule(AppUser actor, ScheduleInterviewRequest req) {
        if (req.getApplicationId() == null || req.getType() == null) {
            throw new ApiException("Application and interview type are required", HttpStatus.BAD_REQUEST);
        }
        InternshipApplication app = applicationService.getForCompany(actor, req.getApplicationId());
        if (app.getStatus() != ApplicationStatus.COMPANY_SHORTLISTED
                && app.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new ApiException("Shortlist the candidate before scheduling interviews", HttpStatus.BAD_REQUEST);
        }

        List<String> stages = ApplicationService.parseStages(app.getInternship().getInterviewStages());
        List<Interview> existing = interviewRepository.findByApplicationOrderByRoundAsc(app);
        String type = req.getType().name();

        if (!stages.isEmpty()) {
            int idx = stages.indexOf(type);
            if (idx < 0) {
                throw new ApiException("This internship's pipeline does not include " + type
                        + ". Configured stages: " + String.join(" -> ", stages), HttpStatus.BAD_REQUEST);
            }
            Set<String> passed = applicationService.passedStages(app);
            for (int i = 0; i < idx; i++) {
                if (!passed.contains(stages.get(i))) {
                    throw new ApiException("Previous stage " + stages.get(i) + " must be passed first",
                            HttpStatus.BAD_REQUEST);
                }
            }
            if (passed.contains(type)) {
                throw new ApiException("This candidate has already passed " + type, HttpStatus.CONFLICT);
            }
        }
        boolean pendingSame = existing.stream()
                .anyMatch(i -> i.getType() == req.getType() && i.getResult() == InterviewResult.PENDING);
        if (pendingSame) {
            throw new ApiException("A pending " + type + " already exists for this candidate", HttpStatus.CONFLICT);
        }

        Interview interview = interviewRepository.save(Interview.builder()
                .application(app)
                .type(req.getType())
                .round(req.getRound() != null ? req.getRound() : existing.size() + 1)
                .scheduledDate(req.getScheduledDate())
                .mode(req.getMode())
                .result(InterviewResult.PENDING)
                .build());

        app.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        applicationRepository.save(app);

        notificationService.notify(app.getStudent().getAppUser(), "INTERVIEW",
                type + " (round " + interview.getRound() + ") scheduled for " + app.getInternship().getRole()
                        + (req.getScheduledDate() != null ? " on " + req.getScheduledDate() : ""));

        auditService.logEvent(actor.getId(), actor.getEmail(), actor.getRole().name(),
                "SCHEDULE_INTERVIEW", "INTERVIEW", interview.getId(),
                "Scheduled " + interview.getType() + " round " + interview.getRound() + " for " + app.getInternship().getRole(), null);

        return interview;
    }

    public Interview recordResult(AppUser actor, Long interviewId, InterviewResult result, String remarks) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ApiException("Interview not found", HttpStatus.NOT_FOUND));
        InternshipApplication app = applicationService.getForCompany(actor, interview.getApplication().getId());

        if (result == null || result == InterviewResult.PENDING) {
            throw new ApiException("Result must be PASS or FAIL", HttpStatus.BAD_REQUEST);
        }
        if (interview.getResult() != InterviewResult.PENDING) {
            throw new ApiException("A result has already been recorded for this interview", HttpStatus.CONFLICT);
        }
        interview.setResult(result);
        interview.setRemarks(remarks);
        interviewRepository.save(interview);

        auditService.logEvent(actor.getId(), actor.getEmail(), actor.getRole().name(),
                "RECORD_INTERVIEW_RESULT", "INTERVIEW", interviewId,
                "Result: " + result.name() + " for " + interview.getType() + " round " + interview.getRound(), null);

        if (result == InterviewResult.FAIL) {
            app.setStatus(ApplicationStatus.REJECTED);
            applicationRepository.save(app);
            notificationService.notify(app.getStudent().getAppUser(), "REJECTION",
                    "You did not clear the " + interview.getType() + " round for " + app.getInternship().getRole());
        } else {
            List<String> stages = ApplicationService.parseStages(app.getInternship().getInterviewStages());
            Set<String> passed = applicationService.passedStages(app);
            Optional<String> next = stages.stream().filter(s -> !passed.contains(s)).findFirst();
            notificationService.notify(app.getStudent().getAppUser(), "INTERVIEW",
                    next.isPresent()
                            ? "You cleared " + interview.getType() + ". Next stage: " + next.get()
                            : "You have cleared all interview stages for " + app.getInternship().getRole());
        }
        return interview;
    }

    @Transactional(readOnly = true)
    public List<Interview> forApplication(AppUser actor, Long applicationId) {
        InternshipApplication app = applicationService.getById(applicationId);
        applicationService.assertCanView(actor, app);
        return interviewRepository.findByApplicationOrderByRoundAsc(app);
    }

    public Assessment recordAssessment(AppUser actor, AssessmentRequest req) {
        if (req.getApplicationId() == null) {
            throw new ApiException("Application is required", HttpStatus.BAD_REQUEST);
        }
        InternshipApplication app = applicationService.getById(req.getApplicationId());
        applicationService.assertFacultyOrOwnerCompany(actor, app);
        checkScore(req.getAtrScore(), "ATR");
        checkScore(req.getAptitudeScore(), "Aptitude");
        checkScore(req.getCodingScore(), "Coding");

        Assessment assessment = assessmentRepository.findByApplication(app)
                .orElseGet(() -> Assessment.builder().application(app).build());
        if (req.getAtrScore() != null) assessment.setAtrScore(req.getAtrScore());
        if (req.getAptitudeScore() != null) assessment.setAptitudeScore(req.getAptitudeScore());
        if (req.getCodingScore() != null) assessment.setCodingScore(req.getCodingScore());
        return assessmentRepository.save(assessment);
    }

    private void checkScore(Double score, String name) {
        if (score != null && (score < 0 || score > 100)) {
            throw new ApiException(name + " score must be between 0 and 100", HttpStatus.BAD_REQUEST);
        }
    }
}
