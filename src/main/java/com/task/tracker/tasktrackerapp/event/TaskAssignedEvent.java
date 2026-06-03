package com.task.tracker.tasktrackerapp.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a task is assigned to a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignedEvent {

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("task_description")
    private String taskDescription;

    @JsonProperty("assigned_user_id")
    private Long assignedUserId;

    @JsonProperty("assigned_username")
    private String assignedUsername;

    @JsonProperty("assigned_user_email")
    private String assignedUserEmail;

    @JsonProperty("assigned_user_first_name")
    private String assignedUserFirstName;

    @JsonProperty("owner_username")
    private String ownerUsername;

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("event_id")
    private String eventId;
}

