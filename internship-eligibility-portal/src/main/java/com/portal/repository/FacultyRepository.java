package com.portal.repository;

import com.portal.entity.AppUser;
import com.portal.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByAppUser(AppUser appUser);
}
