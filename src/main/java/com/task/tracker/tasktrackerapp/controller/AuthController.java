package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.dto.AuthRequest;
import com.task.tracker.tasktrackerapp.dto.AuthResponse;
import com.task.tracker.tasktrackerapp.service.CognitoAuthService;
import com.task.tracker.tasktrackerapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final CognitoAuthService cognitoAuthService;
    private final UserService userService;

    /**
     * Login endpoint - authenticate user with AWS Cognito
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        AuthResponse response = cognitoAuthService.authenticate(
            request.getUsername(),
            request.getPassword()
        );

        // After successful login, sync user to database if not already present
        if (response.getIdToken() != null) {
            try {
                // Extract user information from the ID token and sync to database
                cognitoAuthService.syncUserToDatabase(response.getIdToken(), userService);
                log.info("User {} synced to database after login", request.getUsername());
            } catch (Exception e) {
                log.error("Failed to sync user {} to database after login: {}", request.getUsername(), e.getMessage());
                // Continue with login even if database sync fails
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Register endpoint - create new user in AWS Cognito and database
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody AuthRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());

        try {
            // Register user in AWS Cognito
            cognitoAuthService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
            );
            log.info("User {} registered in Cognito successfully", request.getUsername());

            // Create user record in database
            try {
                userService.syncUserFromCognito(
                    request.getUsername(),
                    request.getEmail(),
                    null  // cognitoSub will be updated after first login
                );
                log.info("User {} saved to database successfully", request.getUsername());
            } catch (Exception dbException) {
                log.error("Failed to save user {} to database: {}", request.getUsername(), dbException.getMessage(), dbException);
                // User is registered in Cognito, but failed to save to database
                // We continue anyway and let them confirm and login
            }

            log.info("User {} registration process completed", request.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully. Please check your email for verification code.",
                "username", request.getUsername()
            ));
        } catch (Exception e) {
            log.error("Registration failed for user {}: {}", request.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Confirm registration - verify email with code
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmRegistration(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String confirmationCode = request.get("confirmationCode");

        log.info("Email confirmation for user: {}", username);

        cognitoAuthService.confirmUserRegistration(username, confirmationCode);

        return ResponseEntity.ok(Map.of(
            "message", "User confirmed successfully. You can now login.",
            "username", username
        ));
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        log.info("Token refresh request");

        AuthResponse response = cognitoAuthService.refreshToken(refreshToken);

        return ResponseEntity.ok(response);
    }

    /**
     * Assign role to user (replaces existing role) - Admin only
     * Removes all existing roles and assigns the new one
     */
    @PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> assignRole(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String role = request.get("role");

        log.info("Replacing role for user {} with {}", username, role);

        // Use replaceUserRole instead of addUserToGroup to ensure only one role
        cognitoAuthService.replaceUserRole(username, role);

        return ResponseEntity.ok(Map.of(
            "message", "Role assigned successfully",
            "username", username,
            "role", role
        ));
    }

    /**
     * Remove user from group (role removal) - Admin only
     */
    @PostMapping("/remove-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeRole(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String role = request.get("role");

        log.info("Removing role {} from user {}", role, username);

        cognitoAuthService.removeUserFromGroup(username, role);

        return ResponseEntity.ok(Map.of(
            "message", "Role removed successfully",
            "username", username,
            "role", role
        ));
    }

    /**
     * Forgot password endpoint - initiates password reset
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        log.info("Forgot password request for user: {}", username);

        try {
            cognitoAuthService.forgotPassword(username);

            return ResponseEntity.ok(Map.of(
                "message", "Password reset code sent to your email",
                "username", username
            ));
        } catch (Exception e) {
            log.error("Forgot password failed for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Forgot password failed: " + e.getMessage());
        }
    }

    /**
     * Confirm forgot password endpoint - verify code and set new password
     */
    @PostMapping("/confirm-forgot-password")
    public ResponseEntity<Map<String, String>> confirmForgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String confirmationCode = request.get("confirmationCode");
        String newPassword = request.get("newPassword");

        log.info("Confirm forgot password for user: {}", username);

        try {
            cognitoAuthService.confirmForgotPassword(username, confirmationCode, newPassword);

            return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully. You can now login with your new password.",
                "username", username
            ));
        } catch (Exception e) {
            log.error("Confirm forgot password failed for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Password reset failed: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Authentication Service"
        ));
    }
}

