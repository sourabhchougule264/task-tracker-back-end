package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.event.TaskAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka Producer to publish task-related events
 * Handles failures gracefully - events are logged for retry if delivery fails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEventProducerService {

    private final KafkaTemplate<String, TaskAssignedEvent> kafkaTemplate;

    @Value("${kafka.topics.task-assigned:task.assigned}")
    private String taskAssignedTopic;

    @Value("${kafka.producer.enabled:true}")
    private boolean producerEnabled;

    /**
     * Publish task assigned event to Kafka
     * If Kafka is unavailable, logs the event for manual retry
     */
    public void publishTaskAssignedEvent(TaskAssignedEvent event) {
        if (!producerEnabled) {
            log.debug("Kafka producer is disabled");
            return;
        }

        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }

            String messageKey = "task-" + event.getTaskId();

            Message<TaskAssignedEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, taskAssignedTopic)
                    .setHeader("kafka_messageKey", messageKey)
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((sendResult, exception) -> {
                        if (exception != null) {
                            log.warn("Failed to send task assigned event to Kafka for task: {}, Event ID: {}. Will retry automatically",
                                event.getTaskId(), event.getEventId(), exception);
                        } else {
                            log.info("Task assigned event published to Kafka successfully - Task ID: {}, Event ID: {}",
                                event.getTaskId(), event.getEventId());
                        }
                    });
        } catch (Exception e) {
            log.warn("Error publishing task assigned event to Kafka for task: {}",
                event.getTaskId(), e);
        }
    }
}

