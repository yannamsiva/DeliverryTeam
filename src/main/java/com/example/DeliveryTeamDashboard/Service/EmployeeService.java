	package com.example.DeliveryTeamDashboard.Service;
	
	import java.io.IOException;
	import java.time.LocalDate;
	import java.time.LocalTime;
	import java.util.ArrayList;
	import java.util.List;
	import java.util.Objects;
	
	import org.springframework.stereotype.Service;
	import org.springframework.web.multipart.MultipartFile;
	
	import com.example.DeliveryTeamDashboard.Entity.ClientInterview;
	import com.example.DeliveryTeamDashboard.Entity.Employee;
	import com.example.DeliveryTeamDashboard.Entity.InterviewQuestion;
	import com.example.DeliveryTeamDashboard.Entity.JobDescription;
	import com.example.DeliveryTeamDashboard.Entity.MockInterview;
	import com.example.DeliveryTeamDashboard.Entity.Resume;
	import com.example.DeliveryTeamDashboard.Entity.User;
	import com.example.DeliveryTeamDashboard.Repository.ClientInterviewRepository;
	import com.example.DeliveryTeamDashboard.Repository.EmployeeRepository;
	import com.example.DeliveryTeamDashboard.Repository.InterviewQuestionRepository;
	import com.example.DeliveryTeamDashboard.Repository.JobDescriptionRepository;
	import com.example.DeliveryTeamDashboard.Repository.MockInterviewRepository;
	import com.example.DeliveryTeamDashboard.Repository.ResumeRepository;
	import com.example.DeliveryTeamDashboard.Repository.UserRepository;
	
	@Service
	public class EmployeeService {
	
		private final EmployeeRepository employeeRepository;
		private final UserRepository userRepository;
		private final JobDescriptionRepository jobDescriptionRepository;
		private final ResumeRepository resumeRepository;
		private final MockInterviewRepository mockInterviewRepository;
		private final ClientInterviewRepository clientInterviewRepository;
		private final InterviewQuestionRepository interviewQuestionRepository;
		private final S3Service s3Service;
	
		public EmployeeService(EmployeeRepository employeeRepository, JobDescriptionRepository jobDescriptionRepository,
				ResumeRepository resumeRepository, MockInterviewRepository mockInterviewRepository,
				ClientInterviewRepository clientInterviewRepository,
				InterviewQuestionRepository interviewQuestionRepository, UserRepository userRepository,
				S3Service s3Service) {
			this.employeeRepository = employeeRepository;
			this.jobDescriptionRepository = jobDescriptionRepository;
			this.resumeRepository = resumeRepository;
			this.mockInterviewRepository = mockInterviewRepository;
			this.clientInterviewRepository = clientInterviewRepository;
			this.interviewQuestionRepository = interviewQuestionRepository;
			this.userRepository = userRepository;
			this.s3Service = s3Service;
		}
	
		public Object scheduleInterview(String empId, String interviewType, LocalDate date, LocalTime time, String client,
				Long interviewerId, Integer level, String jobDescriptionTitle, String meetingLink, Boolean deployedStatus) {
			if ("mock".equalsIgnoreCase(interviewType)) {
				if (interviewerId == null) {
					throw new IllegalArgumentException("Interviewer ID is required for mock interviews");
				}
				return scheduleMockInterview(empId, date, time, interviewerId);
			} else if ("client".equalsIgnoreCase(interviewType)) {
				if (client == null || client.trim().isEmpty()) {
					throw new IllegalArgumentException("Client is required for client interviews");
				}
				if (level == null) {
					throw new IllegalArgumentException("Level is required for client interviews");
				}
				if (jobDescriptionTitle == null || jobDescriptionTitle.trim().isEmpty()) {
					throw new IllegalArgumentException("Job description title is required for client interviews");
				}
				if (meetingLink == null || meetingLink.trim().isEmpty()) {
					throw new IllegalArgumentException("Meeting link is required for client interviews");
				}
				return scheduleClientInterview(empId, date, time, client, level, jobDescriptionTitle, meetingLink, deployedStatus);
			} else {
				throw new IllegalArgumentException("Invalid interview type: " + interviewType);
			}
		}
	
		public MockInterview scheduleMockInterview(String empId, LocalDate date, LocalTime time, Long interviewerId) {
			if (empId == null || empId.trim().isEmpty()) {
				throw new IllegalArgumentException("Employee ID cannot be null or empty");
			}
			if (date == null) {
				throw new IllegalArgumentException("Date cannot be null");
			}
			if (time == null) {
				throw new IllegalArgumentException("Time cannot be null");
			}
			if (interviewerId == null) {
				throw new IllegalArgumentException("Interviewer ID cannot be null");
			}
	
			Employee employee = employeeRepository.findByEmpId(empId)
					.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + empId));
	
			User interviewer = userRepository.findById(interviewerId)
					.orElseThrow(() -> new IllegalArgumentException("Interviewer not found with ID: " + interviewerId));
			
			 if (Boolean.TRUE.equals(employee.getSentToSales())) {
		            employee.setSentToSales(false);
		            employeeRepository.save(employee);
		        }
	
			MockInterview interview = new MockInterview();
			interview.setEmployee(employee);
			interview.setDate(date);
			interview.setTime(time);
			interview.setInterviewer(interviewer);
			interview.setStatus("scheduled");
			return mockInterviewRepository.save(interview);
		}
	
		public ClientInterview scheduleClientInterview(String empId, LocalDate date, LocalTime time, String client,
				Integer level, String jobDescriptionTitle, String meetingLink, Boolean deployedStatus) {
			if (empId == null || empId.trim().isEmpty()) {
				throw new IllegalArgumentException("Employee ID cannot be null or empty");
			}
			if (date == null) {
				throw new IllegalArgumentException("Date cannot be null");
			}
			if (time == null) {
				throw new IllegalArgumentException("Time cannot be null");
			}
			if (client == null || client.trim().isEmpty()) {
				throw new IllegalArgumentException("Client name cannot be null or empty");
			}
			if (level == null) {
				throw new IllegalArgumentException("Level cannot be null");
			}
			if (jobDescriptionTitle == null || jobDescriptionTitle.trim().isEmpty()) {
				throw new IllegalArgumentException("Job description title cannot be null or empty");
			}
			if (meetingLink == null || meetingLink.trim().isEmpty()) {
				throw new IllegalArgumentException("Meeting link cannot be null or empty");
			}
	
			Employee employee = employeeRepository.findByEmpId(empId)
					.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + empId));
			
			if (!Boolean.TRUE.equals(employee.getSentToSales())) {
	            throw new IllegalArgumentException("Employee with ID: " + empId + " is not eligible for client interviews. Must be sent to sales first by delivery team.");
	        }
			
			ClientInterview interview = new ClientInterview();
			interview.setEmployee(employee);
			interview.setDate(date);
			interview.setTime(time);
			interview.setClient(client.trim());
			interview.setLevel(level);
			interview.setJobDescriptionTitle(jobDescriptionTitle.trim());
			interview.setMeetingLink(meetingLink.trim());
			interview.setStatus("scheduled");
			interview.setDeployedStatus(deployedStatus);
			return clientInterviewRepository.save(interview);
		}
	
		public Employee updateEmployeeDetails(Long employeeId, String technology, String empId) {
			if (employeeId == null) {
				throw new IllegalArgumentException("Employee ID cannot be null");
			}
	
			Employee employee = employeeRepository.findById(employeeId)
					.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
	
			if (technology != null && !technology.trim().isEmpty()) {
				employee.setTechnology(technology.trim());
			}
	
			if (empId != null && !empId.trim().isEmpty()) {
				empId = empId.trim();
				if (employeeRepository.existsByEmpId(empId) && !empId.equals(employee.getEmpId())) {
					throw new IllegalArgumentException("Employee ID '" + empId + "' is already in use");
				}
				employee.setEmpId(empId);
			}
	
			return employeeRepository.save(employee);
		}
	
		public Employee getEmployeeById(Long employeeId) {
			if (employeeId == null) {
				throw new IllegalArgumentException("Employee ID cannot be null");
			}
			return employeeRepository.findById(employeeId)
					.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
		}
	
		public List<JobDescription> getJobDescriptions(String search, String technology, String resourceType) {
			if (search != null && !search.trim().isEmpty()) {
				return jobDescriptionRepository.findByTitleContainingIgnoreCaseOrClientContainingIgnoreCase(search.trim(),
						search.trim());
			}
			if (technology != null && !"all".equalsIgnoreCase(technology.trim())) {
				return jobDescriptionRepository.findByTechnology(technology.trim());
			}
			if (resourceType != null && !"all".equalsIgnoreCase(resourceType.trim())) {
				return jobDescriptionRepository.findByResourceType(resourceType.trim());
			}
			return jobDescriptionRepository.findAll();
		}
	
		public Resume uploadResume(Long employeeId, Long jdId, MultipartFile file) throws IOException {
			if (employeeId == null) {
				throw new IllegalArgumentException("Employee ID cannot be null");
			}
			if (jdId == null) {
				throw new IllegalArgumentException("Job Description ID cannot be null");
			}
			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("Resume file cannot be null or empty");
			}
	
			Employee employee = employeeRepository.findById(employeeId)
					.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
			JobDescription jd = jobDescriptionRepository.findById(jdId)
					.orElseThrow(() -> new IllegalArgumentException("Job Description not found with ID: " + jdId));
	
			String s3Key = s3Service.uploadFile(file, "resumes");
			Resume resume = new Resume();
			resume.setEmployee(employee);
			resume.setJobDescription(jd);
			resume.setS3Key(s3Key);
			resume.setStatus("pending");
			return resumeRepository.save(resume);
		}
	
		
		
		public byte[] downloadResume(Long resumeId) throws IOException {
			if (resumeId == null) {
				throw new IllegalArgumentException("Resume ID cannot be null");
			}
			Resume resume = resumeRepository.findById(resumeId)
					.orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + resumeId));
			return s3Service.downloadFile(resume.getS3Key());
		}
		
		 public byte[] downloadResumeByEmployeeId(Long employeeId) throws IOException {
		        if (employeeId == null) {
		            throw new IllegalArgumentException("Employee ID cannot be null");
		        }
		        List<Resume> resumes = resumeRepository.findByEmployeeId(employeeId);
		        if (resumes.isEmpty()) {
		            throw new IllegalArgumentException("No resume found for employee ID: " + employeeId);
		        }
		        // Select the latest resume based on ID
		        Resume latestResume = resumes.stream()
		                .max((r1, r2) -> Long.compare(r1.getId(), r2.getId()))
		                .orElseThrow(() -> new IllegalArgumentException("No resume found for employee ID: " + employeeId));
		        if (latestResume.getS3Key() == null) {
		            throw new IllegalArgumentException("No resume file associated with employee ID: " + employeeId);
		        }
		        return s3Service.downloadFile(latestResume.getS3Key());
		    }
	
		public void deleteResume(Long resumeId) {
			if (resumeId == null) {
				throw new IllegalArgumentException("Resume ID cannot be null");
			}
			Resume resume = resumeRepository.findById(resumeId)
					.orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + resumeId));
			s3Service.deleteFile(resume.getS3Key());
			resumeRepository.delete(resume);
		}
	
		public List<MockInterview> getMockInterviews(Long employeeId, String technology, String resourceType) {
			List<MockInterview> interviews = (employeeId != null) ? mockInterviewRepository.findByEmployeeId(employeeId)
					: mockInterviewRepository.findAll();
	
			return interviews.stream()
					.filter(i -> technology == null || "all".equalsIgnoreCase(technology.trim())
							|| Objects.equals(i.getEmployee().getTechnology().toLowerCase(),
									technology.trim().toLowerCase()))
					.filter(i -> resourceType == null || "all".equalsIgnoreCase(resourceType.trim()) || Objects
							.equals(i.getEmployee().getResourceType().toLowerCase(), resourceType.trim().toLowerCase()))
					.toList();
		}
	
		public List<ClientInterview> getClientInterviews(Long employeeId, String technology, String resourceType) {
			List<ClientInterview> interviews = (employeeId != null) ? clientInterviewRepository.findByEmployeeId(employeeId)
					: clientInterviewRepository.findAll();
	
			return interviews.stream()
					.filter(i -> technology == null || "all".equalsIgnoreCase(technology.trim())
							|| Objects.equals(i.getEmployee().getTechnology().toLowerCase(),
									technology.trim().toLowerCase()))
					.filter(i -> resourceType == null || "all".equalsIgnoreCase(resourceType.trim()) || Objects
							.equals(i.getEmployee().getResourceType().toLowerCase(), resourceType.trim().toLowerCase()))
					.toList();
		}
	
		public InterviewQuestion addInterviewQuestion(String technology, String question, String user) {
			if (technology == null || technology.trim().isEmpty()) {
				throw new IllegalArgumentException("Technology cannot be null or empty");
			}
			if (question == null || question.trim().isEmpty()) {
				throw new IllegalArgumentException("Question cannot be null or empty");
			}
			if (user == null || user.trim().isEmpty()) {
				throw new IllegalArgumentException("User cannot be null or empty");
			}
	
			InterviewQuestion interviewQuestion = new InterviewQuestion();
			interviewQuestion.setTechnology(technology.trim());
			interviewQuestion.setQuestion(question.trim());
			interviewQuestion.setUser(user.trim());
			interviewQuestion.setDate(LocalDate.now());
			return interviewQuestionRepository.save(interviewQuestion);
		}
	
		public List<InterviewQuestion> getInterviewQuestions(String technology) {
			if (technology != null && !"all".equalsIgnoreCase(technology.trim())) {
				return interviewQuestionRepository.findByTechnology(technology.trim());
			}
			return interviewQuestionRepository.findAll();
		}
		
		


		public Employee updateProfilePicture(Long employeeId, MultipartFile file) throws IOException {
	        if (employeeId == null) {
	            throw new IllegalArgumentException("Employee ID cannot be null");
	        }
	        if (file == null || file.isEmpty()) {
	            throw new IllegalArgumentException("Profile picture file cannot be null or empty");
	        }
	        String contentType = file.getContentType();
	        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
	            throw new IllegalArgumentException("Profile picture must be a JPEG (.jpg, .jpeg) or PNG (.png) file");
	        }

	        Employee employee = employeeRepository.findById(employeeId)
	                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

	        // Delete existing profile picture from S3 if it exists
	        if (employee.getProfilePicS3Key() != null) {
	            s3Service.deleteFile(employee.getProfilePicS3Key());
	        }

	        String s3Key = s3Service.uploadFile(file, "profile-pictures");
	        employee.setProfilePicS3Key(s3Key);
	        return employeeRepository.save(employee);
	    }

	    public byte[] getProfilePicture(Long employeeId) throws IOException {
	        if (employeeId == null) {
	            throw new IllegalArgumentException("Employee ID cannot be null");
	        }
	        Employee employee = employeeRepository.findById(employeeId)
	                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
	        if (employee.getProfilePicS3Key() == null) {
	            throw new IllegalArgumentException("No profile picture found for employee ID: " + employeeId);
	        }
	        return s3Service.downloadFile(employee.getProfilePicS3Key());
	    }

	  
		public List<Employee> getEmployeesReadyForDeployment(String technology, String resourceType) {
			List<Employee> employees = employeeRepository.findByReadyForDeploymentTrue();
			
			return employees.stream()
					.filter(e -> technology == null || "all".equalsIgnoreCase(technology.trim()) 
							|| Objects.equals(e.getTechnology().toLowerCase(), technology.trim().toLowerCase()))
					.filter(e -> resourceType == null || "all".equalsIgnoreCase(resourceType.trim()) 
							|| Objects.equals(e.getResourceType().toLowerCase(), resourceType.trim().toLowerCase()))
					.toList();
		}

		public List<Employee> getDeployedEmployees() {
			try {
				List<Employee> employees = employeeRepository.findByDeployedTrue();
				if (employees == null) {
					return new ArrayList<>();
				}
				return employees;
			} catch (Exception e) {
				throw new RuntimeException("Error fetching deployed employees: " + e.getMessage());
			}
		}

	}