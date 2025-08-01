package com.example.DeliveryTeamDashboard.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.DeliveryTeamDashboard.Entity.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByEmployeeId(Long employeeId);
    List<Resume> findByJobDescriptionId(Long jobDescriptionId);
}
