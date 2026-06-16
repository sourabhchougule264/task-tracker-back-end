package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage realMimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@tasktracker.com");

        Session session = Session.getInstance(new Properties());
        realMimeMessage = new MimeMessage(session);
    }

    @Test
    void sendTaskAssignmentEmail_WhenDisabled_ShouldReturnEarly() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        TaskAssignedEvent mockEvent = mock(TaskAssignedEvent.class);

        emailService.sendTaskAssignmentEmail(mockEvent);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendTaskAssignmentEmail_Success_ShortDescription_WithProjectAndDueDate() throws Exception {

        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        TaskAssignedEvent mockEvent = mock(TaskAssignedEvent.class);

        when(mockEvent.getAssignedUserEmail()).thenReturn("user@example.com");
        when(mockEvent.getTaskId()).thenReturn(123L);
        when(mockEvent.getTaskDescription()).thenReturn("Short Description");
        when(mockEvent.getProjectName()).thenReturn("Project Alpha");
        when(mockEvent.getDueDate()).thenReturn(String.valueOf(LocalDate.parse("2026-05-07")));
        when(mockEvent.getAssignedUsername()).thenReturn("JohnDoe");
        when(mockEvent.getOwnerUsername()).thenReturn("JaneSmith");

        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);


        emailService.sendTaskAssignmentEmail(mockEvent);


        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertEquals("New Task Assigned: Short Description", sentMessage.getSubject());
        assertEquals("noreply@tasktracker.com", sentMessage.getFrom()[0].toString());
        assertEquals("user@example.com", sentMessage.getAllRecipients()[0].toString());
    }

    @Test
    void sendTaskAssignmentEmail_Success_LongDescription_WithoutProjectAndDueDate() throws Exception {

        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        TaskAssignedEvent mockEvent = mock(TaskAssignedEvent.class);

        String longDescription = "This is a very long task description that definitely exceeds 50 characters to test the substring logic.";

        when(mockEvent.getAssignedUserEmail()).thenReturn("user2@example.com");
        when(mockEvent.getTaskId()).thenReturn(456L);
        when(mockEvent.getTaskDescription()).thenReturn(longDescription);
        when(mockEvent.getProjectName()).thenReturn(null);
        when(mockEvent.getDueDate()).thenReturn(null);
        when(mockEvent.getAssignedUsername()).thenReturn("Alice");
        when(mockEvent.getOwnerUsername()).thenReturn("Bob");

        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);


        emailService.sendTaskAssignmentEmail(mockEvent);


        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        String expectedSubject = "New Task Assigned: " + longDescription.substring(0, 50) + "...";
        assertEquals(expectedSubject, sentMessage.getSubject());
    }

    @Test
    void sendTaskAssignmentEmail_WhenExceptionThrown_ShouldCatchAndLog() {

        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        TaskAssignedEvent mockEvent = mock(TaskAssignedEvent.class);
        when(mockEvent.getAssignedUserEmail()).thenReturn("error@example.com");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Simulated mail server failure"));

        assertDoesNotThrow(() -> emailService.sendTaskAssignmentEmail(mockEvent));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}