package com.portal.controller;

import com.portal.entity.Notification;
import com.portal.security.UserPrincipal;
import com.portal.service.NotificationService;
import com.portal.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService streamService;

    @GetMapping("/me")
    public List<Notification> mine(@AuthenticationPrincipal UserPrincipal p) {
        return notificationService.getForUser(p.getAppUser());
    }

    @GetMapping("/me/paged")
    public Page<Notification> minePaged(
            @AuthenticationPrincipal UserPrincipal p,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.getForUser(p.getAppUser(), pageable);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        notificationService.markRead(p.getAppUser(), id);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(@AuthenticationPrincipal UserPrincipal p) {
        notificationService.markAllRead(p.getAppUser());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal p) {
        return streamService.subscribe(p.getId());
    }
}
