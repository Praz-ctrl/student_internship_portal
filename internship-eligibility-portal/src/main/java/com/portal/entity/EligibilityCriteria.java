package com.portal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "eligibility_criteria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // back-reference: ignored in JSON / toString / equals to avoid infinite recursion
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne
    @JoinColumn(name = "internship_id", nullable = false, unique = true)
    private Internship internship;

    @Builder.Default
    private Double minCgpa = 0.0;

    @Builder.Default
    private Integer maxBacklogs = 99;

    // comma separated, e.g. "CSE,IT"
    private String eligibleBranches;

    // comma separated required skills, e.g. "Java,SQL"
    private String requiredSkills;

    // comma separated, optional
    private String requiredCertifications;

    private Integer requiredSemester;

    // minimum resume match score (0-100) needed to apply, optional
    private Double minResumeScore;

    // minimum ATR / assessment score (0-100) needed for faculty shortlisting, optional
    private Double minAtrScore;
}
