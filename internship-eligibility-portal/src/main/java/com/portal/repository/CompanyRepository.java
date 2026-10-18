package com.portal.repository;

import com.portal.entity.AppUser;
import com.portal.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByAppUser(AppUser appUser);
}
