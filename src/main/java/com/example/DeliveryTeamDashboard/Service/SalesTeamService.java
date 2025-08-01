package com.example.DeliveryTeamDashboard.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.DeliveryTeamDashboard.Entity.Client;
import com.example.DeliveryTeamDashboard.Entity.ClientInterview;
import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.JobDescription;
import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.ClientInterviewRepository;
import com.example.DeliveryTeamDashboard.Repository.ClientRepository;
import com.example.DeliveryTeamDashboard.Repository.EmployeeRepository;
import com.example.DeliveryTeamDashboard.Repository.JobDescriptionRepository;
import com.example.DeliveryTeamDashboard.Repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class SalesTeamService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    private final EmployeeRepository employeeRepository;
    private final ClientInterviewRepository clientInterviewRepository;
    private final ClientRepository clientRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final S3Service s3Service;
    private final EmployeeService employeeService;

    public SalesTeamService(EmployeeRepository employeeRepository,
                            ClientInterviewRepository clientInterviewRepository,
                            ClientRepository clientRepository,
                            JobDescriptionRepository jobDescriptionRepository,
                            S3Service s3Service,
                            EmployeeService employeeService) {
        this.employeeRepository = employeeRepository;
        this.clientInterviewRepository = clientInterviewRepository;
        this.clientRepository = clientRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.s3Service = s3Service;
        this.employeeService = employeeService;
    }

    public List<Employee> getCandidates(String technology, String status, String resourceType) {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
                .filter(e -> "all".equalsIgnoreCase(technology) || e.getTechnology().equalsIgnoreCase(technology))
                .filter(e -> "all".equalsIgnoreCase(status) || e.getStatus().equalsIgnoreCase(status))
                .filter(e -> "all".equalsIgnoreCase(resourceType) || e.getResourceType().equalsIgnoreCase(resourceType))
                .filter(Employee::getSentToSales)
                .collect(Collectors.toList());
    }
    

    public ClientInterview scheduleClientInterview(String empId, String client, LocalDate date, LocalTime time, Integer level, String jobDescriptionTitle, String meetingLink, Boolean deployedStatus) {
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + empId));
       
        try {
            emailService.sendClientInterviewNotification(employee.getUser().getEmail(),employee.getUser().getFullName(),client,date,time,level,jobDescriptionTitle,meetingLink);
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (!Boolean.TRUE.equals(employee.getSentToSales())) {
            throw new IllegalArgumentException("Employee with ID: " + empId + " is not eligible for client interviews. Must be sent to sales first.");
        }

        return (ClientInterview) employeeService.scheduleInterview(empId, "client", date, time, client, null, level, jobDescriptionTitle, meetingLink, deployedStatus);
        
    }

    public User getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    public ClientInterview updateClientInterview(Long interviewId, String result, String feedback, Integer technicalScore, Integer communicationScore, Boolean deployedStatus) {
        return updateClientInterview(interviewId, result, feedback, technicalScore, communicationScore, deployedStatus, null);
    }

    public ClientInterview updateClientInterview(Long interviewId, String result, String feedback, Integer technicalScore, Integer communicationScore, Boolean deployedStatus, MultipartFile file) {
        ClientInterview interview = clientInterviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));
        interview.setResult(result);
        interview.setFeedback(feedback);
        interview.setTechnicalScore(technicalScore);
        interview.setCommunicationScore(communicationScore);
        interview.setStatus("completed");
        if ("pass".equalsIgnoreCase(result) && (deployedStatus == null || !deployedStatus)) {
            Integer currentLevel = interview.getLevel();
            if (currentLevel != null) {
                interview.setLevel(currentLevel + 1);
            } else {
                interview.setLevel(1); // Default to level 1 if null
            }
        }
        if (deployedStatus != null) {
            interview.setDeployedStatus(deployedStatus);
            if (deployedStatus) {
                Employee employee = interview.getEmployee();
                if (employee != null) {
                    employee.setDeployed(true);
                    employeeRepository.save(employee);
                }
            }
        }
        ClientInterview savedInterview = clientInterviewRepository.save(interview);
        try {
            Employee employee = interview.getEmployee();
            if (employee != null && employee.getUser() != null) {
                String employeeEmail = employee.getUser().getEmail();
                String employeeName = employee.getUser().getFullName();
                String client = interview.getClient();
                LocalDate date = interview.getDate();
                LocalTime time = interview.getTime();
                String subject = "Client Interview Feedback";
                String body = String.format("Dear %s,\n\nYour client interview feedback for %s on %s at %s:\nResult: %s\nFeedback: %s\nTechnical Score: %d\nCommunication Score: %d\nLevel: %d\n\nBest regards,\nSales Team",
                        employeeName, client, date, time, result, feedback, technicalScore, communicationScore, interview.getLevel());
                if (file != null && !file.isEmpty()) {
                    emailService.sendEmailWithAttachment(employeeEmail, subject, body, file);
                } else {
                    emailService.sendClientInterviewFeedbackNotification(
                        employeeEmail,
                        employeeName,
                        client,
                        date,
                        time,
                        result,
                        feedback,
                        technicalScore,
                        communicationScore,
                        interview.getLevel()
                    );
                }
            }
        } catch (MessagingException e) {
            System.err.println("Failed to send client interview feedback email notification: " + e.getMessage());
        }
        return savedInterview;
    }


    public List<ClientInterview> getClientInterviews(String search) {
        if (search != null && !search.isEmpty()) {
            return clientInterviewRepository.findByClientContainingIgnoreCase(search);
        }
        return clientInterviewRepository.findAll();
    }

    public Client addClient(String name, String contactEmail, Integer activePositions, List<String> technologies) {
        Client client = new Client();
        client.setName(name);
        client.setContactEmail(contactEmail);
        client.setActivePositions(activePositions);
        client.setTechnologies(technologies);
        return clientRepository.save(client);
    }

    public List<Client> getClients(String search) {
        if (search != null && !search.isEmpty()) {
            return clientRepository.findByNameContainingIgnoreCase(search);
        }
        return clientRepository.findAll();
    }

    public ClientInterview getClientInterviewById(Long interviewId) {
        return clientInterviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + interviewId));
    }

    public JobDescription uploadJobDescription(String title, String client, LocalDate receivedDate,
                                              LocalDate deadline, String technology, String resourceType,
                                              String description, MultipartFile file) throws IOException {
        String s3Key = file != null ? s3Service.uploadFile(file, "job-descriptions") : null;
        JobDescription jd = new JobDescription();
        jd.setTitle(title);
        jd.setClient(client);
        jd.setReceivedDate(receivedDate);
        jd.setDeadline(deadline);
        jd.setTechnology(technology);
        jd.setResourceType(resourceType);
        jd.setDescription(description);
        jd.setS3Key(s3Key);
        return jobDescriptionRepository.save(jd);
    }

    public byte[] downloadJobDescription(Long jdId) throws IOException {
        JobDescription jd = jobDescriptionRepository.findById(jdId)
                .orElseThrow(() -> new IllegalArgumentException("Job Description not found with ID: " + jdId));
        if (jd.getS3Key() == null) {
            throw new IllegalArgumentException("No file associated with this Job Description");
        }
        return s3Service.downloadFile(jd.getS3Key());
    }

    public void deleteJobDescription(Long jdId) {
        JobDescription jd = jobDescriptionRepository.findById(jdId)
                .orElseThrow(() -> new IllegalArgumentException("Job Description not found with ID: " + jdId));
        if (jd.getS3Key() != null) {
            s3Service.deleteFile(jd.getS3Key());
        }
        jobDescriptionRepository.delete(jd);
    }

    public List<JobDescription> getAllJobDescriptions() {
        return jobDescriptionRepository.findAll();
    }

    public List<Employee> getDeployedEmployees() {
        return employeeRepository.findByDeployedTrue();
    }
    
    
    public List<ClientInterview> scheduleMultipleClientInterviews(String empId, List<ClientInterviewSchedule> schedules) {
        if (empId == null || empId.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID cannot be null or empty");
        }
        if (schedules == null || schedules.isEmpty()) {
            throw new IllegalArgumentException("Schedules list cannot be null or empty");
        }
        
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + empId));
        
        if (!Boolean.TRUE.equals(employee.getSentToSales())) {
            throw new IllegalArgumentException("Employee with ID: " + empId + " is not eligible for client interviews. Must be sent to sales first.");
        }

        return schedules.stream()
                .map(schedule -> scheduleClientInterview(
                        empId,
                        schedule.getClient(),
                        schedule.getDate(),
                        schedule.getTime(),
                        schedule.getLevel(),
                        schedule.getJobDescriptionTitle(),
                        schedule.getMeetingLink(),
                        schedule.getDeployedStatus()))
                .collect(Collectors.toList());
    }
    
        public static class ClientInterviewSchedule {
        private String client;
        private LocalDate date;
        private LocalTime time;
        private Integer level;
        private String jobDescriptionTitle;
        private String meetingLink;
        private Boolean deployedStatus;

        // Getters and setters
        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public String getJobDescriptionTitle() {
            return jobDescriptionTitle;
        }

        public void setJobDescriptionTitle(String jobDescriptionTitle) {
            this.jobDescriptionTitle = jobDescriptionTitle;
        }

        public String getMeetingLink() {
            return meetingLink;
        }

        public void setMeetingLink(String meetingLink) {
            this.meetingLink = meetingLink;
        }

        public Boolean getDeployedStatus() {
            return deployedStatus;
        }

        public void setDeployedStatus(Boolean deployedStatus) {
            this.deployedStatus = deployedStatus;
        }
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

            User user =userRepository.findById(employeeId)
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

        
}