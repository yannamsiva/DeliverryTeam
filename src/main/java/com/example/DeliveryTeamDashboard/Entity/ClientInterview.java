package com.example.DeliveryTeamDashboard.Entity;

import java.time.LocalDate;
import java.time.LocalTime;

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
@Table(name = "client_interviews")
@Data
public class ClientInterview {
	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne
	    @JoinColumn(name = "employee_id", nullable = false)
	    private Employee employee;

	    @Column(length = 100)
	    private String client;

	    @Column(nullable = false)
	    private LocalDate date;

	    @Column(nullable = false)
	    private LocalTime time;

	    @Column
	    private Integer level;

	    @Column(name = "job_description_title", length = 100)
	    private String jobDescriptionTitle;

	    @Column(name = "meeting_link", length = 255)
	    private String meetingLink;

	    @Column(length = 50)
	    private String status;

	    @Column(length = 50)
	    private String result;

	    @Column(length = 500)
	    private String feedback;

	    @Column
	    private Integer technicalScore;

	    @Column
	    private Integer communicationScore;
	    
	    @Column(name = "deployed_status")
	    private Boolean deployedStatus;
}
