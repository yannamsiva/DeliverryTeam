package com.example.DeliveryTeamDashboard.Service;

// ...existing code...

// ...existing code...
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.DeliveryTeamDashboard.Entity.Employee;
import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.EmployeeRepository;
import com.example.DeliveryTeamDashboard.Repository.UserRepository;
// ...existing code...

@Service
public class AuthService {

    
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String fullName, String empId, String email, String password, String role, String technology, String resourceType) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (employeeRepository.existsByEmpId(empId)) {
            throw new IllegalArgumentException("Employee ID already exists");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_" + role.toUpperCase().replace("-", "_"));
        User savedUser = userRepository.save(user);

        if (role.equalsIgnoreCase("employee")) {
            Employee employee = new Employee();
            employee.setUser(savedUser);
            employee.setEmpId(empId);
            employee.setTechnology(technology);
            employee.setResourceType(resourceType);
            employee.setStatus("Active");
            employeeRepository.save(employee);
        }

        return savedUser;
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return user;
    }
}
