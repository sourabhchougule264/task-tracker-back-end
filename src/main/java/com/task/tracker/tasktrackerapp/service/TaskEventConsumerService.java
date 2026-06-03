package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka Consumer to consume task-related events and trigger actions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEventConsumerService {

    private final EmailService emailService;

    /**
     * Consume task assigned events and send email notifications
     */
    @KafkaListener(
        topics = "${kafka.topics.task-assigned:task.assigned}",
        groupId = "${kafka.consumer.group-id:task-tracker-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTaskAssignedEvent(TaskAssignedEvent event) {
        try {
            log.info("Received task assigned event - Event ID: {}, Task ID: {}, Assigned To: {}",
                event.getEventId(), event.getTaskId(), event.getAssignedUsername());

            emailService.sendTaskAssignmentEmail(event);

            log.info("Task assigned event processed successfully - Event ID: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing task assigned event - Event ID: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process task assigned event", e);
        }
    }
}


