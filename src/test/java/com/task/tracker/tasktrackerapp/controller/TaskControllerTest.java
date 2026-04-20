package com.task.tracker.tasktrackerapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.tracker.tasktrackerapp.config.TestSecurityConfig;
import com.task.tracker.tasktrackerapp.dto.TaskDTO;
import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import com.task.tracker.tasktrackerapp.service.AuthorizationService;
import com.task.tracker.tasktrackerapp.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(TestSecurityConfig.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder; // Prevents context failure due to missing JWK URI

    private TaskDTO taskDTO;

    @BeforeEach
    void setUp() {
        taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setDescription("Test Description");
        taskDTO.setStatus(TaskStatus.NEW);
        taskDTO.setProjectId(100L);
    }

    // --- CREATE TASK TESTS ---

    @Test
    @DisplayName("POST /tasks - Success as TASK_CREATOR")
    @WithMockUser(username = "creator_user", roles = "TASK_CREATOR")
    void createTask_Success() throws Exception {
        when(taskService.createTask(any(TaskDTO.class), eq("creator_user"))).thenReturn(taskDTO);

        mockMvc.perform(post("/tasks")
                        .with(csrf()) // Required for POST/PUT/PATCH/DELETE in tests
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    @DisplayName("POST /tasks - Forbidden for READ_ONLY user")
    @WithMockUser(roles = "READ_ONLY")
    void createTask_Forbidden() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isForbidden());
    }

    // --- GET TASK TESTS ---

    @Test
    @DisplayName("GET /tasks/{id} - Success")
    @WithMockUser
    void getTaskById_Success() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(taskDTO);

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /tasks/my-tasks - Success")
    @WithMockUser(username = "john_doe")
    void getMyTasks_Success() throws Exception {
        when(taskService.getTasksByAssignedUsername("john_doe")).thenReturn(List.of(taskDTO));

        mockMvc.perform(get("/tasks/my-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // --- UPDATE TASK TESTS ---

    @Test
    @DisplayName("PUT /tasks/{id} - Success as Admin")
    @WithMockUser(roles = "ADMIN")
    void updateTask_AdminSuccess() throws Exception {
        when(taskService.updateTask(eq(1L), any(TaskDTO.class))).thenReturn(taskDTO);

        mockMvc.perform(put("/tasks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /tasks/{id} - Success as Owner")
    @WithMockUser(username = "sourabh")
    void updateTask_OwnerSuccess() throws Exception {
        // Mock the custom authorization service expression
        when(authorizationService.isTaskOwner(eq(1L), eq("sourabh"))).thenReturn(true);
        when(taskService.updateTask(eq(1L), any(TaskDTO.class))).thenReturn(taskDTO);

        mockMvc.perform(put("/tasks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isOk());
    }

    // --- PATCH TESTS (Assign/Status) ---

    @Test
    @DisplayName("PATCH /tasks/{id}/assign/{user} - Success as TASK_CREATOR")
    @WithMockUser(roles = "TASK_CREATOR")
    void assignTask_Success() throws Exception {
        when(taskService.assignTaskToUserByUsername(1L, "new_user")).thenReturn(taskDTO);

        mockMvc.perform(patch("/tasks/1/assign/new_user")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /tasks/{id}/status/{status} - Success")
    @WithMockUser(username = "user1")
    void updateStatus_Success() throws Exception {
        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.COMPLETED), any())).thenReturn(taskDTO);

        mockMvc.perform(patch("/tasks/1/status/COMPLETED")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    // --- DELETE TESTS ---

    @Test
    @DisplayName("DELETE /tasks/{id} - Success as Admin")
    @WithMockUser(roles = "ADMIN")
    void deleteTask_Success() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/tasks/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /tasks/{id} - Forbidden for unauthorized user")
    @WithMockUser(username = "random_user")
    void deleteTask_Forbidden() throws Exception {
        // User is not admin and not owner
        when(authorizationService.isTaskOwner(eq(1L), eq("random_user"))).thenReturn(false);

        mockMvc.perform(delete("/tasks/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}