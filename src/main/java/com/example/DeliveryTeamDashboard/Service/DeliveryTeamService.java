package com.example.DeliveryTeamDashboard.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.MockInterview;
import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.EmployeeRepository;
import com.example.DeliveryTeamDashboard.Repository.MockInterviewRepository;
import com.example.DeliveryTeamDashboard.Repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class DeliveryTeamService {

	private static final Logger logger = LoggerFactory.getLogger(DeliveryTeamService.class);
	
	private final EmployeeRepository employeeRepository;
	private final MockInterviewRepository mockInterviewRepository;
	private final EmployeeService employeeService;
	private final UserRepository userRepository;
	private final EmailService emailService;

	@Autowired
	private S3Service s3Service;

	public DeliveryTeamService(EmployeeRepository employeeRepository, MockInterviewRepository mockInterviewRepository,
			EmployeeService employeeService, UserRepository userRepository, EmailService emailService) {
		this.employeeRepository = employeeRepository;
		this.mockInterviewRepository = mockInterviewRepository;
		this.employeeService = employeeService;
		this.userRepository = userRepository;
		this.emailService = emailService;
	}

	public List<Employee> getEmployees(String technology, String resourceType) {
		List<Employee> employees = employeeRepository.findAll();
		return employees.stream()
				.filter(e -> "all".equalsIgnoreCase(technology) || e.getTechnology().equalsIgnoreCase(technology))
				.filter(e -> "all".equalsIgnoreCase(resourceType) || e.getResourceType().equalsIgnoreCase(resourceType))
				.collect(Collectors.toList());
	}

//    public MockInterview scheduleMockInterview(String empId, LocalDate date, LocalTime time, Long interviewerId) {
//    	MockInterview interview =(MockInterview) employeeService.scheduleInterview(empId, "mock", date, time, null, interviewerId, null, null, null, null);
//    	  try {
//              Employee employee = employeeRepository.findByEmpId(empId)
//                      .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + empId));
//              User interviewer = userRepository.findById(interviewerId)
//                      .orElseThrow(() -> new IllegalArgumentException("Interviewer not found with ID: " + interviewerId));
//              
//              emailService.sendMockInterviewNotification(
//                  employee.getUser(). getEmail(),
//                  interviewer.getEmail(),
//                  employee.getUser().getFullName(),
//                  interviewer.getFullName(),
//                  date,
//                  time
//              );
//          } catch (MessagingException e) {
//              // Log the error but don't fail the scheduling
//              System.err.println("Failed to send email notifications: " + e.getMessage());
//          }
//          
//          return interview;
//    }

	public MockInterview scheduleMockInterview(String empId, LocalDate date, LocalTime time, Long interviewerId,
			MultipartFile[] files) {
		MockInterview interview = (MockInterview) employeeService.scheduleInterview(empId, "mock", date, time, null,
				interviewerId, null, null, null, null);
		try {
			Employee employee = employeeRepository.findByEmpId(empId)
					.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + empId));
			User interviewer = userRepository.findById(interviewerId)
					.orElseThrow(() -> new IllegalArgumentException("Interviewer not found with ID: " + interviewerId));

			// Upload files to S3 and store their keys
			List<String> s3Keys = files != null && files.length > 0
					? s3Service.uploadMultipleFiles(files, "mock-interview-files")
					: List.of();
			interview.setFileS3Keys(s3Keys);
			mockInterviewRepository.save(interview);

			// Generate presigned URLs for email
			List<String> presignedUrls = s3Keys.stream().map(s3Service::generatePresignedUrl)
					.collect(Collectors.toList());

			emailService.sendMockInterviewNotification(employee.getUser().getEmail(), interviewer.getEmail(),
					employee.getUser().getFullName(), interviewer.getFullName(), date, time, presignedUrls);
		} catch (MessagingException | IOException e) {
			System.err.println("Failed to send email notifications or upload files: " + e.getMessage());
		}

		return interview;
	}

//    public MockInterview updateMockInterviewFeedback(Long interviewId, String technicalFeedback, String communicationFeedback,
//            Integer technicalScore, Integer communicationScore, Boolean sentToSales) {
//    	MockInterview interview = mockInterviewRepository.findById(interviewId)
//    			.orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));
//    	interview.setTechnicalFeedback(technicalFeedback);
//    	interview.setCommunicationFeedback(communicationFeedback);
//    	interview.setTechnicalRating(technicalScore);
//    	interview.setCommunicationRating(communicationScore);
//    	interview.setStatus("completed");
//
//    	if (sentToSales != null && sentToSales) {
//    		interview.setSentToSales(true);
//    		Employee employee = interview.getEmployee();
//    		if (employee != null) {
//    			employee.setSentToSales(true); // Update employee's sentToSales status
//    			employeeRepository.save(employee);
//    		}
//    	}
//
//    	MockInterview savedInterview = mockInterviewRepository.save(interview);
//
//        // Send email notification to employee
//
//        try {
//            Employee employee = interview.getEmployee();
//            if (employee != null && employee.getUser() != null) {
//                String employeeEmail = employee.getUser().getEmail();
//                String employeeName = employee.getUser().getFullName();
//
//                emailService.sendMockInterviewFeedbackNotification(
//                    employeeEmail,
//                    employeeName,
//                    interview.getDate(),
//                    interview.getTime(),
//                    technicalFeedback,
//                    communicationFeedback,
//                    technicalScore,
//                    communicationScore
//                );
//            }
//        } catch (MessagingException e) {
//            System.err.println("Failed to send feedback email notification: " + e.getMessage());
//        }
//
//        return savedInterview;
//        }

	public MockInterview updateMockInterviewFeedback(Long interviewId, String technicalFeedback, String communicationFeedback,
            Integer technicalScore, Integer communicationScore, Boolean sentToSales, MultipartFile file) {
        logger.info("Updating feedback for interview ID: {}", interviewId);
        MockInterview interview = mockInterviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));
        interview.setTechnicalFeedback(technicalFeedback);
        interview.setCommunicationFeedback(communicationFeedback);
        interview.setTechnicalRating(technicalScore);
        interview.setCommunicationRating(communicationScore);
        interview.setStatus("completed");

        if (sentToSales != null && sentToSales) {
            interview.setSentToSales(true);
            Employee employee = interview.getEmployee();
            if (employee != null) {
                employee.setSentToSales(true);
                employeeRepository.save(employee);
            }
        }

        // Handle file upload if provided
        String presignedUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                logger.info("Processing file upload for feedback: {}", file.getOriginalFilename());
                List<String> s3Keys = interview.getFileS3Keys() != null ? new ArrayList<>(interview.getFileS3Keys()) : new ArrayList<>();
                String s3Key = s3Service.uploadFile(file, "mock-interview-feedback");
                s3Keys.add(s3Key);
                interview.setFileS3Keys(s3Keys);
                presignedUrl = s3Service.generatePresignedUrl(s3Key);
                logger.info("File uploaded to S3 with key: {}, presigned URL: {}", s3Key, presignedUrl);
            } catch (IOException e) {
                logger.error("Failed to upload feedback file: {}", e.getMessage(), e);
            }
        } else {
            logger.info("No file provided for feedback update for interview ID: {}", interviewId);
        }

        MockInterview savedInterview = mockInterviewRepository.save(interview);

        try {
            Employee employee = interview.getEmployee();
            if (employee != null && employee.getUser() != null) {
                String employeeEmail = employee.getUser().getEmail();
                String employeeName = employee.getUser().getFullName();

                logger.info("Sending feedback email to: {}", employeeEmail);
                emailService.sendMockInterviewFeedbackNotification(
                    employeeEmail,
                    employeeName,
                    interview.getDate(),
                    interview.getTime(),
                    technicalFeedback,
                    communicationFeedback,
                    technicalScore,
                    communicationScore,
                    presignedUrl
                );
            } else {
                logger.warn("No employee or user found for interview ID: {}", interviewId);
            }
        } catch (MessagingException e) {
            logger.error("Failed to send feedback email notification: {}", e.getMessage(), e);
        }

        return savedInterview;
    }
	public List<MockInterview> getUpcomingInterviews() {
		return mockInterviewRepository.findByStatus("scheduled");
	}

	public List<MockInterview> getCompletedInterviews() {
		return mockInterviewRepository.findByStatus("completed");
	}

	public MockInterview updateInterviewStatus(Long interviewId) {
		MockInterview interview = mockInterviewRepository.findById(interviewId)
				.orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));

		if (!"scheduled".equalsIgnoreCase(interview.getStatus())) {
			throw new IllegalArgumentException("Can only update status of scheduled interviews");
		}

		interview.setStatus("completed");
		return mockInterviewRepository.save(interview);
	}

	public User updateProfilePicture(Long employeeId, MultipartFile file) throws IOException {
		if (employeeId == null) {
			throw new IllegalArgumentException("User ID cannot be null");
		}
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Profile picture file cannot be null or empty");
		}
		String contentType = file.getContentType();
		if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
			throw new IllegalArgumentException("Profile picture must be a JPEG (.jpg, .jpeg) or PNG (.png) file");
		}

		User user = userRepository.findById(employeeId)
				.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + employeeId));

		// Delete existing profile picture from S3 if it exists
		if (user.getProfilePicS3Key() != null) {
			s3Service.deleteFile(user.getProfilePicS3Key());
		}

		String s3Key = s3Service.uploadFile(file, "profile-pictures");
		user.setProfilePicS3Key(s3Key);
		return userRepository.save(user);
	}

	public byte[] getProfilePicture(Long employeeId) throws IOException {
		if (employeeId == null) {
			throw new IllegalArgumentException("Employee ID cannot be null");
		}
		User user = userRepository.findById(employeeId)
				.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + employeeId));
		if (user.getProfilePicS3Key() == null) {
			throw new IllegalArgumentException("No profile picture found for employee ID: " + employeeId);
		}
		return s3Service.downloadFile(user.getProfilePicS3Key());
	}

	public User getUserByEmail(String email) {
		if (email == null || email.isEmpty()) {
			throw new IllegalArgumentException("Email cannot be null or empty");
		}
		return userRepository.findByEmail(email).orElse(null);
	}
	public List<Map<String, Object>> getMockInterviewPerformance() {
		List<MockInterview> completedInterviews = mockInterviewRepository.findByStatus("completed");

		return completedInterviews.stream().map(interview -> {
			Map<String, Object> performance = new HashMap<>();
			Employee employee = interview.getEmployee();
			User user = employee != null ? employee.getUser() : null;

			performance.put("employeeId", employee != null ? employee.getEmpId() : null);
			performance.put("employeeName", user != null ? user.getFullName() : "Unknown");
			performance.put("technology", employee != null ? employee.getTechnology() : null);
			performance.put("resourceType", employee != null ? employee.getResourceType() : null);

			// Calculate total rating, handle null ratings
			int totalRating = 0;
			if (interview.getTechnicalRating() != null && interview.getCommunicationRating() != null) {
				totalRating = interview.getTechnicalRating() + interview.getCommunicationRating();
			}
			performance.put("totalRating", totalRating);

			return performance;
		}).sorted((a, b) -> Integer.compare((Integer) b.get("totalRating"), (Integer) a.get("totalRating")))
				.collect(Collectors.toList());
	}
}