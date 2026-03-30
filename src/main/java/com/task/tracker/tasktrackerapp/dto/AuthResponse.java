package com.task.tracker.tasktrackerapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String idToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;
    private String challengeName;  // For authentication challenges like NEW_PASSWORD_REQUIRED
    private String session;        // Session token for challenge responses
    private String message;        // User-friendly message
}

