package com.portal.controller;

import com.portal.dto.InternshipDtos.EligibilityResultResponse;
import com.portal.entity.Internship;
import com.portal.entity.Student;
import com.portal.security.UserPrincipal;
import com.portal.service.InternshipService;
import com.portal.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {

    private final InternshipService internshipService;
    private final StudentService studentService;

    @GetMapping("/public")
    public List<Internship> listOpenInternships() {
        return internshipService.listOpen();
    }

    @GetMapping("/public/{id}")
    public Internship getInternship(@PathVariable Long id) {
        return internshipService.getById(id);
    }

    @GetMapping("/{id}/eligibility")
    public EligibilityResultResponse checkEligibility(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        Student student = studentService.getByAppUser(p.getAppUser());
        return internshipService.checkEligibility(student, internshipService.getById(id));
    }
}
