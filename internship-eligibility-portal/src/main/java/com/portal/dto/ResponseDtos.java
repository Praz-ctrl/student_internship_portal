package com.portal.dto;

import com.portal.entity.*;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ResponseDtos {

    @Builder
    public record EligibilityCriteriaDto(
            Long id,
            Double minCgpa,
            Integer maxBacklogs,
            String eligibleBranches,
            String requiredSkills,
            String requiredCertifications,
            Integer requiredSemester,
            Double minResumeScore,
            Double minAtrScore
    ) {
        public static EligibilityCriteriaDto from(EligibilityCriteria c) {
            if (c == null) return null;
            return EligibilityCriteriaDto.builder()
                    .id(c.getId())
                    .minCgpa(c.getMinCgpa())
                    .maxBacklogs(c.getMaxBacklogs())
                    .eligibleBranches(c.getEligibleBranches())
                    .requiredSkills(c.getRequiredSkills())
                    .requiredCertifications(c.getRequiredCertifications())
                    .requiredSemester(c.getRequiredSemester())
                    .minResumeScore(c.getMinResumeScore())
                    .minAtrScore(c.getMinAtrScore())
                    .build();
        }
    }

    @Builder
    public record InternshipResponse(
            Long id,
            Long companyId,
            String companyName,
            String role,
            String description,
            String duration,
            Double stipend,
            String location,
            String workMode,
            Integer vacancies,
            LocalDate deadline,
            String status,
            String interviewStages,
            EligibilityCriteriaDto criteria
    ) {
        public static InternshipResponse from(Internship i) {
            if (i == null) return null;
            return InternshipResponse.builder()
                    .id(i.getId())
                    .companyId(i.getCompany() != null ? i.getCompany().getId() : null)
                    .companyName(i.getCompany() != null ? i.getCompany().getCompanyName() : null)
                    .role(i.getRole())
                    .description(i.getDescription())
                    .duration(i.getDuration())
                    .stipend(i.getStipend())
                    .location(i.getLocation())
                    .workMode(i.getWorkMode())
                    .vacancies(i.getVacancies())
                    .deadline(i.getDeadline())
                    .status(i.getStatus() != null ? i.getStatus().name() : null)
                    .interviewStages(i.getInterviewStages())
                    .criteria(EligibilityCriteriaDto.from(i.getEligibilityCriteria()))
                    .build();
        }
    }

    @Builder
    public record ApplicationResponse(
            Long id,
            Long studentId,
            String studentName,
            String studentPrn,
            String studentDepartment,
            Double studentCgpa,
            Long internshipId,
            String internshipRole,
            String companyName,
            LocalDateTime applicationDate,
            String status,
            Double resumeMatchScore,
            Double atrScore
    ) {
        public static ApplicationResponse from(InternshipApplication a, Double atrScore) {
            if (a == null) return null;
            return ApplicationResponse.builder()
                    .id(a.getId())
                    .studentId(a.getStudent() != null ? a.getStudent().getId() : null)
                    .studentName(a.getStudent() != null ? a.getStudent().getName() : null)
                    .studentPrn(a.getStudent() != null ? a.getStudent().getPrn() : null)
                    .studentDepartment(a.getStudent() != null ? a.getStudent().getDepartment() : null)
                    .studentCgpa(a.getStudent() != null ? a.getStudent().getCgpa() : null)
                    .internshipId(a.getInternship() != null ? a.getInternship().getId() : null)
                    .internshipRole(a.getInternship() != null ? a.getInternship().getRole() : null)
                    .companyName(a.getInternship() != null && a.getInternship().getCompany() != null
                            ? a.getInternship().getCompany().getCompanyName() : null)
                    .applicationDate(a.getApplicationDate())
                    .status(a.getStatus() != null ? a.getStatus().name() : null)
                    .resumeMatchScore(a.getResumeMatchScore())
                    .atrScore(atrScore)
                    .build();
        }

        public static ApplicationResponse from(InternshipApplication a) {
            return from(a, null);
        }
    }

    @Builder
    public record StudentResponse(
            Long id,
            Long userId,
            String email,
            String name,
            String prn,
            String department,
            Integer semester,
            Double cgpa,
            Integer backlogs,
            String phone,
            String internshipPreferences,
            String certifications,
            String projects,
            boolean verified
    ) {
        public static StudentResponse from(Student s) {
            if (s == null) return null;
            return StudentResponse.builder()
                    .id(s.getId())
                    .userId(s.getAppUser() != null ? s.getAppUser().getId() : null)
                    .email(s.getAppUser() != null ? s.getAppUser().getEmail() : null)
                    .name(s.getName())
                    .prn(s.getPrn())
                    .department(s.getDepartment())
                    .semester(s.getSemester())
                    .cgpa(s.getCgpa())
                    .backlogs(s.getBacklogs())
                    .phone(s.getPhone())
                    .internshipPreferences(s.getInternshipPreferences())
                    .certifications(s.getCertifications())
                    .projects(s.getProjects())
                    .verified(s.isVerified())
                    .build();
        }
    }

    @Builder
    public record DocumentResponse(
            Long id,
            Long studentId,
            String documentType,
            String filePath,
            String verificationStatus
    ) {
        public static DocumentResponse from(StudentDocument d) {
            if (d == null) return null;
            return DocumentResponse.builder()
                    .id(d.getId())
                    .studentId(d.getStudent() != null ? d.getStudent().getId() : null)
                    .documentType(d.getDocumentType())
                    .filePath(d.getFilePath())
                    .verificationStatus(d.getVerificationStatus() != null ? d.getVerificationStatus().name() : null)
                    .build();
        }
    }

    @Builder
    public record InterviewResponse(
            Long id,
            Long applicationId,
            String candidateName,
            String internshipRole,
            String type,
            Integer round,
            LocalDateTime scheduledDate,
            String mode,
            String result,
            String remarks
    ) {
        public static InterviewResponse from(Interview i) {
            if (i == null) return null;
            return InterviewResponse.builder()
                    .id(i.getId())
                    .applicationId(i.getApplication() != null ? i.getApplication().getId() : null)
                    .candidateName(i.getApplication() != null && i.getApplication().getStudent() != null
                            ? i.getApplication().getStudent().getName() : null)
                    .internshipRole(i.getApplication() != null && i.getApplication().getInternship() != null
                            ? i.getApplication().getInternship().getRole() : null)
                    .type(i.getType() != null ? i.getType().name() : null)
                    .round(i.getRound())
                    .scheduledDate(i.getScheduledDate())
                    .mode(i.getMode())
                    .result(i.getResult() != null ? i.getResult().name() : null)
                    .remarks(i.getRemarks())
                    .build();
        }
    }

    @Builder
    public record OfferResponse(
            Long id,
            Long applicationId,
            String studentName,
            String companyName,
            String internshipRole,
            LocalDate offerDate,
            String status
    ) {
        public static OfferResponse from(Offer o) {
            if (o == null) return null;
            return OfferResponse.builder()
                    .id(o.getId())
                    .applicationId(o.getApplication() != null ? o.getApplication().getId() : null)
                    .studentName(o.getApplication() != null && o.getApplication().getStudent() != null
                            ? o.getApplication().getStudent().getName() : null)
                    .companyName(o.getApplication() != null && o.getApplication().getInternship() != null
                            && o.getApplication().getInternship().getCompany() != null
                            ? o.getApplication().getInternship().getCompany().getCompanyName() : null)
                    .internshipRole(o.getApplication() != null && o.getApplication().getInternship() != null
                            ? o.getApplication().getInternship().getRole() : null)
                    .offerDate(o.getOfferDate())
                    .status(o.getStatus() != null ? o.getStatus().name() : null)
                    .build();
        }
    }
}
