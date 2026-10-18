package com.portal.repository;

import com.portal.entity.Student;
import com.portal.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {
    List<StudentSkill> findByStudent(Student student);
    void deleteByStudent(Student student);
}
