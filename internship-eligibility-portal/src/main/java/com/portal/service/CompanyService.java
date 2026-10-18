package com.portal.service;

import com.portal.dto.InternshipDtos.CreateInternshipRequest;
import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.CompanyRepository;
import com.portal.repository.EligibilityCriteriaRepository;
import com.portal.repository.InternshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final InternshipRepository internshipRepository;
    private final EligibilityCriteriaRepository eligibilityCriteriaRepository;
    private final InternshipService internshipService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.require-internship-approval:true}")
    private boolean requireApproval;

    @Transactional(readOnly = true)
    public Company getByAppUser(AppUser user) {
        return companyRepository.findByAppUser(user)
                .orElseThrow(() -> new ApiException("Company profile not found", HttpStatus.NOT_FOUND));
    }

    public Internship createInternship(AppUser user, CreateInternshipRequest req) {
        Company company = getByAppUser(user);
        if (req.getRole() == null || req.getRole().isBlank()) {
            throw new ApiException("Internship role is required", HttpStatus.BAD_REQUEST);
        }

        Internship internship = internshipRepository.save(Internship.builder()
                .company(company)
                .role(req.getRole().trim())
                .description(req.getDescription())
                .duration(req.getDuration())
                .stipend(req.getStipend())
                .location(req.getLocation())
                .workMode(req.getWorkMode())
                .vacancies(req.getVacancies())
                .deadline(req.getDeadline())
                .interviewStages(req.getInterviewStages())
                .status(requireApproval ? InternshipStatus.PENDING_APPROVAL : InternshipStatus.OPEN)
                .build());

        EligibilityCriteria criteria = eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                .internship(internship)
                .minCgpa(req.getMinCgpa() != null ? req.getMinCgpa() : 0.0)
                .maxBacklogs(req.getMaxBacklogs() != null ? req.getMaxBacklogs() : 99)
                .eligibleBranches(req.getEligibleBranches())
                .requiredSkills(req.getRequiredSkills())
                .requiredCertifications(req.getRequiredCertifications())
                .requiredSemester(req.getRequiredSemester())
                .minResumeScore(req.getMinResumeScore())
                .minAtrScore(req.getMinAtrScore())
                .build());
        internship.setEligibilityCriteria(criteria);

        if (internship.getStatus() == InternshipStatus.OPEN) {
            internshipService.notifyEligibleStudents(internship);
        }

        auditService.logEvent(user.getId(), user.getEmail(), user.getRole().name(),
                "CREATE_INTERNSHIP", "INTERNSHIP", internship.getId(),
                "Role: " + internship.getRole() + ", Status: " + internship.getStatus(), null);

        return internship;
    }

    /**
     * Dynamic Preference Engine: a company can change criteria at any time. Students are
     * re-evaluated against the new rules, and students who just became eligible are notified.
     */
    public Internship updateInternship(AppUser user, Long internshipId, CreateInternshipRequest req) {
        Internship internship = getOwnedInternship(user, internshipId);
        EligibilityCriteria criteria = eligibilityCriteriaRepository.findByInternship(internship)
                .orElseGet(() -> EligibilityCriteria.builder().internship(internship).build());

        Set<Long> eligibleBefore = internshipService.eligibleStudents(internship).stream()
                .map(Student::getId).collect(Collectors.toSet());

        if (req.getDescription() != null) internship.setDescription(req.getDescription());
        if (req.getDuration() != null) internship.setDuration(req.getDuration());
        if (req.getStipend() != null) internship.setStipend(req.getStipend());
        if (req.getLocation() != null) internship.setLocation(req.getLocation());
        if (req.getWorkMode() != null) internship.setWorkMode(req.getWorkMode());
        if (req.getVacancies() != null) internship.setVacancies(req.getVacancies());
        if (req.getDeadline() != null) internship.setDeadline(req.getDeadline());
        if (req.getInterviewStages() != null) internship.setInterviewStages(req.getInterviewStages());
        internshipRepository.save(internship);

        if (req.getMinCgpa() != null) criteria.setMinCgpa(req.getMinCgpa());
        if (req.getMaxBacklogs() != null) criteria.setMaxBacklogs(req.getMaxBacklogs());
        if (req.getEligibleBranches() != null) criteria.setEligibleBranches(req.getEligibleBranches());
        if (req.getRequiredSkills() != null) criteria.setRequiredSkills(req.getRequiredSkills());
        if (req.getRequiredCertifications() != null) criteria.setRequiredCertifications(req.getRequiredCertifications());
        if (req.getRequiredSemester() != null) criteria.setRequiredSemester(req.getRequiredSemester());
        if (req.getMinResumeScore() != null) criteria.setMinResumeScore(req.getMinResumeScore());
        if (req.getMinAtrScore() != null) criteria.setMinAtrScore(req.getMinAtrScore());
        eligibilityCriteriaRepository.save(criteria);
        internship.setEligibilityCriteria(criteria);

        if (internship.getStatus() == InternshipStatus.OPEN) {
            for (Student s : internshipService.eligibleStudents(internship)) {
                if (!eligibleBefore.contains(s.getId())) {
                    notificationService.notify(s.getAppUser(), "ELIGIBILITY",
                            "You are now eligible for " + internship.getRole() + " at "
                                    + internship.getCompany().getCompanyName());
                }
            }
        }

        auditService.logEvent(user.getId(), user.getEmail(), user.getRole().name(),
                "UPDATE_INTERNSHIP_CRITERIA", "INTERNSHIP", internshipId,
                "Updated criteria: minCgpa=" + criteria.getMinCgpa() + ", skills=" + criteria.getRequiredSkills(), null);

        return internship;
    }

    @Transactional(readOnly = true)
    public Internship getOwnedInternship(AppUser user, Long internshipId) {
        Company company = getByAppUser(user);
        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ApiException("Internship not found", HttpStatus.NOT_FOUND));
        if (!internship.getCompany().getId().equals(company.getId())) {
            throw new ApiException("This internship does not belong to your company", HttpStatus.FORBIDDEN);
        }
        return internship;
    }

    @Transactional(readOnly = true)
    public List<Internship> myInternships(AppUser user) {
        return internshipRepository.findByCompany(getByAppUser(user));
    }

    public void closeInternship(AppUser user, Long internshipId) {
        Internship internship = getOwnedInternship(user, internshipId);
        internship.setStatus(InternshipStatus.CLOSED);
        internshipRepository.save(internship);
    }
}
