package com.portal.controller;

import com.portal.dto.InterviewDtos.AssessmentRequest;
import com.portal.dto.InterviewDtos.RecordResultRequest;
import com.portal.dto.InterviewDtos.ScheduleInterviewRequest;
import com.portal.entity.Assessment;
import com.portal.entity.Interview;
import com.portal.security.UserPrincipal;
import com.portal.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/** Reachable by COMPANY and FACULTY only; per-application ownership is enforced in the service. */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/schedule")
    public Interview schedule(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ScheduleInterviewRequest req) {
        return interviewService.schedule(p.getAppUser(), req);
    }

    @PostMapping("/{id}/result")
    public Interview recordResult(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
                                  @Valid @RequestBody RecordResultRequest req) {
        return interviewService.recordResult(p.getAppUser(), id, req.getResult(), req.getRemarks());
    }

    @GetMapping("/application/{applicationId}")
    public List<Interview> forApplication(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId) {
        return interviewService.forApplication(p.getAppUser(), applicationId);
    }

    @PostMapping("/assessment")
    public Assessment recordAssessment(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody AssessmentRequest req) {
        return interviewService.recordAssessment(p.getAppUser(), req);
    }
}
