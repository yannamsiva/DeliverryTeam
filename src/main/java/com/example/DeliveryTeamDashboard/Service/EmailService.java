
package com.example.DeliveryTeamDashboard.Service;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service

public class EmailService {
	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String senderEmail;

	// @Autowired removed as it is unnecessary
	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendEmail(String to, String subject, String body, boolean isHtml) throws MessagingException {
		logger.info("Attempting to send email to: {} | Subject: {} | isHtml: {}", to, subject, isHtml);
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body, isHtml);
			helper.setFrom(senderEmail);

			mailSender.send(message);
			logger.info("Email sent successfully to: {}", to);
		} catch (MessagingException e) {
			logger.error("Failed to send email to: {} | Exception: {}", to, e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error while sending email to: {} | Exception: {}", to, e.getMessage(), e);
			throw new MessagingException("Unexpected error: " + e.getMessage(), e);
		}
	}

	public void sendMockInterviewNotification(String employeeEmail, String interviewerEmail, String employeeName,
			String interviewerName, LocalDate date, LocalTime time, List<String> fileUrls) throws MessagingException {
		logger.info("Preparing to send mock interview notification to employee: {} and interviewer: {}", employeeEmail, interviewerEmail);
		String subject = "Mock Interview Scheduled - " + date.toString();
		StringBuilder fileLinks = new StringBuilder();
		if (fileUrls != null && !fileUrls.isEmpty()) {
			fileLinks.append("<p><strong>Attached Files:</strong></p><ul>");
			for (String url : fileUrls) {
				fileLinks.append("<li><a href='").append(url).append("'>Download File</a></li>");
			}
			fileLinks.append("</ul>");
		}

		String employeeBody = String.format(
				"<h3>Mock Interview Scheduled</h3>" + "<p>Dear %s,</p>"
						+ "<p>You have a mock interview scheduled with %s on %s at %s.</p>" + "%s"
						+ "<p>Please prepare accordingly and ensure you are available at the scheduled time.</p>"
						+ "<p>Best regards,<br>Delivery Team</p>",
				employeeName, interviewerName, date.toString(), time.toString(), fileLinks.toString());
		String interviewerBody = String.format(
				"<h3>Mock Interview Assignment</h3>" + "<p>Dear %s,</p>"
						+ "<p>You are scheduled to conduct a mock interview with %s on %s at %s.</p>" + "%s"
						+ "<p>Please ensure you are prepared to evaluate the candidate.</p>"
						+ "<p>Best regards,<br>Delivery Team</p>",
				interviewerName, employeeName, date.toString(), time.toString(), fileLinks.toString());

		sendEmail(employeeEmail, subject, employeeBody, true);
		sendEmail(interviewerEmail, subject, interviewerBody, true);
	}

	public void sendClientInterviewNotification(String employeeEmail, String employeeName, String client,
			LocalDate date, LocalTime time, Integer level, String jobDescriptionTitle, String meetingLink)
			throws MessagingException {
		String subject = "Client Interview Scheduled - " + date.toString();
		String employeeBody = String.format(
				"<h3>Client Interview Scheduled</h3>" + "<p>Dear %s,</p>"
						+ "<p>You have a client interview scheduled with %s on %s at %s.</p>"
						+ "<p><strong>Details:</strong></p>" + "<ul>" + "<li><strong>Level:</strong> %d</li>"
						+ "<li><strong>Job Description:</strong> %s</li>"
						+ "<li><strong>Meeting Link:</strong> <a href='%s'>Join Meeting</a></li>" + "</ul>"
						+ "<p>Please prepare thoroughly and ensure you are available at the scheduled time.</p>"
						+ "<p>Best regards,<br>Sales Team</p>",
				employeeName, client, date.toString(), time.toString(), level, jobDescriptionTitle, meetingLink);
		sendEmail(employeeEmail, subject, employeeBody, true);
	}

//	public void sendMockInterviewFeedbackNotification(String employeeEmail, String employeeName, LocalDate date,
//			LocalTime time, String technicalFeedback, String communicationFeedback, Integer technicalScore,
//			Integer communicationScore) throws MessagingException {
//		String subject = "Mock Interview Feedback - " + date.toString();
//		String employeeBody = String.format("<h3>Mock Interview Feedback</h3>" + "<p>Dear %s,</p>"
//				+ "<p>Your mock interview on %s at %s has been completed. Below is the feedback:</p>"
//				+ "<p><strong>Technical Feedback:</strong> %s</p>" + "<p><strong>Technical Score:</strong> %d</p>"
//				+ "<p><strong>Communication Feedback:</strong> %s</p>"
//				+ "<p><strong>Communication Score:</strong> %d</p>"
//				+ "<p>Please review the feedback and reach out if you have any questions.</p>"
//				+ "<p>Best regards,<br>Delivery Team</p>", employeeName, date.toString(), time.toString(),
//				technicalFeedback, technicalScore, communicationFeedback, communicationScore);
//
//		sendEmail(employeeEmail, subject, employeeBody, true);
//	}

	
	public void sendMockInterviewFeedbackNotification(String employeeEmail, String employeeName,
			LocalDate date, LocalTime time, String technicalFeedback,
			String communicationFeedback, Integer technicalScore,
			Integer communicationScore, String fileUrl) throws MessagingException {
		String subject = "Mock Interview Feedback - " + date.toString();
		String fileLink = fileUrl != null && !fileUrl.isEmpty() 
			? String.format("<p><strong>Feedback File:</strong> <a href='%s'>Download File</a></p>", fileUrl)
			: "<p>No feedback file attached.</p>";
		
		String employeeBody = String.format(
			"<h3>Mock Interview Feedback</h3>" +
			"<p>Dear %s,</p>" +
			"<p>Your mock interview on %s at %s has been completed. Below is the feedback:</p>" +
			"<p><strong>Technical Feedback:</strong> %s</p>" +
			"<p><strong>Technical Score:</strong> %d</p>" +
			"<p><strong>Communication Feedback:</strong> %s</p>" +
			"<p><strong>Communication Score:</strong> %d</p>" +
			"%s" +
			"<p>Please review the feedback and reach out if you have any questions.</p>" +
			"<p>Best regards,<br>Delivery Team</p>",
			employeeName, date.toString(), time.toString(),
			technicalFeedback, technicalScore, communicationFeedback, communicationScore, fileLink
		);
		
		sendEmail(employeeEmail, subject, employeeBody, true);
	}
	
	public void sendClientInterviewFeedbackNotification(String employeeEmail, String employeeName, String client,
			LocalDate date, LocalTime time, String result, String feedback, Integer technicalScore,
			Integer communicationScore, Integer level) throws MessagingException {
		String subject = "Client Interview Feedback - " + date.toString();
		String employeeBody = String.format(
				"<h3>Client Interview Feedback</h3>" + "<p>Dear %s,</p>"
						+ "<p>Your client interview with %s on %s at %s has been completed. Below is the feedback:</p>"
						+ "<p><strong>Result:</strong> %s</p>" + "<p><strong>Feedback:</strong> %s</p>"
						+ "<p><strong>Technical Score:</strong> %d</p>"
						+ "<p><strong>Communication Score:</strong> %d</p>" + "<p><strong>Level:</strong> %d</p>"
						+ "<p>Please review the feedback and reach out if you have any questions.</p>"
						+ "<p>Best regards,<br>Sales Team</p>",
				employeeName, client, date.toString(), time.toString(), result, feedback, technicalScore,
				communicationScore, level);

		sendEmail(employeeEmail, subject, employeeBody, true);
	}
	public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile file) throws MessagingException {
		logger.info("Attempting to send email with attachment to: {} | Subject: {}", to, subject);
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body);
			helper.setFrom(senderEmail);

			if (file != null && !file.isEmpty()) {
				helper.addAttachment(file.getOriginalFilename(), file);
			}

			mailSender.send(message);
			logger.info("Email with attachment sent successfully to: {}", to);
		} catch (MessagingException e) {
			logger.error("Failed to send email with attachment to: {} | Exception: {}", to, e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error while sending email with attachment to: {} | Exception: {}", to, e.getMessage(), e);
			throw new MessagingException("Unexpected error: " + e.getMessage(), e);
		}
	}
}