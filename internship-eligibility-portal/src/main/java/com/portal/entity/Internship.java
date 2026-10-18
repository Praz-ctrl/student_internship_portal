package com.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "internship")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String role;

    @Lob
    private String description;

    private String duration;   // e.g. "6 months"
    private Double stipend;
    private String location;
    private String workMode;   // ONLINE / OFFLINE / HYBRID
    private Integer vacancies;
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InternshipStatus status = InternshipStatus.OPEN;

    // Configurable interview pipeline, comma separated InterviewType names in order,
    // e.g. "APTITUDE_TEST,CODING_TEST,TECHNICAL,HR"
    private String interviewStages;

    // one internship has exactly one eligibility rule set
    @OneToOne(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    private EligibilityCriteria eligibilityCriteria;
}
