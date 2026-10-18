package com.portal.controller;

import com.portal.entity.AppUser;
import com.portal.exception.ApiException;
import com.portal.repository.AppUserRepository;
import com.portal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AppUserRepository appUserRepository;

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return appUserRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("enabled", u.isEnabled());
            return m;
        }).toList();
    }

    @PostMapping("/users/{id}/enabled")
    public void setEnabled(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @RequestParam boolean value) {
        if (p.getId().equals(id)) {
            throw new ApiException("You cannot disable your own account", HttpStatus.BAD_REQUEST);
        }
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setEnabled(value);
        appUserRepository.save(user);
    }
}
