package com.portal.repository;

import com.portal.entity.AppUser;
import com.portal.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"appUser"})
    Optional<Student> findByAppUser(AppUser appUser);

    @EntityGraph(attributePaths = {"appUser"})
    Optional<Student> findByAppUserId(Long appUserId);

    @Override
    @EntityGraph(attributePaths = {"appUser"})
    Optional<Student> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"appUser"})
    List<Student> findAll();

    @EntityGraph(attributePaths = {"appUser"})
    Page<Student> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"appUser"})
    Page<Student> findByVerified(boolean verified, Pageable pageable);

    long countByDepartment(String department);
    long countByVerified(boolean verified);
}
