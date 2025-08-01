package com.example.DeliveryTeamDashboard.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.DeliveryTeamDashboard.Entity.MockInterview;

@Repository
public interface MockInterviewRepository extends JpaRepository<MockInterview, Long> {
    List<MockInterview> findByEmployeeId(Long employeeId);
    List<MockInterview> findByStatus(String status);//
}