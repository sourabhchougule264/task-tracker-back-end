package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service to send emails using Spring Mail (SMTP)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@tasktracker.com}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send email to user when task is assigned
     */
    public void sendTaskAssignmentEmail(TaskAssignedEvent event) {
        if (!emailEnabled) {
            log.debug("Email sending is disabled");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.getAssignedUserEmail());
            helper.setSubject(buildSubject(event));

            String htmlContent = buildEmailBody(event);

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Task assignment email sent successfully to: {} for task: {}", event.getAssignedUserEmail(), event.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task assignment email to: {}", event.getAssignedUserEmail(), e);
        }
    }

    /**
     * Build email subject
     */
    private String buildSubject(TaskAssignedEvent event) {
        return String.format("New Task Assigned: %s",
            event.getTaskDescription().length() > 50
                ? event.getTaskDescription().substring(0, 50) + "..."
                : event.getTaskDescription());
    }

    /**
     * Build email body
     */
    private String buildEmailBody(TaskAssignedEvent event) {
        // Replace this with your actual application URL (e.g., your Cloudflare tunnel or EC2 domain)
        String appUrl = "https://indicators-assuming-constraints-bar.trycloudflare.com/";

        StringBuilder body = new StringBuilder();
        body.append("<p>Hi ").append(event.getAssignedUsername()).append(",</p>");
        body.append("<p>A new task has been assigned to you.</p>");

        body.append("<h3>Task Details:</h3>");
        body.append("<hr style='border: none; border-top: 1px solid #ccc; width: 100%; text-align: left; margin-left: 0;'>");
        body.append("<p><b>Task ID:</b> ").append(event.getTaskId()).append("<br>");
        body.append("<b>Description:</b> ").append(event.getTaskDescription()).append("<br>");

        if (event.getProjectName() != null) {
            body.append("<b>Project:</b> ").append(event.getProjectName()).append("<br>");
        }

        if (event.getDueDate() != null) {
            body.append("<b>Due Date:</b> ").append(event.getDueDate()).append("<br>");
        }

        body.append("<b>Assigned By:</b> ").append(event.getOwnerUsername()).append("</p>");
        body.append("<hr style='border: none; border-top: 1px solid #ccc; width: 100%; text-align: left; margin-left: 0;'>");

        // FIX: Adding the dynamic hyperlink here
        body.append("<p>Please log in to the <a href='").append(appUrl)
                .append("' style='color: #0066cc; text-decoration: underline;'>Task Tracker</a> ")
                .append("application to view more details and start working on this task.</p>");

        body.append("<p>Best regards,<br>");
        body.append("<b>Task Tracker Team</b></p>");

        return body.toString();
    }
}

