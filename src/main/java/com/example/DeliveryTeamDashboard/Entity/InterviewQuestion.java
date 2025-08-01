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
@Table(name = "interview_questions")
@Data
public class InterviewQuestion {
		@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(length = 50)
	    private String technology;

	    @Column(length = 500)
	    private String question;

	    @Column(length = 100)
	    private String user;

	    @Column
	    private LocalDate date;
}
