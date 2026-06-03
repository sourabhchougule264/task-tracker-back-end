package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
            String to = event.getAssignedUserEmail();
            String subject = buildSubject(event);
            String body = buildEmailBody(event);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Task assignment email sent successfully to: {} for task: {}", to, event.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task assignment email to: {}", event.getAssignedUserEmail(), e);
            // Don't rethrow - let Kafka retry logic handle it
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
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(event.getAssignedUserFirstName()).append(",\n\n");
        body.append("A new task has been assigned to you.\n\n");
        body.append("Task Details:\n");
        body.append("─────────────\n");
        body.append("Task ID: ").append(event.getTaskId()).append("\n");
        body.append("Description: ").append(event.getTaskDescription()).append("\n");

        if (event.getProjectName() != null) {
            body.append("Project: ").append(event.getProjectName()).append("\n");
        }

        if (event.getDueDate() != null) {
            body.append("Due Date: ").append(event.getDueDate()).append("\n");
        }

        body.append("Assigned By: ").append(event.getOwnerUsername()).append("\n");
        body.append("\n─────────────\n");
        body.append("Please log in to the Task Tracker application to view more details and start working on this task.\n\n");
        body.append("Best regards,\n");
        body.append("Task Tracker Team\n");

        return body.toString();
    }
}

