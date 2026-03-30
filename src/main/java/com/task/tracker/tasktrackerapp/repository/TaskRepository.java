package com.task.tracker.tasktrackerapp.repository;

import com.task.tracker.tasktrackerapp.entity.Task;
import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssignedUserId(Long userId);
    List<Task> findByAssignedUserUsername(String username);
    List<Task> findByOwnerId(Long ownerId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}
