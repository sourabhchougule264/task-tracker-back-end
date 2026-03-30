package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.TaskDTO;
import com.task.tracker.tasktrackerapp.entity.Project;
import com.task.tracker.tasktrackerapp.entity.Task;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import com.task.tracker.tasktrackerapp.repository.ProjectRepository;
import com.task.tracker.tasktrackerapp.repository.TaskRepository;
import com.task.tracker.tasktrackerapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        User owner = userRepository.findById(taskDTO.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found with id: " + taskDTO.getOwnerId()));

        Project project = projectRepository.findById(taskDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDTO.getProjectId()));

        Task task = Task.builder()
                .description(taskDTO.getDescription())
                .dueDate(taskDTO.getDueDate())
                .status(taskDTO.getStatus() != null ? taskDTO.getStatus() : TaskStatus.NEW)
                .owner(owner)
                .project(project)
                .build();

        // Handle assigned user - support both username and userId
        if (taskDTO.getAssignedUsername() != null && !taskDTO.getAssignedUsername().isEmpty()) {
            User assignedUser = userRepository.findByUsername(taskDTO.getAssignedUsername())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with username: " + taskDTO.getAssignedUsername()));
            task.setAssignedUser(assignedUser);
        } else if (taskDTO.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(taskDTO.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with id: " + taskDTO.getAssignedUserId()));
            task.setAssignedUser(assignedUser);
        }

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner not found with username: " + username));

        Project project = null;
        if(taskDTO.getProjectId() != null) {
             project = projectRepository.findById(taskDTO.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDTO.getProjectId()));

        }

        Task task = Task.builder()
                .description(taskDTO.getDescription())
                .dueDate(taskDTO.getDueDate())
                .status(taskDTO.getStatus() != null ? taskDTO.getStatus() : TaskStatus.NEW)
                .owner(owner)
                .project(project)
                .build();

        // Handle assigned user - support both username and userId
        if (taskDTO.getAssignedUsername() != null && !taskDTO.getAssignedUsername().isEmpty()) {
            User assignedUser = userRepository.findByUsername(taskDTO.getAssignedUsername())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with username: " + taskDTO.getAssignedUsername()));
            task.setAssignedUser(assignedUser);
        } else if (taskDTO.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(taskDTO.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with id: " + taskDTO.getAssignedUserId()));
            task.setAssignedUser(assignedUser);
        }

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        return convertToDTO(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByAssignedUser(Long userId) {
        return taskRepository.findByAssignedUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByAssignedUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return taskRepository.findByAssignedUserId(user.getId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setDescription(taskDTO.getDescription());
        task.setDueDate(taskDTO.getDueDate());
        task.setStatus(taskDTO.getStatus());

        // Handle project update
        if (taskDTO.getProjectId() != null) {
            Project project = projectRepository.findById(taskDTO.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDTO.getProjectId()));
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        // Handle assigned user - support both username and userId
        if (taskDTO.getAssignedUsername() != null && !taskDTO.getAssignedUsername().isEmpty()) {
            User assignedUser = userRepository.findByUsername(taskDTO.getAssignedUsername())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with username: " + taskDTO.getAssignedUsername()));
            task.setAssignedUser(assignedUser);
        } else if (taskDTO.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(taskDTO.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with id: " + taskDTO.getAssignedUserId()));
            task.setAssignedUser(assignedUser);
        } else {
            task.setAssignedUser(null);
        }

        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Transactional
    public TaskDTO assignTaskToUser(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        task.setAssignedUser(user);
        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Transactional
    public TaskDTO assignTaskToUserByUsername(Long taskId, String username) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        task.setAssignedUser(user);
        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Transactional
    public TaskDTO updateTaskStatus(Long taskId, TaskStatus status, Authentication authentication) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        // Check if user has permission to update task status
        if (!authorizationService.canUpdateTaskStatus(task, authentication)) {
            throw new SecurityException("You don't have permission to update this task's status");
        }

        // If READ_ONLY user, they can only mark tasks as COMPLETE
        if (authorizationService.isReadOnly(authentication) && status != TaskStatus.COMPLETED) {
            throw new SecurityException("READ_ONLY users can only mark tasks as COMPLETE");
        }

        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    private TaskDTO convertToDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
                .ownerId(task.getOwner().getId())
                .ownerUsername(task.getOwner().getUsername())
                .assignedUserId(task.getAssignedUser() != null ? task.getAssignedUser().getId() : null)
                .assignedUsername(task.getAssignedUser() != null ? task.getAssignedUser().getUsername() : null)
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .build();
    }
}
