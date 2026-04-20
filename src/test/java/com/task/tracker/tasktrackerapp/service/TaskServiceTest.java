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
        mockAssignee = User.builder().id(2L).username("assigneeUser").build();
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
            // 1. Setup DTO for this specific case (Clear the assignee to avoid the extra DB call)
            taskDTO.setAssignedUsername(null);
            taskDTO.setAssignedUserId(null);

            // 2. Setup Mocks
            when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(mockOwner));
            when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
            when(taskRepository.save(any(Task.class))).thenReturn(mockTask);

            // 3. Execute
            TaskDTO result = taskService.createTask(taskDTO, "ownerUser");

            // 4. Verify
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

                TaskDTO result = taskService.updateTaskStatus(100L, TaskStatus.IN_PROGRESS, auth);

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
}