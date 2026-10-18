package com.portal.service;

import com.portal.entity.AppUser;
import com.portal.entity.Notification;
import com.portal.exception.ApiException;
import com.portal.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationStreamService streamService;

    public Notification notify(AppUser recipient, String type, String message) {
        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .build());

        // 1. Live SSE push to connected browser sessions
        streamService.broadcast(recipient.getId(), notification);

        // 2. Asynchronous email delivery
        emailService.sendNotificationEmail(recipient.getEmail(), type, message);

        return notification;
    }

    @Transactional(readOnly = true)
    public List<Notification> getForUser(AppUser user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getForUser(AppUser user, Pageable pageable) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
    }

    public void markRead(AppUser user, Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new ApiException("Not your notification", HttpStatus.FORBIDDEN);
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    public void markAllRead(AppUser user) {
        List<Notification> unread = notificationRepository.findByRecipientAndReadFalse(user);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
