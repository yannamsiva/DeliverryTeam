package com.example.DeliveryTeamDashboard.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "resumes")
@Data
public class Resume {
	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne
	    @JoinColumn(name = "employee_id", nullable = false)
	    private Employee employee;

	    @ManyToOne
	    @JoinColumn(name = "job_description_id", nullable = false)
	    private JobDescription jobDescription;

	    @Column(name = "s3_key")
	    private String s3Key; // S3 key for the resume file
	    
	    @Column(length = 50)
	    private String status; // pending, submitted, rejected
}