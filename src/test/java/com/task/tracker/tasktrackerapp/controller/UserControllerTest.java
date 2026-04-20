package com.task.tracker.tasktrackerapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.tracker.tasktrackerapp.config.TestSecurityConfig;
import com.task.tracker.tasktrackerapp.dto.UserDTO;
import com.task.tracker.tasktrackerapp.service.AuthorizationService;
import com.task.tracker.tasktrackerapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userDTO = UserDTO.builder()
                .id(101L)
                .username("jdoe_99")
                .email("jane.doe@example.com")
                .cognitoSub("0133fd5a-b001-700b-cadb-31587fd2d95c")
                .firstName("Jane")
                .lastName("Doe")
                .isActive(true)
                .roles(List.of("USER"))
                .build();
    }

    // --- CREATE USER TESTS ---

    @Test
    @DisplayName("POST /users - Success as ADMIN")
    @WithMockUser(roles = "ADMIN")
    void createUserProfile_Success() throws Exception {
        when(userService.createUserProfile(any(UserDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("jdoe_99"));
    }

    @Test
    @DisplayName("POST /users - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void createUserProfile_Forbidden() throws Exception {
        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isForbidden());
    }

    // --- GET USER TESTS ---

    @Test
    @DisplayName("GET /users/me - Success")
    @WithMockUser(username = "jdoe_99")
    void getCurrentUser_Success() throws Exception {
        when(userService.getUserByUsername("jdoe_99")).thenReturn(userDTO);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));
    }

    @Test
    @DisplayName("GET /users/{id} - Success")
    @WithMockUser
    void getUserById_Success() throws Exception {
        when(userService.getUserById(101L)).thenReturn(userDTO);

        mockMvc.perform(get("/users/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));
    }

    @Test
    @DisplayName("GET /users - Success as ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_AdminSuccess() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // --- UPDATE USER TESTS ---

    @Test
    @DisplayName("PUT /users/{id} - Success as ADMIN")
    @WithMockUser(roles = "ADMIN")
    void updateUser_AdminSuccess() throws Exception {
        when(userService.updateUser(eq(101L), any(UserDTO.class))).thenReturn(userDTO);

        mockMvc.perform(put("/users/101")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/{id} - Success as Owner")
    @WithMockUser(roles = "ADMIN")
    @Disabled
    void updateUser_OwnerSuccess() throws Exception {
        String subClaim = "0133fd5a-b001-700b-cadb-31587fd2d95c";
        UserDTO userDTO1 = new UserDTO();
        userDTO1.setId(1L);
        userDTO1.setUsername("jdoe_99");
        userDTO1.setFirstName("Jane");
        userDTO1.setLastName("Doe");
        userDTO1.setIsActive(true);
        userDTO1.setRoles(List.of("ADMIN"));
        userDTO1.setCognitoSub(subClaim);

        when(userService.updateUser(eq(101L), any(UserDTO.class))).thenReturn(userDTO1);

        mockMvc.perform(put("/users/101")
                        .with(csrf())
                        .with(jwt().jwt(j -> j.claim("username", subClaim)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO1)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/{id} - Forbidden for other user")
    void updateUser_OtherUserForbidden() throws Exception {
        // Different sub than what is in the URI logic
        mockMvc.perform(put("/users/101")
                        .with(csrf())
                        .with(jwt().jwt(builder -> builder.claim("sub", "0133fd5a-b001-700b-cadb-31587fd2d95v")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isForbidden());
    }

    // --- DELETE USER TESTS ---

    @Test
    @DisplayName("DELETE /users/{id} - Success as ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(101L);

        mockMvc.perform(delete("/users/101")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/{id} - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void deleteUser_Forbidden() throws Exception {
        mockMvc.perform(delete("/users/101")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}