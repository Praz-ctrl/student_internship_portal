package com.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resume")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    private String filePath;

    @Lob
    private String rawText; // plain text content of the resume, used for parsing

    @Lob
    private String extractedSkills; // comma separated skills found in the resume

    @Builder.Default
    private Double resumeScore = 0.0; // generic quality/completeness score (0-100)
}
