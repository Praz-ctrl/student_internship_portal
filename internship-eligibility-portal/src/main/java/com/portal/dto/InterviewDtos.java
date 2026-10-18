package com.portal.dto;

import com.portal.entity.InterviewResult;
import com.portal.entity.InterviewType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class InterviewDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleInterviewRequest {
        @NotNull(message = "Application ID is required")
        private Long applicationId;

        @NotNull(message = "Interview type is required")
        private InterviewType type;

        @Positive(message = "Round number must be positive")
        private Integer round;

        private LocalDateTime scheduledDate;
        private String mode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordResultRequest {
        @NotNull(message = "Interview result is required")
        private InterviewResult result;

        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssessmentRequest {
        @NotNull(message = "Application ID is required")
        private Long applicationId;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private Double atrScore;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private Double aptitudeScore;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private Double codingScore;
    }
}
