package com.example.DeliveryTeamDashboard.Entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "mock_interviews")
@Data
public class MockInterview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer; // Changed from String to User

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(length = 50)
    private String status;

    @Column
    private Integer technicalRating;

    @Column
    private Integer communicationRating;

    @Column(length = 500)
    private String technicalFeedback;

    @Column(length = 500)
    private String communicationFeedback;

    @Column
    private Boolean sentToSales;
    
    private boolean deployed;
    

    @ElementCollection
    @CollectionTable(name = "mock_interview_files", joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "s3_key")
    private List<String> fileS3Keys; 
}