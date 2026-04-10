package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.dto.AuthRequest;
import com.task.tracker.tasktrackerapp.dto.AuthResponse;
import com.task.tracker.tasktrackerapp.service.CognitoAuthService;
import com.task.tracker.tasktrackerapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.examples.Example;
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
@Tag(name = "Authentication Controller", description = "Endpoints for user authentication, registration, and role management")
public class AuthController {

    private final CognitoAuthService cognitoAuthService;
    private final UserService userService;

    /**
     * Login endpoint - authenticate user with AWS Cognito
     */
    @Operation(summary = "User login", description = "Authenticate user with AWS Cognito and return tokens")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successful login",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    name = "Standard Login Response",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "idToken": "eyJraWQiOiJmS3F6S1R...",
                                              "refreshToken": "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIi...",
                                              "expiresIn": 3600,
                                              "tokenType": "Bearer",
                                              "challengeName": "NEW_PASSWORD_REQUIRED",
                                              "session": "AYABePz6p3L9m8X7c6...",
                                              "message": "Authentication successful. Please complete the required challenge."
                                            }""",
                                    summary = "Example of a standard login response with tokens and challenge information for a user that needs to complete a NEW_PASSWORD_REQUIRED challenge"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Login Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid username or password",
                                              "path": "/auth/login"
                                            }""",
                                    summary = "Example of an invalid login request"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Login Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication failed. Please check your credentials.",
                                              "path": "/auth/login"
                                            }""",
                                    summary = "Example of an unauthorized login attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Login Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check your username.",
                                              "path": "/auth/login"
                                            }""",
                                    summary = "Example of a login attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Login",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during login. Please try again later.",
                                              "path": "/auth/login"
                                            }""",
                                    summary = "Example of an internal server error during login"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User details for log in",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthRequest.class),
                    examples = @ExampleObject(
                            name = "Standard Login Details",
                            value = """
                                    {"username: "john.doe",
                                      "password: "P@ssw0rd"
                                     }
                                    """,
                            summary = "Example of a standard login request with username and password"
                    )
            )
    )
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
    @Operation(summary = "User Registration", description = "Register user with AWS Cognito and create user profile in database")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successful Registration",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Registration Response",
                                    value = """
                                            {
                                            "message": "User registered successfully. Please check your email for verification code.",
                                             "username": "john.doe"
                                            }
                                            """,
                                    summary = "Example of a standard registration response with success message and username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Registration Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Username already exists. Please choose a different username.",
                                              "path": "/auth/register"
                                            }""",
                                    summary = "Example of an invalid registration request due to existing username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Registration Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Registration failed. Please check your credentials and try again.",
                                              "path": "/auth/register"
                                            }""",
                                    summary = "Example of an unauthorized registration attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Registration",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during registration. Please try again later.",
                                              "path": "/auth/register"
                                            }""",
                                    summary = "Example of an internal server error during registration"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User details for Sign Up",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthRequest.class),
                    examples = @ExampleObject(
                            name = "Standard Sign Up Details",
                            value = """
                                    {
                                      "username": "john.doe",
                                      "password": "P@ssw0rd",
                                      "email": "john.doe@gmail.com"
                                    }""",
                            summary = "Example of a standard sign-up request with username, password and email"
                    )
            )
    )
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
    @Operation(summary = "Confirm User Registration", description = "Confirm user registration by verifying email with code sent to user's email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User confirmed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Confirmation Response",
                                    value = """
                                            {
                                              "message": "User confirmed successfully. You can now login.",
                                              "username": "john.doe"
                                            }""",
                                    summary = "Example of a standard confirmation response with success message and username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Confirmation Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid confirmation code. Please check the code and try again.",
                                              "path": "/auth/confirm"
                                            }""",
                                    summary = "Example of an invalid confirmation request due to incorrect code"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Confirmation Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Confirmation failed. Please check your credentials and try again.",
                                              "path": "/auth/confirm"
                                            }""",
                                    summary = "Example of an unauthorized confirmation attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Confirmation Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check your username.",
                                              "path": "/auth/confirm"
                                            }""",
                                    summary = "Example of a confirmation attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Confirmation",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during confirmation. Please try again later.",
                                              "path": "/auth/confirm"
                                            }""",
                                    summary = "Example of an internal server error during confirmation"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User details for confirming registration",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Standard Confirmation Details",
                            value = "{\n  \"username\": \"john.doe\",\n  \"confirmationCode\": \"123456\"\n}",
                            summary = "Example of a standard confirmation request with username and confirmation code"
                    )
            )
    )
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
    @Operation(summary = "Refresh Authentication Token", description = "Refresh authentication token using a valid refresh token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    name = "Standard Refresh Token Response",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "idToken": "eyJraWQiOiJmS3F6S1R...",
                                              "refreshToken": "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIi...",
                                              "expiresIn": 3600,
                                              "tokenType": "Bearer",
                                              "message": "Token refreshed successfully."
                                            }""",
                                    summary = "Example of a standard refresh token response with new tokens and success message"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Refresh Token Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid refresh token. Please provide a valid refresh token.",
                                              "path": "/auth/refresh"
                                            }""",
                                    summary = "Example of an invalid refresh token request due to incorrect or expired token"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Refresh Token Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Token refresh failed. Please check your credentials and try again.",
                                              "path": "/auth/refresh"
                                            }""",
                                    summary = "Example of an unauthorized refresh token attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Refresh Token Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check your username.",
                                              "path": "/auth/refresh"
                                            }""",
                                    summary = "Example of a refresh token attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Token Refresh",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during token refresh. Please try again later.",
                                              "path": "/auth/refresh"
                                            }""",
                                    summary = "Example of an internal server error during token refresh"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token details",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Standard Refresh Token Request",
                            value = "{\n  \"refreshToken\": \"eyJraWQiOiJLTjVhZ3l5c1l6aG9iVjZtZ0lXc2p3bG9nV2N1c2h5a3lqTjJmM0xqQjA4eG9oYzVhIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJqb2huLmRvZSIsInRva2VuX3VzZSI6InJlZnJlc2giLCJhdXRoX3RpbWUiOjE2ODg4ODg4ODksImlzcyI6Imh0dHBzOi8vc2lnbkluZm8uYXdzLWFtYXpvbmF3cy5jb20vY29nbml0bzJhcHAiLCJleHAiOjE2ODg4ODkyODksImlhdCI6MTY4ODg4ODg4OSwianRpIjoiMjM0NTY3LTg5MGEtNDJhYi04NzY1LTU0MzIxMGFiYzEyMyIsInVzZXJuYW1lIjoiam9obi5kb2UifQ.abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890",
                            summary = "Example of a standard refresh token request with a valid refresh token"
                    )
            )
    )
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
    @Operation(summary = "Assign Role to User", description = "Assign a role to a user. This will replace any existing roles the user has. Admin only.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Role assigned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Role Assignment Response",
                                    value = """
                                            {
                                              "message": "Role assigned successfully",
                                              "username": "john.doe",
                                              "role": "TASK_CREATOR"
                                            }""",
                                    summary = "Example of a standard role assignment response with success message, username and assigned role"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Role Assignment Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid role specified. Please provide a valid role.",
                                              "path": "/auth/assign-role"
                                            }""",
                                    summary = "Example of an invalid role assignment request due to incorrect role"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Role Assignment Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Role assignment failed. You do not have permission to assign roles.",
                                              "path": "/auth/assign-role"
                                            }""",
                                    summary = "Example of an unauthorized role assignment attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Role Assignment Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check the username.",
                                              "path": "/auth/assign-role"
                                            }""",
                                    summary = "Example of a role assignment attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Role Assignment",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during role assignment. Please try again later.",
                                              "path": "/auth/assign-role"
                                            }""",
                                    summary = "Example of an internal server error during role assignment"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Details for assigning role to user",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Standard Role Assignment Request",
                            value = """
                                    {
                                      "username": "john.doe",
                                      "role": "TASK_CREATOR"
                                    }""",
                            summary = "Example of a standard role assignment request with username and role to assign"
                    )
            )
    )
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
    @Operation(summary = "Remove Role from User", description = "Remove a role from a user. Admin only.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Role removed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Role Removal Response",
                                    value = """
                                            {
                                              "message": "Role removed successfully",
                                              "username": "john.doe",
                                              "role": "TASK_CREATOR"
                                            }""",
                                    summary = "Example of a standard role removal response with success message, username and removed role"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Role Removal Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid role specified. Please provide a valid role.",
                                              "path": "/auth/remove-role"
                                            }""",
                                    summary = "Example of an invalid role removal request due to incorrect role"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Role Removal Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Role removal failed. You do not have permission to remove roles.",
                                              "path": "/auth/remove-role"
                                            }""",
                                    summary = "Example of an unauthorized role removal attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Role Removal Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check the username.",
                                              "path": "/auth/remove-role"
                                            }""",
                                    summary = "Example of a role removal attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Role Removal",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during role removal. Please try again later.",
                                              "path": "/auth/remove-role"
                                            }""",
                                    summary = "Example of an internal server error during role removal"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Details for removing role from user",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Standard Role Removal Request",
                            value = "{\n  \"username\": \"john.doe\",\n  \"role\": \"TASK_CREATOR\"\n}",
                            summary = "Example of a standard role removal request with username and role to remove"
                    )
            )
    )
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
    @Operation(summary = "Forgot Password", description = "Initiate password reset process by sending a reset code to the user's email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset initiated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Forgot Password Response",
                                    value = """
                                            {
                                              "message": "Password reset code sent to your email",
                                              "username": "john.doe"
                                            }""",
                                    summary = "Example of a standard forgot password response with success message and username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Forgot Password Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid request. Please provide a valid username.",
                                              "path": "/auth/forgot-password"
                                            }""",
                                    summary = "Example of an invalid forgot password request due to missing or incorrect username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Forgot Password Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Forgot password failed. Please check your credentials and try again.",
                                              "path": "/auth/forgot-password"
                                            }""",
                                    summary = "Example of an unauthorized forgot password attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Forgot Password Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check your username.",
                                              "path": "/auth/forgot-password"
                                            }""",
                                    summary = "Example of a forgot password attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Forgot Password",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during forgot password process. Please try again later.",
                                              "path": "/auth/forgot-password"
                                            }""",
                                    summary = "Example of an internal server error during forgot password process"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Details for initiating forgot password process",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Standard Forgot Password Request",
                            value = "{\n  \"username\": \"john.doe\"\n}",
                            summary = "Example of a standard forgot password request with username"
                    )
            )
    )
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
    @Operation(summary = "Confirm Forgot Password", description = "Confirm password reset by verifying the code sent to user's email and setting a new password")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Confirm Forgot Password Response",
                                    value = """
                                            {
                                              "message": "Password reset successfully. You can now login with your new password.",
                                              "username": "john.doe"
                                            }""",
                                    summary = "Example of a standard confirm forgot password response with success message and username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Confirm Forgot Password Request",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid request. Please provide valid username, confirmation code and new password.",
                                              "path": "/auth/confirm-forgot-password"
                                            }""",
                                    summary = "Example of an invalid confirm forgot password request due to missing or incorrect details"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Confirm Forgot Password Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Password reset failed. Please check your credentials and try again.",
                                              "path": "/auth/confirm-forgot-password"
                                            }""",
                                    summary = "Example of an unauthorized confirm forgot password attempt"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User Not Found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Confirm Forgot Password Attempt",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found. Please check your username.",
                                              "path": "/auth/confirm-forgot-password"
                                            }""",
                                    summary = "Example of a confirm forgot password attempt with a non-existent user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Internal Server Error During Confirm Forgot Password",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred during password reset process. Please try again later.",
                                              "path": "/auth/confirm-forgot-password"
                                            }""",
                                    summary = "Example of an internal server error during confirm forgot password process"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Details for confirming forgot password process",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Standard Confirm Forgot Password Request",
                            value = "{\n  \"username\": \"john.doe\",\n  \"confirmationCode\": \"123456\",\n  \"newPassword\": \"NewP@ssw0rd\"\n}",
                            summary = "Example of a standard confirm forgot password request with username, confirmation code and new password"
                    )
            )
    )
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
    @Operation(summary = "Health Check", description = "Check the health status of the Authentication Service")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Health Checked successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Standard Health Check Response",
                                    value = """
                                            {
                                              "status": "UP",
                                              "service": "Task-Tracker Service"
                                            }""",
                                    summary = "Example of a standard health check response indicating the service is up"
                            )
                    )
            ),
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Authentication Service"
        ));
    }
}

