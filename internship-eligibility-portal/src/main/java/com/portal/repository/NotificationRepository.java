package com.portal.repository;

import com.portal.entity.AppUser;
import com.portal.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient);
    Page<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient, Pageable pageable);
    List<Notification> findByRecipientAndReadFalse(AppUser recipient);
}
