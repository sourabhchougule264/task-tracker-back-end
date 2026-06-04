package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.TaskDTO;
import com.task.tracker.tasktrackerapp.entity.Project;
import com.task.tracker.tasktrackerapp.entity.Task;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import com.task.tracker.tasktrackerapp.repository.ProjectRepository;
import com.task.tracker.tasktrackerapp.repository.TaskRepository;
import com.task.tracker.tasktrackerapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private TaskEventProducerService taskEventProducerService;

    @InjectMocks
    private TaskService taskService;

    private User mockOwner;
    private User mockAssignee;
    private Project mockProject;
    private Task mockTask;
    private TaskDTO taskDTO;

    @BeforeEach
    void setUp() {
        mockOwner = User.builder().id(1L).username("ownerUser").build();
        mockAssignee = User.builder().id(2L).username("assigneeUser").email("assignee@example.com").firstName("John").build();
        mockProject = Project.builder().id(10L).name("Test Project").build();

        mockTask = Task.builder()
                .id(100L)
                .description("Test Task")
                .status(TaskStatus.NEW)
                .owner(mockOwner)
                .project(mockProject)
                .build();

        taskDTO = TaskDTO.builder()
                .description("Test Task")
                .projectId(10L)
                .ownerId(1L)
                .status(TaskStatus.NEW)
                .assignedUsername("assigneeUser")
                .build();
    }

    @Nested
    @DisplayName("Create Task Tests")
    class CreateTaskTests {
        @Test
        @DisplayName("Should create task successfully with DTO info")
        void createTask_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findByUsername("assigneeUser")).thenReturn(Optional.of(mockAssignee));
            when(taskRepository.save(any(Task.class))).thenReturn(mockTask);

            TaskDTO result = taskService.createTask(taskDTO);

            assertThat(result).isNotNull();
            assertThat(result.getDescription()).isEqualTo("Test Task");
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should create task using context username")
        void createTask_WithUsername_Success() {
            taskDTO.setAssignedUsername(null);
            taskDTO.setAssignedUserId(null);

            when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(taskRepository.save(any(Task.class))).thenReturn(mockTask);

            TaskDTO result = taskService.createTask(taskDTO, "ownerUser");

            assertThat(result.getOwnerUsername()).isEqualTo("ownerUser");
            verify(userRepository, never()).findByUsername("assigneeUser");
        }

        @Nested
        @DisplayName("Update and Status Tests")
        class StatusTests {
            @Test
            @DisplayName("Should update status if authorized")
            void updateTaskStatus_Success() {
                Authentication auth = mock(Authentication.class);
                when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
                when(authorizationService.canUpdateTaskStatus(any(), any())).thenReturn(true);
                when(taskRepository.save(any())).thenReturn(mockTask);

                taskService.updateTaskStatus(100L, TaskStatus.IN_PROGRESS, auth);

                assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            }

            @Test
            @DisplayName("Should throw SecurityException if unauthorized")
            void updateTaskStatus_Unauthorized() {
                Authentication auth = mock(Authentication.class);
                when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
                when(authorizationService.canUpdateTaskStatus(any(), any())).thenReturn(false);

                assertThatThrownBy(() -> taskService.updateTaskStatus(100L, TaskStatus.COMPLETED, auth))
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("permission");
            }

            @Test
            @DisplayName("Should prevent READ_ONLY from setting status other than COMPLETED")
            void updateTaskStatus_ReadOnlyRestriction() {
                Authentication auth = mock(Authentication.class);
                when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
                when(authorizationService.canUpdateTaskStatus(any(), any())).thenReturn(true);
                when(authorizationService.isReadOnly(auth)).thenReturn(true);

                assertThatThrownBy(() -> taskService.updateTaskStatus(100L, TaskStatus.IN_PROGRESS, auth))
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("mark tasks as COMPLETE");
            }
        }

        @Nested
        @DisplayName("Assignment Tests")
        class AssignmentTests {
            @Test
            @DisplayName("Should assign task by username")
            void assignByUsername_Success() {
                when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
                when(userRepository.findByUsername("assigneeUser")).thenReturn(Optional.of(mockAssignee));
                when(taskRepository.save(any())).thenReturn(mockTask);

                taskService.assignTaskToUserByUsername(100L, "assigneeUser");

                assertThat(mockTask.getAssignedUser()).isEqualTo(mockAssignee);
                verify(taskEventProducerService, times(1)).publishTaskAssignedEvent(any());
            }
        }

        @Nested
        @DisplayName("Retrieval and Deletion Tests")
        class RetrievalTests {
            @Test
            @DisplayName("Should return tasks by project ID")
            void getTasksByProject_Success() {
                when(taskRepository.findByProjectId(10L)).thenReturn(List.of(mockTask));

                List<TaskDTO> result = taskService.getTasksByProject(10L);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getProjectId()).isEqualTo(10L);
            }

            @Test
            @DisplayName("Should delete task if exists")
            void deleteTask_Success() {
                when(taskRepository.existsById(100L)).thenReturn(true);

                taskService.deleteTask(100L);

                verify(taskRepository).deleteById(100L);
            }

            @Test
            @DisplayName("Should throw error when deleting non-existent task")
            void deleteTask_NotFound() {
                when(taskRepository.existsById(999L)).thenReturn(false);

                assertThatThrownBy(() -> taskService.deleteTask(999L))
                        .isInstanceOf(RuntimeException.class);
            }
        }
    }

    @Nested
    @DisplayName("Get Task Tests")
    class GetTaskTests {
        @Test
        @DisplayName("Should get task by ID successfully")
        void getTaskById_Success() {
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

            TaskDTO result = taskService.getTaskById(100L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getDescription()).isEqualTo("Test Task");
        }

        @Test
        @DisplayName("Should throw error when task not found by ID")
        void getTaskById_NotFound() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found with id: 999");
        }

        @Test
        @DisplayName("Should get all tasks successfully")
        void getAllTasks_Success() {
            when(taskRepository.findAll()).thenReturn(List.of(mockTask));

            List<TaskDTO> result = taskService.getAllTasks();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Test Task");
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void getAllTasks_Empty() {
            when(taskRepository.findAll()).thenReturn(List.of());

            List<TaskDTO> result = taskService.getAllTasks();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get tasks by assigned user ID")
        void getTasksByAssignedUser_Success() {
            when(taskRepository.findByAssignedUserId(2L)).thenReturn(List.of(mockTask));

            List<TaskDTO> result = taskService.getTasksByAssignedUser(2L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get tasks by assigned username")
        void getTasksByAssignedUsername_Success() {
            when(userRepository.findByUsername("assigneeUser")).thenReturn(Optional.of(mockAssignee));
            when(taskRepository.findByAssignedUserId(2L)).thenReturn(List.of(mockTask));

            List<TaskDTO> result = taskService.getTasksByAssignedUsername("assigneeUser");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should throw error when user not found by username")
        void getTasksByAssignedUsername_UserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTasksByAssignedUsername("nonexistent"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found with username: nonexistent");
        }

        @Test
        @DisplayName("Should get tasks by status")
        void getTasksByStatus_Success() {
            when(taskRepository.findByStatus(TaskStatus.NEW)).thenReturn(List.of(mockTask));

            List<TaskDTO> result = taskService.getTasksByStatus(TaskStatus.NEW);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTaskTests {
        @Test
        @DisplayName("Should update task successfully with all fields")
        void updateTask_Success() {
            TaskDTO updateDTO = TaskDTO.builder()
                    .description("Updated Task")
                    .dueDate(LocalDate.parse("2026-12-31"))
                    .status(TaskStatus.IN_PROGRESS)
                    .projectId(10L)
                    .assignedUserId(2L)
                    .build();

            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findById(2L)).thenReturn(Optional.of(mockAssignee));
            when(taskRepository.save(any())).thenReturn(mockTask);

            TaskDTO result = taskService.updateTask(100L, updateDTO);

            assertThat(result).isNotNull();
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should update task with null project")
        void updateTask_NullProject() {
            TaskDTO updateDTO = TaskDTO.builder()
                    .description("Updated Task")
                    .status(TaskStatus.IN_PROGRESS)
                    .projectId(null)
                    .build();

            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(taskRepository.save(any())).thenReturn(mockTask);

            taskService.updateTask(100L, updateDTO);

            assertThat(mockTask.getProject()).isNull();
        }

        @Test
        @DisplayName("Should update task with username instead of ID")
        void updateTask_WithUsername() {
            TaskDTO updateDTO = TaskDTO.builder()
                    .description("Updated Task")
                    .projectId(10L)
                    .assignedUsername("assigneeUser")
                    .build();

            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findByUsername("assigneeUser")).thenReturn(Optional.of(mockAssignee));
            when(taskRepository.save(any())).thenReturn(mockTask);

            taskService.updateTask(100L, updateDTO);

            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should update task and clear assigned user")
        void updateTask_ClearAssignedUser() {
            TaskDTO updateDTO = TaskDTO.builder()
                    .description("Updated Task")
                    .assignedUsername(null)
                    .assignedUserId(null)
                    .build();

            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(taskRepository.save(any())).thenReturn(mockTask);

            taskService.updateTask(100L, updateDTO);

            assertThat(mockTask.getAssignedUser()).isNull();
        }

        @Test
        @DisplayName("Should throw error when updating non-existent task")
        void updateTask_NotFound() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(999L, taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when project not found during update")
        void updateTask_ProjectNotFound() {
            taskDTO.setProjectId(999L);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(100L, taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found");
        }

        @Test
        @DisplayName("Should throw error when assigned user not found by username during update")
        void updateTask_AssignedUserNotFoundByUsername() {
            taskDTO.setAssignedUsername("nonexistent");
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(100L, taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Assigned user not found with username: nonexistent");
        }

        @Test
        @DisplayName("Should throw error when assigned user not found by ID during update")
        void updateTask_AssignedUserNotFoundById() {
            TaskDTO updateDTO = TaskDTO.builder()
                    .description("Updated Task")
                    .projectId(10L)
                    .assignedUserId(999L)
                    .build();
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(100L, updateDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Assigned user not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Assign Task Tests")
    class AssignTaskTests {
        @Test
        @DisplayName("Should assign task to user by ID")
        void assignTaskToUser_Success() {
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(userRepository.findById(2L)).thenReturn(Optional.of(mockAssignee));
            when(taskRepository.save(any())).thenReturn(mockTask);

            TaskDTO result = taskService.assignTaskToUser(100L, 2L);

            assertThat(result).isNotNull();
            assertThat(mockTask.getAssignedUser()).isEqualTo(mockAssignee);
            verify(taskEventProducerService, times(1)).publishTaskAssignedEvent(any());
        }

        @Test
        @DisplayName("Should throw error when task not found in assignTaskToUser")
        void assignTaskToUser_TaskNotFound() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignTaskToUser(999L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when user not found in assignTaskToUser")
        void assignTaskToUser_UserNotFound() {
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignTaskToUser(100L, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when task not found in assignTaskToUserByUsername")
        void assignTaskToUserByUsername_TaskNotFound() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignTaskToUserByUsername(999L, "assigneeUser"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when user not found in assignTaskToUserByUsername")
        void assignTaskToUserByUsername_UserNotFound() {
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignTaskToUserByUsername(100L, "nonexistent"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found with username: nonexistent");
        }
    }

    @Nested
    @DisplayName("Create Task Error Tests")
    class CreateTaskErrorTests {
        @Test
        @DisplayName("Should throw error when owner not found in createTask")
        void createTask_OwnerNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            taskDTO.setOwnerId(999L);

            assertThatThrownBy(() -> taskService.createTask(taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Owner not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when project not found in createTask")
        void createTask_ProjectNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            taskDTO.setProjectId(999L);

            assertThatThrownBy(() -> taskService.createTask(taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when assigned user not found by username in createTask")
        void createTask_AssignedUserNotFoundByUsername() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
            taskDTO.setAssignedUsername("nonexistent");

            assertThatThrownBy(() -> taskService.createTask(taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Assigned user not found with username: nonexistent");
        }

        @Test
        @DisplayName("Should throw error when assigned user not found by ID in createTask")
        void createTask_AssignedUserNotFoundById() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            taskDTO.setAssignedUsername(null);
            taskDTO.setAssignedUserId(999L);

            assertThatThrownBy(() -> taskService.createTask(taskDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Assigned user not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when owner not found in createTask with username")
        void createTaskWithUsername_OwnerNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(taskDTO, "nonexistent"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Owner not found with username: nonexistent");
        }

        @Test
        @DisplayName("Should create task with null project in createTask with username")
        void createTaskWithUsername_NullProject() {
            taskDTO.setProjectId(null);
            taskDTO.setAssignedUsername(null);
            taskDTO.setAssignedUserId(null);
            when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(mockOwner));
            when(taskRepository.save(any())).thenReturn(mockTask);

            TaskDTO result = taskService.createTask(taskDTO, "ownerUser");

            assertThat(result).isNotNull();
            verify(userRepository, never()).findByUsername("assigneeUser");
        }

        @Test
        @DisplayName("Should throw error when project not found in createTask with username")
        void createTaskWithUsername_ProjectNotFound() {
            when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            taskDTO.setProjectId(999L);

            assertThatThrownBy(() -> taskService.createTask(taskDTO, "ownerUser"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found with id: 999");
        }

        @Test
        @DisplayName("Should throw error when assigned user not found by username in createTask with username")
        void createTaskWithUsername_AssignedUserNotFoundByUsername() {
            when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
            taskDTO.setAssignedUsername("nonexistent");

            assertThatThrownBy(() -> taskService.createTask(taskDTO, "ownerUser"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Assigned user not found with username: nonexistent");
        }

        @Test
        @DisplayName("Should throw error when assigned user not found by ID in createTask with username")
        void createTaskWithUsername_AssignedUserNotFoundById() {
            when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            taskDTO.setAssignedUsername(null);
            taskDTO.setAssignedUserId(999L);

            assertThatThrownBy(() -> taskService.createTask(taskDTO, "ownerUser"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Assigned user not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Task Status Update Error Tests")
    class TaskStatusUpdateErrorTests {
        @Test
        @DisplayName("Should throw error when task not found in updateTaskStatus")
        void updateTaskStatus_TaskNotFound() {
            Authentication auth = mock(Authentication.class);
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTaskStatus(999L, TaskStatus.COMPLETED, auth))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found with id: 999");
        }
    }
}