package com.example.DeliveryTeamDashboard.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.DeliveryTeamDashboard.Entity.JobDescription;

@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findByTitleContainingIgnoreCaseOrClientContainingIgnoreCase(String title, String client);
    List<JobDescription> findByTechnology(String technology);
    List<JobDescription> findByResourceType(String resourceType);
}
