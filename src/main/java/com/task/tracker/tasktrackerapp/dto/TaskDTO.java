package com.task.tracker.tasktrackerapp.dto;

import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {
    private Long id;
    private String description;
    private LocalDate dueDate;
    private TaskStatus status;
    private Long ownerId;
    private String ownerUsername;
    private Long assignedUserId;
    private String assignedUsername;
    private Long projectId;
    private String projectName;
}
