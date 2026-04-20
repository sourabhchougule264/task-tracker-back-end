package com.task.tracker.tasktrackerapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.tracker.tasktrackerapp.config.TestSecurityConfig;
import com.task.tracker.tasktrackerapp.dto.ProjectDTO;
import com.task.tracker.tasktrackerapp.service.AuthorizationService;
import com.task.tracker.tasktrackerapp.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(TestSecurityConfig.class)
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectDTO projectDTO;

    @BeforeEach
    void setUp() {
        projectDTO = new ProjectDTO();
        projectDTO.setId(1L);
        projectDTO.setName("Test Project");
        projectDTO.setDescription("Test Description");
    }

    // --- CREATE PROJECT TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProject_ValidData_ReturnsCreated() throws Exception {
        when(projectService.createProject(any(ProjectDTO.class), anyString())).thenReturn(projectDTO);

        mockMvc.perform(post("/projects")
                        .with(csrf()) // CSRF is required for POST/PUT/DELETE in Spring Security
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY") // User role shouldn't have access to create
    void createProject_UnauthorizedRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectDTO)))
                .andExpect(status().isForbidden());
    }

    // --- GET PROJECT TESTS ---

    @Test
    @WithMockUser
    void getProjectById_Exists_ReturnsProject() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(projectDTO);

        mockMvc.perform(get("/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "sourabh")
    void getMyProjects_ReturnsList() throws Exception {
        when(projectService.getProjectsByOwnerUsername("sourabh"))
                .thenReturn(Collections.singletonList(projectDTO));

        mockMvc.perform(get("/projects/my-projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Project"));
    }

    // --- UPDATE/DELETE TESTS (Security logic) ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProject_AsAdmin_ReturnsOk() throws Exception {
        when(projectService.updateProject(eq(1L), any(ProjectDTO.class))).thenReturn(projectDTO);

        mockMvc.perform(put("/projects/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dummyUser")
    void deleteProject_AsNonOwner_ReturnsForbidden() throws Exception {

        when(authorizationService.isProjectOwner(eq(1L), eq("dummyUser")))
                .thenReturn(false);

        mockMvc.perform(delete("/projects/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}