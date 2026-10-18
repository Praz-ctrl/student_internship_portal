package com.portal.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class StudentDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfileUpdateRequest {
        @NotBlank(message = "Name cannot be blank")
        @Size(max = 255)
        private String name;

        @Size(max = 255)
        private String department;

        @Min(value = 1, message = "Semester must be between 1 and 10")
        @Max(value = 10, message = "Semester must be between 1 and 10")
        private Integer semester;

        @DecimalMin(value = "0.0", message = "CGPA cannot be negative")
        @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10.0")
        private Double cgpa;

        @Min(value = 0, message = "Backlogs cannot be negative")
        private Integer backlogs;

        @Size(max = 20)
        private String phone;

        @Size(max = 255)
        private String internshipPreferences;

        @Size(max = 2000)
        private String certifications;

        @Size(max = 4000)
        private String projects;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillsUpdateRequest {
        @NotNull(message = "Skills list cannot be null")
        private List<String> skills; // replaces the existing set
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeUploadRequest {
        @NotBlank(message = "Resume text cannot be blank")
        private String rawText; // plain text pasted by the student
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AcademicRecordRequest {
        @NotNull(message = "Semester is required")
        @Min(value = 1, message = "Semester must be between 1 and 10")
        @Max(value = 10, message = "Semester must be between 1 and 10")
        private Integer semester;

        @NotBlank(message = "Subject cannot be blank")
        private String subject;

        @DecimalMin(value = "0.0", message = "Marks cannot be negative")
        private Double marks;

        @DecimalMin(value = "0.0", message = "SGPA cannot be negative")
        @DecimalMax(value = "10.0", message = "SGPA cannot exceed 10.0")
        private Double sgpa;
    }
}
