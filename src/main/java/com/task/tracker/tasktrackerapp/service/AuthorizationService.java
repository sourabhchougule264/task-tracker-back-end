package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.entity.Task;
import com.task.tracker.tasktrackerapp.repository.ProjectRepository;
import com.task.tracker.tasktrackerapp.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Service for handling authorization logic based on user roles.
 * Implements role-based access control (RBAC) for the Task Tracker application.
 * Uses AWS Cognito JWT-based authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    // ==================== JWT/COGNITO ROLE CHECKING ====================

    /**
     * Check if JWT authentication has a specific role (from Cognito groups)
     */
    public boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleName));
    }

    /**
     * Check if user has ADMIN role
     */
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ADMIN");
    }

    /**
     * Check if user has TASK_CREATOR role
     */
    public boolean isTaskCreator(Authentication authentication) {
        return hasRole(authentication, "TASK_CREATOR");
    }

    /**
     * Check if user has READ_ONLY role
     */
    public boolean isReadOnly(Authentication authentication) {
        return hasRole(authentication, "READ_ONLY");
    }

    /**
     * Get username from JWT token
     */
    public String getUsernameFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("cognito:username");
        }
        return authentication.getName();
    }

    /**
     * Check if authenticated user is the project owner
     */
    public boolean isProjectOwner(Long projectId, String username) {
        return projectRepository.findById(projectId)
                .map(project -> project.getOwner().getUsername().equals(username))
                .orElse(false);
    }

    /**
     * Check if authenticated user is the task owner
     */
    public boolean isTaskOwner(Long taskId, String username) {
        return taskRepository.findById(taskId)
                .map(task -> task.getOwner().getUsername().equals(username))
                .orElse(false);
    }

    /**
     * Check if authenticated user is assigned to the task
     */
    public boolean isTaskAssignee(Long taskId, String username) {
        return taskRepository.findById(taskId)
                .map(task -> task.getAssignedUser() != null &&
                            task.getAssignedUser().getUsername().equals(username))
                .orElse(false);
    }

    // ==================== PROJECT PERMISSIONS ====================

    /**
     * Check if user can create projects
     * ADMIN: Yes
     * TASK_CREATOR: Yes
     * READ_ONLY: No
     */
    public boolean canCreateProject(Authentication authentication) {
        return isAdmin(authentication) || isTaskCreator(authentication);
    }

    /**
     * Check if user can update a project
     * ADMIN: Yes (any project)
     * TASK_CREATOR: Yes (only projects they own)
     * READ_ONLY: No
     */
    public boolean canUpdateProject(Long projectId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (isTaskCreator(authentication)) {
            String username = getUsernameFromAuth(authentication);
            return isProjectOwner(projectId, username);
        }
        return false;
    }

    /**
     * Check if user can delete a project
     * ADMIN: Yes (any project)
     * TASK_CREATOR: Yes (only projects they own)
     * READ_ONLY: No
     */
    public boolean canDeleteProject(Long projectId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (isTaskCreator(authentication)) {
            String username = getUsernameFromAuth(authentication);
            return isProjectOwner(projectId, username);
        }
        return false;
    }

    // ==================== TASK PERMISSIONS ====================

    /**
     * Check if user can create tasks
     * ADMIN: Yes
     * TASK_CREATOR: Yes
     * READ_ONLY: No
     */
    public boolean canCreateTask(Authentication authentication) {
        return isAdmin(authentication) || isTaskCreator(authentication);
    }

    /**
     * Check if user can update a task
     * ADMIN: Yes (any task)
     * TASK_CREATOR: Yes (tasks they own or are assigned to)
     * READ_ONLY: No (except status updates for assigned tasks)
     */
    public boolean canUpdateTask(Long taskId, Authentication authentication) {
        String username = getUsernameFromAuth(authentication);

        if (isAdmin(authentication)) {
            return true;
        }
        if (isTaskCreator(authentication)) {
            return isTaskOwner(taskId, username) || isTaskAssignee(taskId, username);
        }
        return false;
    }

    /**
     * Check if user can update task status
     * ADMIN: Yes (any task, any status)
     * TASK_CREATOR: Yes (tasks they own or are assigned to, any status)
     * READ_ONLY: Yes (only tasks assigned to them, only to mark complete)
     */
    public boolean canUpdateTaskStatus(Task task, Authentication authentication) {
        String username = getUsernameFromAuth(authentication);

        if (isAdmin(authentication)) {
            return true;
        }

        if (isTaskCreator(authentication)) {
            return task.getOwner().getUsername().equals(username) ||
                   (task.getAssignedUser() != null &&
                    task.getAssignedUser().getUsername().equals(username));
        }

        if (isReadOnly(authentication)) {
            // READ_ONLY can only update status of tasks assigned to them
            return task.getAssignedUser() != null &&
                   task.getAssignedUser().getUsername().equals(username);
        }

        return false;
    }

    /**
     * Check if user can delete a task
     * ADMIN: Yes (any task)
     * TASK_CREATOR: Yes (only tasks they own)
     * READ_ONLY: No
     */
    public boolean canDeleteTask(Long taskId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (isTaskCreator(authentication)) {
            String username = getUsernameFromAuth(authentication);
            return isTaskOwner(taskId, username);
        }
        return false;
    }

    /**
     * Check if user can assign tasks to other users
     * ADMIN: Yes
     * TASK_CREATOR: Yes
     * READ_ONLY: No
     */
    public boolean canAssignTask(Authentication authentication) {
        return isAdmin(authentication) || isTaskCreator(authentication);
    }

    // ==================== USER PERMISSIONS ====================

    /**
     * Check if user can create new users
     * ADMIN: Yes
     * TASK_CREATOR: No
     * READ_ONLY: No
     */
    public boolean canCreateUser(Authentication authentication) {
        return isAdmin(authentication);
    }

    /**
     * Validate and throw exception if user doesn't have permission
     */
    public void requirePermission(boolean hasPermission, String message) {
        if (!hasPermission) {
            throw new SecurityException(message);
        }
    }
}
