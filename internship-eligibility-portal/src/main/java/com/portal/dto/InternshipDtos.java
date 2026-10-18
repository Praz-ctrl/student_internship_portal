package com.portal.dto;

import com.portal.entity.Internship;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class InternshipDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateInternshipRequest {
        @NotBlank(message = "Role title is required")
        private String role;

        private String description;
        private String duration;

        @PositiveOrZero(message = "Stipend cannot be negative")
        private Double stipend;

        private String location;
        private String workMode;

        @Positive(message = "Vacancies must be at least 1")
        private Integer vacancies;

        @FutureOrPresent(message = "Deadline must be today or in the future")
        private LocalDate deadline;

        private String interviewStages; // e.g. "APTITUDE_TEST,CODING_TEST,TECHNICAL,HR"

        // eligibility criteria, set together with the internship
        @DecimalMin(value = "0.0", message = "Min CGPA cannot be negative")
        @DecimalMax(value = "10.0", message = "Min CGPA cannot exceed 10.0")
        private Double minCgpa;

        @Min(value = 0, message = "Max backlogs cannot be negative")
        private Integer maxBacklogs;

        private String eligibleBranches;       // comma separated
        private String requiredSkills;         // comma separated
        private String requiredCertifications; // comma separated

        @Min(value = 1, message = "Required semester must be between 1 and 10")
        @Max(value = 10, message = "Required semester must be between 1 and 10")
        private Integer requiredSemester;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private Double minResumeScore;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private Double minAtrScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibilityResultResponse {
        private boolean eligible;
        private List<String> reasons; // reasons for ineligibility, empty if eligible
    }

    /** One row of the student's internship browser: the internship + ELIGIBLE / NOT ELIGIBLE. */
    public record StudentInternshipView(Internship internship, boolean eligible,
                                        List<String> reasons, boolean applied) {}

    /** Company-side preview of which students currently satisfy an internship's criteria. */
    public record EligibleStudentView(Long id, String name, String department, Double cgpa,
                                      Integer backlogs, boolean verified) {}
}
