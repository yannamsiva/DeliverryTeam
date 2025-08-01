package com.example.DeliveryTeamDashboard.Controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.EmployeeRepository;
import com.example.DeliveryTeamDashboard.Service.AuthService;
import com.example.DeliveryTeamDashboard.config.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
	
	private final AuthService authService;
	private final JwtUtil jwtUtil;
	private final EmployeeRepository employeeRepository;

	public AuthController(AuthService authService, JwtUtil jwtUtil, EmployeeRepository employeeRepository) {
		this.authService = authService;
		this.jwtUtil = jwtUtil;
		this.employeeRepository = employeeRepository;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(
			@RequestParam String fullName,
			@RequestParam String empId,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String role,
			@RequestParam(required = false) String technology,
			@RequestParam(required = false) String resourceType) {
		try {
			User user = authService.register(fullName, empId, email, password, role, technology, resourceType);
			String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
			return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user.getRole()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(
			@RequestParam String email,
			@RequestParam String password) {
		try {
			logger.info("Login attempt for email: {}", email);
			User user = authService.login(email, password);
			logger.info("User authenticated: {} with role: {}", user.getEmail(), user.getRole());
			
			String token;
			
			// If user is an employee, include employee ID in the token
			if ("ROLE_EMPLOYEE".equals(user.getRole())) {
				logger.info("User is an employee, looking up employee record for email: {}", user.getEmail());
				Employee employee = employeeRepository.findByUser_Email(user.getEmail()).orElse(null);
				if (employee != null) {
					logger.info("Found employee: ID={}, EmpID={}, Technology={}", employee.getId(), employee.getEmpId(), employee.getTechnology());
					token = jwtUtil.generateToken(user.getEmail(), user.getRole(), employee.getId());
				} else {
					logger.warn("No employee record found for user email: {}", user.getEmail());
					token = jwtUtil.generateToken(user.getEmail(), user.getRole());
				}
			} else {
				logger.info("User is not an employee, generating token without employee ID");
				token = jwtUtil.generateToken(user.getEmail(), user.getRole());
			}
			
			// Create response with user and employee information
			Map<String, Object> response = new HashMap<>();
			response.put("token", token);
			response.put("role", user.getRole());
			response.put("user", user);
			
			// If user is an employee, include employee information
			if ("ROLE_EMPLOYEE".equals(user.getRole())) {
				Employee employee = employeeRepository.findByUser_Email(user.getEmail()).orElse(null);
				if (employee != null) {
					logger.info("Adding employee data to response: ID={}", employee.getId());
					response.put("employee", employee);
					response.put("employeeId", employee.getId());
				} else {
					logger.warn("No employee data to add to response for email: {}", user.getEmail());
				}
			}
			
			logger.info("Login successful for user: {}", user.getEmail());
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (IllegalArgumentException e) {
			logger.error("Login failed for email {}: {}", email, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	private static class AuthResponse {
		private final String token;
		private final String role;

		public AuthResponse(String token, String role) {
			this.token = token;
			this.role = role;
		}

		public String getToken() {
			return token;
		}

		public String getRole() {
			return role;
		}
	}
   // ...existing code...
}