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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.DeliveryTeamDashboard.Entity.Client;
import com.example.DeliveryTeamDashboard.Entity.ClientInterview;
import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.JobDescription;
import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.ClientInterviewRepository;
import com.example.DeliveryTeamDashboard.Service.EmailService;
import com.example.DeliveryTeamDashboard.Service.SalesTeamService;
import com.example.DeliveryTeamDashboard.Service.SalesTeamService.ClientInterviewSchedule;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/api/sales")

public class SalesTeamController {
	private static final Logger logger = LoggerFactory.getLogger(SalesTeamController.class);

		
	@Autowired
	private ClientInterviewRepository clientInterviewRepository;
	
	private final SalesTeamService salesTeamService;
	private final EmailService emailService;

	public SalesTeamController(SalesTeamService salesTeamService, EmailService emailService) {
		this.salesTeamService = salesTeamService;
		this.emailService = emailService;
	}

	@GetMapping("/candidates")
	public List<Employee> getCandidates(
			Authentication authentication,
			@RequestParam(defaultValue = "all") String technology,
			@RequestParam(defaultValue = "all") String status,
			@RequestParam(defaultValue = "all") String resourceType) {
		if (authentication != null) {
			logger.info("User: {} | Authorities: {}", authentication.getName(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
		} else {
			logger.info("No authentication present");
		}
		return salesTeamService.getCandidates(technology, status, resourceType);
	}

	@PostMapping("/interviews/schedule")
	@PreAuthorize("hasRole('SALES')")
	public ResponseEntity<?> scheduleInterview(
			Authentication authentication,
			@RequestParam String empId,
			@RequestParam String interviewType,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam @DateTimeFormat(pattern = "HH:mm:ss") LocalTime time,
			@RequestParam(required = false) String client,
			@RequestParam(required = false) String interviewer,
			@RequestParam(required = false) Integer level,
			@RequestParam(required = false) String jobDescriptionTitle,
			@RequestParam(required = false) String meetingLink,
			@RequestParam(required = false, defaultValue = "false") Boolean deployedStatus,
			@RequestParam(required = false) String interviewerEmail,
			@RequestParam(required = false) MultipartFile file) {
		if (authentication != null) {
			logger.info("User: {} | Authorities: {}", authentication.getName(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
		} else {
			logger.info("No authentication present");
		}
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		if (!"client".equalsIgnoreCase(interviewType)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sales team can only schedule client interviews");
		}
		if (client == null || level == null || jobDescriptionTitle == null || meetingLink == null || interviewerEmail == null || file == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Client, level, job description title, meeting link, interviewer email, and file are required for client interviews");
		}
		try {
			ClientInterview interview = salesTeamService.scheduleClientInterview(empId, client, date, time, level, jobDescriptionTitle, meetingLink, deployedStatus);
			// Send file to interviewer email
			String subject = "Client Interview Documents";
			String body = String.format("Dear Interviewer,\n\nPlease find the attached file for the upcoming client interview with employee ID: %s.\n\nBest regards,\nSales Team", empId);
			emailService.sendEmailWithAttachment(interviewerEmail, subject, body, file);
			return ResponseEntity.status(HttpStatus.CREATED).body(interview);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to send email with attachment: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email with attachment: " + e.getMessage());
		}
	}

	@PutMapping("/client-interviews/{interviewId}")
	@PreAuthorize("hasRole('SALES')")
	public ResponseEntity<?> updateClientInterview(
			Authentication authentication,
			@PathVariable Long interviewId,
			@RequestParam String result,
			@RequestParam String feedback,
			@RequestParam Integer technicalScore,
			@RequestParam Integer communicationScore,
			@RequestParam(required = false) Boolean deployedStatus,
			@RequestParam(required = false) MultipartFile file) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		try {
			if (result == null || feedback == null || technicalScore == null || communicationScore == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All required fields (result, feedback, technicalScore, communicationScore) are missing.");
			}
			ClientInterview interview = salesTeamService.updateClientInterview(interviewId, result, feedback, technicalScore, communicationScore, deployedStatus, file);
			// Create response with updated interview details
			Map<String, Object> response = new java.util.HashMap<>();
			response.put("id", interview.getId());
			response.put("result", interview.getResult());
			response.put("feedback", interview.getFeedback());
			response.put("technicalScore", interview.getTechnicalScore());
			response.put("communicationScore", interview.getCommunicationScore());
			response.put("deployedStatus", interview.getDeployedStatus());
			response.put("level", interview.getLevel());
			response.put("status", interview.getStatus());
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

		
		@GetMapping("/client-interviews")
		public List<ClientInterview> getClientInterviews(
				@RequestParam(required = false) String search) {
			return salesTeamService.getClientInterviews(search);
		}

	@PostMapping("/clients")
	@PreAuthorize("hasRole('SALES')")
		public ResponseEntity<?> addClient(
				Authentication authentication,
				@RequestParam String name,
				@RequestParam String contactEmail,
				@RequestParam Integer activePositions,
				@RequestParam List<String> technologies) {
			if (authentication == null || !authentication.isAuthenticated()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
			}
			try {
				Client client = salesTeamService.addClient(name, contactEmail, activePositions, technologies);
				return ResponseEntity.status(HttpStatus.CREATED).body(client);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			}
		}

		@GetMapping("/clients")
		public List<Client> getClients(
				@RequestParam(required = false) String search) {
			return salesTeamService.getClients(search);
		}

	@PostMapping("/job-descriptions")
	@PreAuthorize("hasRole('SALES')")
		public ResponseEntity<?> addJobDescription(
				Authentication authentication,
				@RequestParam String title,
				@RequestParam String client,
				@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedDate,
				@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
				@RequestParam String technology,
				@RequestParam String resourceType,
				@RequestParam String description,
				@RequestParam(required = false) MultipartFile file) throws IOException {
			if (authentication == null || !authentication.isAuthenticated()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
			}
			try {
				JobDescription jobDescription = salesTeamService.uploadJobDescription(title, client, receivedDate, deadline, technology, resourceType, description, file);
				return ResponseEntity.status(HttpStatus.CREATED).body(jobDescription);
			} catch (IllegalArgumentException | IOException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			}
		}

	@GetMapping("/job-descriptions")
	@PreAuthorize("hasRole('SALES')")
		public List<JobDescription> getAllJobDescriptions() {
			return salesTeamService.getAllJobDescriptions();
		}

		@GetMapping("/job-descriptions/{jdId}/download")
		public ResponseEntity<?> downloadJobDescription(@PathVariable Long jdId) {
			try {
				byte[] fileData = salesTeamService.downloadJobDescription(jdId);
				ByteArrayResource resource = new ByteArrayResource(fileData);
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=job_description.pdf")
						.contentType(MediaType.APPLICATION_PDF)
						.contentLength(fileData.length)
						.body(resource);
			} catch (IllegalArgumentException | IOException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			}
		}

		@DeleteMapping("/job-descriptions/{jdId}")
		public ResponseEntity<?> deleteJobDescription(@PathVariable Long jdId) {
			try {
				salesTeamService.deleteJobDescription(jdId);
				return ResponseEntity.ok().build();
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			}
		}

	@GetMapping("/resumes")
	@PreAuthorize("hasRole('SALES') or hasRole('DELIVERY_TEAM')")
		public ResponseEntity<List<Employee>> getAllEmployeeResumes() {
			List<Employee> employeesWithResumes = salesTeamService.getCandidates("all", "all", "all"); // Assuming getCandidates can fetch employees with resume info
			return ResponseEntity.ok(employeesWithResumes);
		}

	@GetMapping("/resumes/filter")
	@PreAuthorize("hasRole('SALES') or hasRole('DELIVERY_TEAM')")
		public ResponseEntity<List<Employee>> getFilteredResumes(
				@RequestParam(required = false, defaultValue = "all") String technology,
				@RequestParam(required = false, defaultValue = "all") String resourceType) {
			try {
				List<Employee> filteredResumes = salesTeamService.getCandidates(technology, "all", resourceType)
					.stream()
					.filter(employee -> Boolean.TRUE.equals(employee.getReadyForDeployment()))
					.toList();
				return ResponseEntity.ok(filteredResumes);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
			}
		}

//	    
	@GetMapping("/client-interviews/{interviewId}/feedback")
	@PreAuthorize("hasRole('SALES') or hasRole('DELIVERY_TEAM') or hasRole('EMPLOYEE')")
		public ResponseEntity<?> getClientInterviewFeedback(@PathVariable Long interviewId) {
			try {
				ClientInterview interview = salesTeamService.getClientInterviewById(interviewId);
				if (interview == null) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Interview not found with ID: " + interviewId);
				}
				Map<String, Object> feedbackDetails = new java.util.HashMap<>();
				feedbackDetails.put("id", interview.getId());
				feedbackDetails.put("feedback", interview.getFeedback());
				feedbackDetails.put("technicalScore", interview.getTechnicalScore());
				feedbackDetails.put("communicationScore", interview.getCommunicationScore());
				feedbackDetails.put("result", interview.getResult());
				feedbackDetails.put("overallStatus", interview.getStatus());
				feedbackDetails.put("level", interview.getLevel());
				return ResponseEntity.ok(feedbackDetails);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			}
		}


		@GetMapping("/employees/deployed")
		public ResponseEntity<List<Employee>> getDeployedEmployees() {
			List<Employee> deployedEmployees = salesTeamService.getDeployedEmployees();
			return ResponseEntity.ok(deployedEmployees);
		}
		
		@PostMapping("/employees/{empId}/interviews")
		@PreAuthorize("isAuthenticated()")
		public ResponseEntity<?> scheduleMultipleClientInterviews(
				@PathVariable String empId,
				@RequestBody List<ClientInterviewSchedule> schedules) {
			try {
				List<ClientInterview> interviews = salesTeamService.scheduleMultipleClientInterviews(empId, schedules);
				return ResponseEntity.ok(interviews);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: " + e.getMessage());
			} catch (AuthenticationException e) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: Invalid or missing token");
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
			}
		}
		
	@GetMapping("/get-all-scheduleclientinterview-count")
	public int getallscheduleclientinterviewcount(Authentication authentication) {
		if (authentication != null) {
			logger.info("User: {} | Authorities: {}", authentication.getName(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
		} else {
			logger.info("No authentication present");
		}
		List<ClientInterview> in=clientInterviewRepository.findAll();
		return in.size();
	}
		
		@PutMapping("/profile-picture")
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
			User user = salesTeamService.updateProfilePicture(Id, file);
			return ResponseEntity.status(HttpStatus.OK).body(user);
		} catch (IllegalArgumentException | IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

		 @GetMapping("/profile-picture/{employeeId}")
		 public ResponseEntity<?> getProfilePicture(@PathVariable Long employeeId) throws IOException {
			 try {
				 byte[] fileData = salesTeamService.getProfilePicture(employeeId);
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

	@GetMapping("/me")
	@PreAuthorize("hasRole('SALES')")
	public ResponseEntity<?> getCurrentUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		String email = authentication.getName();
		try {
			User user = salesTeamService.getUserByEmail(email);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
			}
			return ResponseEntity.ok(user);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

}