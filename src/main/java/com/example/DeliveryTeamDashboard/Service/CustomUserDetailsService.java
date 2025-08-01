package com.example.DeliveryTeamDashboard.Service;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.DeliveryTeamDashboard.Entity.User;
import com.example.DeliveryTeamDashboard.Repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
	 private final UserRepository userRepository;

		public CustomUserDetailsService(UserRepository userRepository) {
			this.userRepository = userRepository;
		}

		@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());
		logger.info("Loaded user '{}' with authorities: {}", email, authority.getAuthority());
		return new org.springframework.security.core.userdetails.User(
				user.getEmail(),
				user.getPassword(),
				Collections.singletonList(authority)
		);
	}
}
