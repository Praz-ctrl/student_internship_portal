package com.portal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "app_user_id", nullable = false, unique = true)
    private AppUser appUser;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String prn;

    private String department;
    private Integer semester;

    @Builder.Default
    private Double cgpa = 0.0;

    @Builder.Default
    private Integer backlogs = 0;

    private String phone;

    // Comma separated free-text preferences (roles / locations / work mode)
    private String internshipPreferences;

    // Comma separated certification names, e.g. "AWS Cloud Practitioner,Oracle Java SE"
    @Column(length = 2000)
    private String certifications;

    // Free text list of projects
    @Column(length = 4000)
    private String projects;

    // Set to true by faculty once the student's profile/academic data has been verified.
    @Builder.Default
    private boolean verified = false;
}
