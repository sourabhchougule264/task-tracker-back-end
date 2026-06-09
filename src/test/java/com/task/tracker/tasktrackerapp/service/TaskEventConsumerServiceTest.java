package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskEventConsumerServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TaskEventConsumerService taskEventConsumerService;

    @Test
    void handleTaskAssignedEvent_Success_ShouldProcessAndSendEmail() {

        TaskAssignedEvent mockEvent = mock(TaskAssignedEvent.class);


        when(mockEvent.getEventId()).thenReturn("EVT-999");
        when(mockEvent.getTaskId()).thenReturn(123L);
        when(mockEvent.getAssignedUsername()).thenReturn("JohnDoe");


        taskEventConsumerService.handleTaskAssignedEvent(mockEvent);


        verify(emailService, times(1)).sendTaskAssignmentEmail(mockEvent);
    }

    @Test
    void handleTaskAssignedEvent_WhenExceptionOccurs_ShouldThrowRuntimeException() {

        TaskAssignedEvent mockEvent = mock(TaskAssignedEvent.class);

        when(mockEvent.getEventId()).thenReturn("EVT-999");
        when(mockEvent.getTaskId()).thenReturn(123L);
        when(mockEvent.getAssignedUsername()).thenReturn("JohnDoe");

        RuntimeException simulatedError = new RuntimeException("Simulated unexpected failure");
        doThrow(simulatedError).when(emailService).sendTaskAssignmentEmail(mockEvent);

        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            taskEventConsumerService.handleTaskAssignedEvent(mockEvent);
        });

        assertEquals("Failed to process task assigned event", thrownException.getMessage());
        assertEquals(simulatedError, thrownException.getCause());

        verify(emailService, times(1)).sendTaskAssignmentEmail(mockEvent);
    }
}