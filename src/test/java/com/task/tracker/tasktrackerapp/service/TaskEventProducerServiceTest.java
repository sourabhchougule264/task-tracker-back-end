package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskEventProducerServiceTest {

    @Mock
    private KafkaTemplate<String, TaskAssignedEvent> kafkaTemplate;

    @InjectMocks
    private TaskEventProducerService taskEventProducerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taskEventProducerService, "taskAssignedTopic", "task.assigned");
    }

    @Test
    void publishTaskAssignedEvent_Disabled() {
        ReflectionTestUtils.setField(taskEventProducerService, "producerEnabled", false);
        TaskAssignedEvent event = mock(TaskAssignedEvent.class);

        taskEventProducerService.publishTaskAssignedEvent(event);

        verify(kafkaTemplate, never()).send(any(Message.class));
    }

    @Test
    void publishTaskAssignedEvent_Success_NullEventId() {
        ReflectionTestUtils.setField(taskEventProducerService, "producerEnabled", true);
        TaskAssignedEvent event = mock(TaskAssignedEvent.class);

        when(event.getEventId()).thenReturn(null);
        when(event.getTaskId()).thenReturn(123L);

        CompletableFuture<SendResult<String, TaskAssignedEvent>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        taskEventProducerService.publishTaskAssignedEvent(event);

        verify(event).setEventId(anyString());
        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void publishTaskAssignedEvent_FailureCallback_NonNullEventId() {
        ReflectionTestUtils.setField(taskEventProducerService, "producerEnabled", true);
        TaskAssignedEvent event = mock(TaskAssignedEvent.class);

        when(event.getEventId()).thenReturn("EXISTING-UUID");
        when(event.getTaskId()).thenReturn(456L);

        CompletableFuture<SendResult<String, TaskAssignedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Simulated Kafka Timeout"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        taskEventProducerService.publishTaskAssignedEvent(event);

        verify(event, never()).setEventId(anyString());
        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void publishTaskAssignedEvent_OuterException() {
        ReflectionTestUtils.setField(taskEventProducerService, "producerEnabled", true);
        TaskAssignedEvent event = mock(TaskAssignedEvent.class);

        when(event.getEventId()).thenReturn("EXISTING-UUID");
        when(event.getTaskId()).thenReturn(789L);

        when(kafkaTemplate.send(any(Message.class))).thenThrow(new RuntimeException("Unexpected setup error"));

        assertDoesNotThrow(() -> taskEventProducerService.publishTaskAssignedEvent(event));
        verify(kafkaTemplate).send(any(Message.class));
    }
}