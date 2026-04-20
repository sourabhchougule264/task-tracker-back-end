package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.entity.Project;
import com.task.tracker.tasktrackerapp.entity.Task;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.repository.ProjectRepository;
import com.task.tracker.tasktrackerapp.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private User ownerUser;
    private User otherUser;
    private Project mockProject;
    private Task mockTask;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setUsername("owner_user");

        otherUser = new User();
        otherUser.setUsername("other_user");

        mockProject = new Project();
        mockProject.setId(1L);
        mockProject.setOwner(ownerUser);

        mockTask = new Task();
        mockTask.setId(10L);
        mockTask.setOwner(ownerUser);
        mockTask.setAssignedUser(otherUser);
    }

    // Helper to create Mock Authentication with Roles and JWT Claims
    private Authentication mockAuth(String username, String role) {
        Authentication auth = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);

        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        lenient().when(jwt.getClaim("cognito:username")).thenReturn(username);
        lenient().when(auth.getName()).thenReturn(username);
        lenient().when(auth.getAuthorities()).thenReturn((List) List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        return auth;
    }

    @Nested
    @DisplayName("Role Checking Tests")
    class RoleTests {
        @Test
        void isAdmin_TrueForAdminRole() {
            Authentication auth = mockAuth("admin", "ADMIN");
            assertTrue(authorizationService.isAdmin(auth));
        }

        @Test
        void isTaskCreator_TrueForCreatorRole() {
            Authentication auth = mockAuth("creator", "TASK_CREATOR");
            assertTrue(authorizationService.isTaskCreator(auth));
        }
    }

    @Nested
    @DisplayName("Project Permission Tests")
    class ProjectPermissionTests {
        @Test
        @DisplayName("Admin can update any project")
        void canUpdateProject_AdminSuccess() {
            Authentication auth = mockAuth("admin", "ADMIN");
            assertTrue(authorizationService.canUpdateProject(1L, auth));
        }

        @Test
        @DisplayName("Task Creator can update own project")
        void canUpdateProject_OwnerSuccess() {
            Authentication auth = mockAuth("owner_user", "TASK_CREATOR");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));

            assertTrue(authorizationService.canUpdateProject(1L, auth));
        }

        @Test
        @DisplayName("Task Creator cannot update someone else's project")
        void canUpdateProject_Forbidden() {
            Authentication auth = mockAuth("wrong_user", "TASK_CREATOR");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));

            assertFalse(authorizationService.canUpdateProject(1L, auth));
        }
    }

    @Nested
    @DisplayName("Task Permission Tests")
    class TaskPermissionTests {
        @Test
        @DisplayName("Task Creator can update task if owner or assignee")
        void canUpdateTask_AssigneeSuccess() {
            Authentication auth = mockAuth("other_user", "TASK_CREATOR");
            when(taskRepository.findById(10L)).thenReturn(Optional.of(mockTask));

            assertTrue(authorizationService.canUpdateTask(10L, auth));
        }

        @Test
        @DisplayName("Read Only user can update status if assigned")
        void canUpdateTaskStatus_ReadOnlyAssignee() {
            Authentication auth = mockAuth("other_user", "READ_ONLY");
            // mockTask is assigned to "other_user"
            assertTrue(authorizationService.canUpdateTaskStatus(mockTask, auth));
        }

        @Test
        @DisplayName("Read Only user cannot update status if NOT assigned")
        void canUpdateTaskStatus_ReadOnlyNotAssignee() {
            Authentication auth = mockAuth("third_user", "READ_ONLY");
            assertFalse(authorizationService.canUpdateTaskStatus(mockTask, auth));
        }

        @Test
        @DisplayName("Task Creator can delete ONLY if owner")
        void canDeleteTask_OwnerOnly() {
            Authentication auth = mockAuth("other_user", "TASK_CREATOR"); // Assignee but not owner
            when(taskRepository.findById(10L)).thenReturn(Optional.of(mockTask));

            assertFalse(authorizationService.canDeleteTask(10L, auth));
        }
    }

    @Nested
    @DisplayName("General Utility Tests")
    class UtilityTests {
        @Test
        @DisplayName("requirePermission throws exception when false")
        void requirePermission_ThrowsException() {
            assertThrows(SecurityException.class, () ->
                    authorizationService.requirePermission(false, "Denied")
            );
        }

        @Test
        @DisplayName("requirePermission does nothing when true")
        void requirePermission_Success() {
            assertDoesNotThrow(() ->
                    authorizationService.requirePermission(true, "Allowed")
            );
        }

        @Test
        @DisplayName("getUsernameFromAuth handles JWT principal correctly")
        void getUsername_FromJwt() {
            Authentication auth = mockAuth("test_user", "USER");
            assertEquals("test_user", authorizationService.getUsernameFromAuth(auth));
        }
    }
}