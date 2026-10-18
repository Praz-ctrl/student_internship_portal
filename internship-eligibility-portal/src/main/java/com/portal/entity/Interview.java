package com.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private InternshipApplication application;

    @Enumerated(EnumType.STRING)
    private InterviewType type;

    private Integer round;
    private LocalDateTime scheduledDate;
    private String mode; // ONLINE / OFFLINE

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterviewResult result = InterviewResult.PENDING;

    @Lob
    private String remarks;
}
