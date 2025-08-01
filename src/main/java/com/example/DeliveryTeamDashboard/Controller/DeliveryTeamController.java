package com.example.DeliveryTeamDashboard.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.MockInterview;
import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.UserRepository;
import com.example.DeliveryTeamDashboard.Service.DeliveryTeamService;
import com.example.DeliveryTeamDashboard.Service.EmployeeService;
import com.example.DeliveryTeamDashboard.Service.S3Service;


@RestController
@RequestMapping("/api/delivery")
public class DeliveryTeamController {
     private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private UserRepository userRepository;

    private final DeliveryTeamService deliveryTeamService;
    // private final EmployeeService employeeService; // Removed unused field

    private EmployeeService employeeService;

    public DeliveryTeamController(DeliveryTeamService deliveryTeamService, EmployeeService employeeService) {
        this.deliveryTeamService = deliveryTeamService;
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public List<Employee> getEmployees(
            @RequestParam(defaultValue = "all") String technology,
            @RequestParam(defaultValue = "all") String resourceType) {
        return deliveryTeamService.getEmployees(technology, resourceType);
    }


    @PostMapping("/schedule")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    public ResponseEntity<?> scheduleInterview(
            Authentication authentication,
            @RequestParam String empId,
            @RequestParam String interviewType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm:ss") LocalTime time,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) Long interviewerId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String jobDescriptionTitle,
            @RequestParam(required = false) String meetingLink,
            @RequestParam(required = false) MultipartFile[] files) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        if (!"mock".equalsIgnoreCase(interviewType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Delivery team can only schedule mock interviews");
        }
        try {
            MockInterview interview = deliveryTeamService.scheduleMockInterview(empId, date, time, interviewerId, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(interview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
//    @PutMapping("/mock-interviews/{interviewId}/feedback")
//    @PreAuthorize("hasRole('DELIVERY_TEAM')")
//    public ResponseEntity<?> updateMockInterviewFeedback(
//            Authentication authentication,
//            @PathVariable Long interviewId,
//            @RequestParam String technicalFeedback,
//            @RequestParam String communicationFeedback,
//            @RequestParam Integer technicalScore,
//            @RequestParam Integer communicationScore,
//            @RequestParam Boolean sentToSales
//            ) {
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
//        }
//        try {
//            MockInterview interview = deliveryTeamService.updateMockInterviewFeedback(interviewId, technicalFeedback,communicationFeedback, technicalScore, communicationScore,sentToSales);
//            return ResponseEntity.ok(interview);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }

    
    @PutMapping("/mock-interviews/{interviewId}/feedback")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    public ResponseEntity<?> updateMockInterviewFeedback(
            Authentication authentication,
            @PathVariable Long interviewId,
            @RequestParam String technicalFeedback,
            @RequestParam String communicationFeedback,
            @RequestParam Integer technicalScore,
            @RequestParam Integer communicationScore,
            @RequestParam Boolean sentToSales,
            @RequestParam(required = false) MultipartFile file) {
        logger.info("Received feedback update request for interview ID: {}. File provided: {}", 
            interviewId, file != null ? file.getOriginalFilename() : "none");
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        try {
            MockInterview interview = deliveryTeamService.updateMockInterviewFeedback(
                interviewId, technicalFeedback, communicationFeedback, technicalScore, 
                communicationScore, sentToSales, file);
            return ResponseEntity.ok(interview);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating feedback for interview ID: {}: {}", interviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/interviews/upcoming")
    public List<MockInterview> getUpcomingInterviews() {
        return deliveryTeamService.getUpcomingInterviews();
    }

    @GetMapping("/interviews/completed")
    public List<MockInterview> getCompletedInterviews() {
        return deliveryTeamService.getCompletedInterviews();
    }

    @PutMapping("/mock-interviews/{interviewId}/update-status")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    public ResponseEntity<?> updateInterviewStatus(
            Authentication authentication,
            @PathVariable Long interviewId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        try {
            MockInterview interview = deliveryTeamService.updateInterviewStatus(interviewId);
            return ResponseEntity.ok(interview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    
    @PutMapping("/profile-picture")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    public ResponseEntity<?> updateProfilePicture(
            Authentication authentication,
            @RequestParam Long Id,
            @RequestParam MultipartFile file) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".png"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile picture must be a JPEG (.jpg, .jpeg) or PNG (.png) file");
        }
        try {
            User user = deliveryTeamService.updateProfilePicture(Id, file);
            return ResponseEntity.status(HttpStatus.OK).body(user);
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/profile-picture/{employeeId}")
    public ResponseEntity<?> getProfilePicture(@PathVariable Long employeeId) throws IOException {
        try {
            byte[] fileData = deliveryTeamService.getProfilePicture(employeeId);
            ByteArrayResource resource = new ByteArrayResource(fileData);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=profile-picture.jpg")
                    .contentType(MediaType.IMAGE_JPEG)
                    .contentLength(fileData.length)
                    .body(resource);
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        }
    
    @GetMapping("/mock-interviews/performance")
    public ResponseEntity<List<Map<String, Object>>> getMockInterviewPerformance() {
        try {
            List<Map<String, Object>> performanceList = deliveryTeamService.getMockInterviewPerformance();
            return ResponseEntity.ok(performanceList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        String email = authentication.getName();
        try {
            User user = deliveryTeamService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    public User getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return userRepository.findByEmail(email).orElse(null);
    }
}