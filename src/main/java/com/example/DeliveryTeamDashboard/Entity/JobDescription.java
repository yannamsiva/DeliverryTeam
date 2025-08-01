package com.example.DeliveryTeamDashboard.Entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "job_descriptions")
@Data
public class JobDescription {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Column(length = 100)
    private String client;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column
    private LocalDate deadline;

    @Column(length = 50)
    private String technology;

    @Column(name = "resource_type", length = 10)
    private String resourceType;

    @Column(length = 1000)
    private String description;

    @Column(name = "s3_key")
    private String s3Key;// S3 key for the JD file
}
