package com.example.DeliveryTeamDashboard.Controller;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.DeliveryTeamDashboard.Entity.ClientInterview;
import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.InterviewQuestion;
import com.example.DeliveryTeamDashboard.Entity.JobDescription;
import com.example.DeliveryTeamDashboard.Entity.MockInterview;
import com.example.DeliveryTeamDashboard.Entity.Resume;
import com.example.DeliveryTeamDashboard.Service.EmployeeService;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

     private final EmployeeService employeeService;
     // private final EmployeeRepository employeeRepository; // Removed unused field

     public EmployeeController(EmployeeService employeeService) {
         this.employeeService = employeeService;
    
     }

     @GetMapping("/mock-interviews")
     public ResponseEntity<List<MockInterview>> getMockInterviews(
             @RequestParam(required = false) Long employeeId,
             @RequestParam(defaultValue = "all") String technology,
             @RequestParam(defaultValue = "all") String resourceType) {
         List<MockInterview> interviews = employeeService.getMockInterviews(employeeId, technology, resourceType);
         return ResponseEntity.ok(interviews);
     }

     @GetMapping("/client-interviews")
     public ResponseEntity<List<ClientInterview>> getClientInterviews(
             @RequestParam(required = false) Long employeeId,
             @RequestParam(defaultValue = "all") String technology,
             @RequestParam(defaultValue = "all") String resourceType) {
         List<ClientInterview> interviews = employeeService.getClientInterviews(employeeId, technology, resourceType);
         return ResponseEntity.ok(interviews);
     }
     
     @GetMapping("/{employeeId}")
     public ResponseEntity<?> getEmployeeDetails(@PathVariable Long employeeId) {
         try {
             Employee employee = employeeService.getEmployeeById(employeeId);
             if (employee == null) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
             }
             return ResponseEntity.ok(employee);
         } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @PutMapping("/me")
     public ResponseEntity<?> updateEmployeeDetails(
             Authentication authentication,
             @RequestParam Long employeeId,
             @RequestParam(required = false) String technology,
             @RequestParam(required = false) String empId) {
         if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
         }
         try {
             Employee employee = employeeService.updateEmployeeDetails(employeeId, technology, empId);
             return ResponseEntity.ok(employee);
         } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @GetMapping("/job-descriptions")
     public List<JobDescription> getJobDescriptions(
             @RequestParam(required = false) String search,
             @RequestParam(required = false) String technology,
             @RequestParam(required = false) String resourceType) {
         return employeeService.getJobDescriptions(search, technology, resourceType);
     }

     @PostMapping("/resumes")
     public ResponseEntity<?> uploadResume(
             Authentication authentication,
             @RequestParam Long employeeId,
             @RequestParam Long jdId,
             @RequestParam MultipartFile file) throws IOException {
         if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
         }
         try {
             Resume resume = employeeService.uploadResume(employeeId, jdId, file);
             return ResponseEntity.status(HttpStatus.CREATED).body(resume);
         } catch (IllegalArgumentException | IOException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @GetMapping("/resumes/{resumeId}/download")
     public ResponseEntity<?> downloadResume(@PathVariable Long resumeId) throws IOException {
         try {
             byte[] fileData = employeeService.downloadResume(resumeId);
             ByteArrayResource resource = new ByteArrayResource(fileData);
             return ResponseEntity.ok()
                     .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume.pdf")
                     .contentType(MediaType.APPLICATION_PDF)
                     .contentLength(fileData.length)
                     .body(resource);
         } catch (IllegalArgumentException | IOException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }
     

     @GetMapping("/resumes/employee/{employeeId}/download")
     public ResponseEntity<?> getResumeByEmployeeId(@PathVariable Long employeeId) throws IOException {
         try {
             byte[] fileData = employeeService.downloadResumeByEmployeeId(employeeId);
             ByteArrayResource resource = new ByteArrayResource(fileData);
             return ResponseEntity.ok()
                     .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume.pdf")
                     .contentType(MediaType.APPLICATION_PDF)
                     .contentLength(fileData.length)
                     .body(resource);
         } catch (IllegalArgumentException | IOException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }


     @DeleteMapping("/resumes/{resumeId}")
     public ResponseEntity<?> deleteResume(@PathVariable Long resumeId) {
         try {
             employeeService.deleteResume(resumeId);
             return ResponseEntity.ok().build();
         } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @PostMapping("/interview-questions")
     public ResponseEntity<?> addInterviewQuestion(
             Authentication authentication,
             @RequestParam String technology,
             @RequestParam String question,
             @RequestParam String user) {
         if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
         }
         try {
             InterviewQuestion interviewQuestion = employeeService.addInterviewQuestion(technology, question, user);
             return ResponseEntity.status(HttpStatus.CREATED).body(interviewQuestion);
         } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @GetMapping("/interview-questions")
     public List<InterviewQuestion> getInterviewQuestions(
             @RequestParam(defaultValue = "all") String technology) {
         return employeeService.getInterviewQuestions(technology);
     }
     
     @PutMapping("/profile-picture")
     public ResponseEntity<?> updateProfilePicture(
             Authentication authentication,
             @RequestParam Long employeeId,
             @RequestParam MultipartFile file) throws IOException {
         if (authentication == null || !authentication.isAuthenticated()) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
         }
         String filename = file.getOriginalFilename();
         if (filename == null || !(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".png"))) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile picture must be a JPEG (.jpg, .jpeg) or PNG (.png) file");
         }
         try {
             Employee employee = employeeService.updateProfilePicture(employeeId, file);
             return ResponseEntity.status(HttpStatus.OK).body(employee);
         } catch (IllegalArgumentException | IOException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @GetMapping("/profile-picture/{employeeId}")
     public ResponseEntity<?> getProfilePicture(@PathVariable Long employeeId) throws IOException {
         try {
             byte[] fileData = employeeService.getProfilePicture(employeeId);
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

  

     @GetMapping("/ready-for-deployment")
     public ResponseEntity<List<Employee>> getEmployeesReadyForDeployment(
             @RequestParam(required = false) String technology,
             @RequestParam(required = false) String resourceType) {
         List<Employee> employees = employeeService.getEmployeesReadyForDeployment(technology, resourceType);
         return ResponseEntity.ok(employees);
     }

     @GetMapping("/deployed")
     public ResponseEntity<List<Employee>> getDeployedEmployees() {
         List<Employee> employees = employeeService.getDeployedEmployees();
         return ResponseEntity.ok(employees);
     }

}
