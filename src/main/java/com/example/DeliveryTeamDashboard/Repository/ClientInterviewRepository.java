package com.example.DeliveryTeamDashboard.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.DeliveryTeamDashboard.Entity.ClientInterview;

@Repository
public interface ClientInterviewRepository extends JpaRepository<ClientInterview, Long> {
    List<ClientInterview> findByEmployeeId(Long employeeId);
    List<ClientInterview> findByClientContainingIgnoreCase(String client);
}
