package com.example.DeliveryTeamDashboard.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;



@Entity
@Table(name = "employee", uniqueConstraints = {
    @UniqueConstraint(columnNames = "emp_id", name = "uk_employee_empid")
})
@Data
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "emp_id", nullable = false, length = 20)
    private String empId;

    @Column(length = 50)
    private String technology;

    @Column(name = "resource_type", length = 10)
    private String resourceType;

//    @Column(length = 50)
//    private String level;

    @Column(length = 20)
    private String status;
    
    @Column(name = "profile_pic_s3_key", length = 255)
    private String profilePicS3Key;

    @Column(name = "ready_for_deployment", nullable = false)
    private Boolean readyForDeployment = false;

    @Column(name = "deployed", nullable = false)
    private Boolean deployed = false;
    
    private Boolean sentToSales = false; // New field
}