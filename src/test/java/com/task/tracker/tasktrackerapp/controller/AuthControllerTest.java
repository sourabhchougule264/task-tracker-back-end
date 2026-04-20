package com.task.tracker.tasktrackerapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.tracker.tasktrackerapp.config.TestSecurityConfig;
import com.task.tracker.tasktrackerapp.dto.AuthRequest;
import com.task.tracker.tasktrackerapp.dto.AuthResponse;
import com.task.tracker.tasktrackerapp.service.CognitoAuthService;
import com.task.tracker.tasktrackerapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CognitoAuthService cognitoAuthService;

    @MockitoBean
    private UserService userService;

    private AuthRequest authRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");
        authRequest.setEmail("test@example.com");

        authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .idToken("id-token")
                .refreshToken("refresh-token")
                .message("Success")
                .build();
    }

    @Test
    @DisplayName("POST /auth/login - Success")
    @WithMockUser
    void login_Success() throws Exception {
        when(cognitoAuthService.authenticate(anyString(), anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.idToken").value("id-token"));

        verify(cognitoAuthService, times(1)).syncUserToDatabase(eq("id-token"), any());
    }

    @Test
    @DisplayName("POST /auth/register - Success")
    @WithMockUser
    void register_Success() throws Exception {
        doNothing().when(cognitoAuthService).registerUser(anyString(), anyString(), anyString());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully. Please check your email for verification code."));

        verify(userService, times(1)).syncUserFromCognito(eq("testuser"), eq("test@example.com"), isNull());
    }

    @Test
    @DisplayName("POST /auth/confirm - Success")
    @WithMockUser
    void confirmRegistration_Success() throws Exception {
        Map<String, String> request = Map.of("username", "testuser", "confirmationCode", "123456");

        mockMvc.perform(post("/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/assign-role - Access Denied for non-admin")
    @WithMockUser(roles = "USER")
    void assignRole_AccessDenied() throws Exception {
        Map<String, String> request = Map.of("username", "testuser", "role", "TASK_CREATOR");

        mockMvc.perform(post("/auth/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /auth/assign-role - Success for Admin")
    @WithMockUser(roles = "ADMIN")
    void assignRole_Success() throws Exception {
        Map<String, String> request = Map.of("username", "testuser", "role", "TASK_CREATOR");

        mockMvc.perform(post("/auth/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TASK_CREATOR"));

        verify(cognitoAuthService, times(1)).replaceUserRole("testuser", "TASK_CREATOR");
    }

    @Test
    @DisplayName("POST /auth/forgot-password - Success")
    @WithMockUser
    void forgotPassword_Success() throws Exception {
        Map<String, String> request = Map.of("username", "testuser");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset code sent to your email"));
    }

    @Test
    @DisplayName("GET /auth/health - Success")
    @WithMockUser
    void healthCheck_Success() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Authentication Service"));
    }
}