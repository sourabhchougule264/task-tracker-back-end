package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.Utility.AuthUtility;
import com.task.tracker.tasktrackerapp.dto.TaskDTO;
import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import com.task.tracker.tasktrackerapp.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task
     * Requires: ADMIN or TASK_CREATOR role
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATOR')")
    public ResponseEntity<TaskDTO> createTask(
            @RequestBody TaskDTO taskDTO,
            Authentication authentication) {

        String username = AuthUtility.getUsernameFromAuth(authentication);
        TaskDTO createdTask = taskService.createTask(taskDTO, username);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    /**
     * Get task by ID
     * Requires: Any authenticated user
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    /**
     * Get all tasks
     * Requires: Any authenticated user
     */
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by project
     * Requires: Any authenticated user
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(@PathVariable Long projectId) {
        List<TaskDTO> tasks = taskService.getTasksByProject(projectId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks assigned to a user
     * Requires: Any authenticated user
     */
    @GetMapping("/assigned/{username}")
    public ResponseEntity<List<TaskDTO>> getTasksByAssignedUser(@PathVariable String username) {
        List<TaskDTO> tasks = taskService.getTasksByAssignedUsername(username);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get current user's assigned tasks
     * Requires: Any authenticated user
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskDTO>> getMyTasks(Authentication authentication) {
        String username = AuthUtility.getUsernameFromAuth(authentication);
        List<TaskDTO> tasks = taskService.getTasksByAssignedUsername(username);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by status
     * Requires: Any authenticated user
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskDTO>> getTasksByStatus(@PathVariable TaskStatus status) {
        List<TaskDTO> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Update task
     * Requires: ADMIN or task owner or assigned user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name) or @authorizationService.isTaskAssignee(#id, authentication.name)")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO taskDTO,
            Authentication authentication) {

        TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Assign task to a user
     * Requires: ADMIN or TASK_CREATOR role
     */
    @PatchMapping("/{taskId}/assign/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATOR')")
    public ResponseEntity<TaskDTO> assignTaskToUser(
            @PathVariable Long taskId,
            @PathVariable String username,
            Authentication authentication) {

        TaskDTO updatedTask = taskService.assignTaskToUserByUsername(taskId, username);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Update task status
     * READ_ONLY: Can only mark assigned tasks as complete
     * TASK_CREATOR: Can update status of owned or assigned tasks
     * ADMIN: Can update any task status
     */
    @PatchMapping("/{taskId}/status/{status}")
    public ResponseEntity<TaskDTO> updateTaskStatus(
            @PathVariable Long taskId,
            @PathVariable TaskStatus status,
            Authentication authentication) {

        String username = AuthUtility.getUsernameFromAuth(authentication);
        TaskDTO updatedTask = taskService.updateTaskStatus(taskId, status, authentication);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Delete task
     * Requires: ADMIN or task owner (TASK_CREATOR)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

}
