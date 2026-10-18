package com.portal.controller;

import com.portal.dto.InternshipDtos.CreateInternshipRequest;
import com.portal.dto.InternshipDtos.EligibleStudentView;
import com.portal.dto.RankingDtos.RankedApplicant;
import com.portal.entity.Company;
import com.portal.entity.Internship;
import com.portal.entity.InternshipApplication;
import com.portal.entity.Offer;
import com.portal.security.UserPrincipal;
import com.portal.service.ApplicationService;
import com.portal.service.CompanyService;
import com.portal.service.InternshipService;
import com.portal.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final InternshipService internshipService;
    private final OfferService offerService;

    @GetMapping("/me")
    public Company myProfile(@AuthenticationPrincipal UserPrincipal p) {
        return companyService.getByAppUser(p.getAppUser());
    }

    @PostMapping("/me/internships")
    public Internship createInternship(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody CreateInternshipRequest req) {
        return companyService.createInternship(p.getAppUser(), req);
    }

    /** Dynamic preferences: change any field/criteria at any time; students are re-evaluated. */
    @PutMapping({"/me/internships/{id}", "/me/internships/{id}/criteria"})
    public Internship updateInternship(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
                                       @Valid @RequestBody CreateInternshipRequest req) {
        return companyService.updateInternship(p.getAppUser(), id, req);
    }

    @GetMapping("/me/internships")
    public List<Internship> myInternships(@AuthenticationPrincipal UserPrincipal p) {
        return companyService.myInternships(p.getAppUser());
    }

    @PostMapping("/me/internships/{id}/close")
    public void closeInternship(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        companyService.closeInternship(p.getAppUser(), id);
    }

    @GetMapping("/me/internships/{id}/applications")
    public List<InternshipApplication> applications(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return applicationService.applicationsFor(companyService.getOwnedInternship(p.getAppUser(), id));
    }

    /** Applicants ranked by composite score (0.6 resume match + 0.4 ATR). */
    @GetMapping("/me/internships/{id}/ranking")
    public List<RankedApplicant> ranking(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return applicationService.rank(companyService.getOwnedInternship(p.getAppUser(), id));
    }

    /** Live preview of which students currently satisfy this internship's criteria. */
    @GetMapping("/me/internships/{id}/eligible-students")
    public List<EligibleStudentView> eligibleStudents(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        Internship internship = companyService.getOwnedInternship(p.getAppUser(), id);
        return internshipService.eligibleStudents(internship).stream()
                .map(s -> new EligibleStudentView(s.getId(), s.getName(), s.getDepartment(), s.getCgpa(),
                        s.getBacklogs(), s.isVerified()))
                .toList();
    }

    @PostMapping("/me/applications/{applicationId}/shortlist")
    public InternshipApplication shortlist(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId) {
        return applicationService.companyShortlist(p.getAppUser(), applicationId);
    }

    @PostMapping("/me/applications/{applicationId}/select")
    public InternshipApplication select(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId) {
        return applicationService.select(p.getAppUser(), applicationId);
    }

    @PostMapping("/me/applications/{applicationId}/reject")
    public InternshipApplication reject(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId,
                                        @RequestParam(required = false) String reason) {
        return applicationService.reject(p.getAppUser(), applicationId, reason);
    }

    @PostMapping("/me/applications/{applicationId}/offer")
    public Offer offer(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long applicationId) {
        return offerService.extend(p.getAppUser(), applicationId);
    }
}
