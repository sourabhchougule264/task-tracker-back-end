package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.Utility.AuthUtility;
import com.task.tracker.tasktrackerapp.dto.UserDTO;
import com.task.tracker.tasktrackerapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "User Management", description = "APIs for managing user profiles and information")
public class UserController {

    private final UserService userService;

    /**
     * Create user profile (no password - managed by Cognito)
     * Typically called after Cognito registration
     */
    @Operation(summary = "Create user profile", description = "Create a new user profile in the system. This is typically called after Cognito registration. Admin only.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User profile created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Create User Example",
                                    value = """
                                            {
                                              "id": 101,
                                              "username": "jdoe_99",
                                              "email": "jane.doe@example.com",
                                              "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                              "firstName": "Jane",
                                              "lastName": "Doe",
                                              "isActive": true,
                                              "roles": [
                                                "TASK-CREATOR",
                                                "ADMIN"
                                              ]
                                            }""",
                                    summary = "Example of a successful user profile creation response"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access Denied: Admin role required to create user profiles.",
                                              "path": "/users"
                                            }""",
                                    summary = "Example of a forbidden response when a non-admin tries to create a user profile"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Bad Request Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed for object='userDTO'. Error count: 1",
                                              "path": "/users"
                                            }""",
                                    summary = "Example of a bad request response when the input data for creating a user profile is invalid (e.g., missing required fields or invalid email format)"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User profile data to create",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class),
                    examples = @ExampleObject(
                            name = "Create User Example",
                            value = """
                                    {
                                      "id": 101,
                                      "username": "jdoe_99",
                                      "email": "jane.doe@example.com",
                                      "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                      "firstName": "Jane",
                                      "lastName": "Doe",
                                      "isActive": true,
                                      "roles": [
                                        "TASK-CREATOR",
                                        "ADMIN"
                                      ]
                                    }""",
                            summary = "Example of a valid request body for creating a user profile. Note that the 'id' field is typically generated by the system and may not be required in the request body, but it's included here for completeness."
                    )
            )
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUserProfile(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.createUserProfile(userDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Get current authenticated user's profile
     */
    @Operation(summary = "Get current user profile", description = "Retrieve the profile information of the currently authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Current User Profile Example",
                                    value = """
                                            {
                                              "id": 101,
                                              "username": "jdoe_99",
                                              "email": "jane.doe@example.com",
                                              "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                              "firstName": "Jane",
                                              "lastName": "Doe",
                                              "isActive": true,
                                              "roles": [
                                                "TASK-CREATOR",
                                                "ADMIN"
                                              ]
                                            }""",
                                    summary = "Example of a successful response when retrieving the current authenticated user's profile information. The response includes the user's ID, username, email, Cognito sub, first and last name, active status, and roles."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource.",
                                              "path": "/users/me"
                                            }""",
                                    summary = "Example of an unauthorized response when trying to access the current user's profile without being authenticated. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User profile not found for the authenticated user.",
                                              "path": "/users/me"
                                            }""",
                                    summary = "Example of a not found response when the authenticated user's profile cannot be found in the system. This could occur if the user was authenticated but their profile was not properly created or has been deleted. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        String username = AuthUtility.getUsernameFromAuth(authentication);
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by ID
     */
    @Operation(summary = "Get user by ID", description = "Retrieve user profile information by user ID. Admins can access any user's profile, while regular users can only access their own profile.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Get User By ID Example",
                                    value = """
                                            {
                                              "id": 101,
                                              "username": "jdoe_99",
                                              "email": "jane.doe@example.com",
                                              "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                              "firstName": "Jane",
                                              "lastName": "Doe",
                                              "isActive": true,
                                              "roles": [
                                                "TASk-CREATOR",
                                                "ADMIN"
                                              ]
                                            }""",
                                    summary = "Example of a successful response when retrieving a user profile by ID. The response includes the user's ID, username, email, Cognito sub, first and last name, active status, and roles."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of an unauthorized response when trying to access a user profile by ID without being authenticated. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User profile not found with the provided ID.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of a not found response when a user profile cannot be found with the provided ID. This could occur if the ID does not exist in the system or if the user was deleted. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            )
    })
    @Parameters(value = {
            @io.swagger.v3.oas.annotations.Parameter(name = "id", description = "ID of the user to retrieve", required = true, example = "101")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username
     */
    @Operation(summary = "Get user by username", description = "Retrieve user profile information by username. Admins can access any user's profile, while regular users can only access their own profile.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Get User By Username Example",
                                    value = """
                                            {
                                              "id": 101,
                                              "username": "jdoe_99",
                                              "email": "jane.doe@example.com",
                                              "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                              "firstName": "Jane",
                                              "lastName": "Doe",
                                              "isActive": true,
                                              "roles": [
                                                "TASk-CREATOR",
                                                "ADMIN"
                                              ]
                                            }""",
                                    summary = "Example of a successful response when retrieving a user profile by username. The response includes the user's ID, username, email, Cognito sub, first and last name, active status, and roles."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource.",
                                              "path": "/users/username/{username}"
                                            }""",
                                    summary = "Example of an unauthorized response when trying to access a user profile by username without being authenticated. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User profile not found with the provided username.",
                                              "path": "/users/username/{username}"
                                            }""",
                                    summary = "Example of a not found response when a user profile cannot be found with the provided username. This could occur if the username does not exist in the system or if the user was deleted. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            )
    })
    @Parameters(value = {
            @io.swagger.v3.oas.annotations.Parameter(name = "username", description = "Username of the user to retrieve", required = true, example = "jdoe_99")
    })
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users
     */
    @Operation(summary = "Get all users", description = "Retrieve a list of all user profiles in the system. Admin only.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of user profiles retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Get All Users Example",
                                    value = """
                                            {
                                              "id": 101,
                                              "username": "jdoe_99",
                                              "email": "jane.doe@example.com",
                                              "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                              "firstName": "Jane",
                                              "lastName": "Doe",
                                              "isActive": true,
                                              "roles": [
                                                "TASk-CREATOR",
                                                "ADMIN"
                                              ]
                                            }""",
                                    summary = "Example of a successful response when retrieving a list of all user profiles. The response includes an array of user profiles, each with their ID, username, email, Cognito sub, first and last name, active status, and roles."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource.",
                                              "path": "/users"
                                            }""",
                                    summary = "Example of an unauthorized response when trying to access the list of all user profiles without being authenticated. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access Denied: Admin role required to view all user profiles.",
                                              "path": "/users"
                                            }""",
                                    summary = "Example of a forbidden response when a non-admin tries to access the list of all user profiles. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user profile
     */
    @Operation(summary = "Update user profile", description = "Update the profile information of a user. Admins can update any user's profile, while regular users can only update their own profile.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Update User Example",
                                    value = """
                                            {
                                              "id": 101,
                                              "username": "jdoe_99",
                                              "email": "jane.doe@example.com",
                                              "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                              "firstName": "Jane",
                                              "lastName": "Doe",
                                              "isActive": true,
                                              "roles": [
                                                "TASk-CREATOR",
                                                "ADMIN"
                                              ]
                                            }""",
                                    summary = "Example of a successful user profile update response"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of an unauthorized response when trying to update a user profile without being authenticated. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required or user can only update their own profile",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access Denied: You can only update your own profile unless you have the Admin role.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of a forbidden response when a user tries to update another user's profile without having the Admin role. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User profile not found with the provided ID.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of a not found response when trying to update a user profile that cannot be found with the provided ID. This could occur if the ID does not exist in the system or if the user was deleted. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            )
    })
    @Parameters(value = {
            @io.swagger.v3.oas.annotations.Parameter(name = "id", description = "ID of the user to update", required = true, example = "101")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated user profile data",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class),
                    examples = @ExampleObject(
                            name = "Update User Example",
                            value = """
                                    {
                                      "id": 101,
                                      "username": "jdoe_99",
                                      "email": "jane.doe@example.com",
                                      "cognitoSub": "us-east-1:a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                                      "firstName": "Jane",
                                      "lastName": "Doe",
                                      "isActive": true,
                                      "roles": [
                                        "ROLE_USER",
                                        "ROLE_ADMIN"
                                      ]
                                    }""",
                            summary = "Example of a valid request body for updating a user profile. The request body includes the user's ID, username, email, Cognito sub, first and last name, active status, and roles. Note that the 'id' field is typically included in the path variable and may not be required in the request body, but it's included here for completeness."
                    )
            )
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['sub']")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user
     * Admin only - Note: This only deletes from database and Cognito.
     */
    @Operation(summary = "Delete user", description = "Delete a user profile from the system. This is an admin-only operation. Note: This will delete the user from both the database and AWS Cognito.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Delete User Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 204,
                                              "message": "User deleted successfully.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of a successful response when deleting a user profile. The response includes a timestamp, status code, success message, and the path of the request. Note that the actual response body for a 204 No Content status would typically be empty, but this example includes a message for illustrative purposes."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of an unauthorized response when trying to delete a user profile without being authenticated. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access Denied: Admin role required to delete user profiles.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of a forbidden response when a non-admin tries to delete a user profile. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Not Found Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User profile not found with the provided ID.",
                                              "path": "/users/{id}"
                                            }""",
                                    summary = "Example of a not found response when trying to delete a user profile that cannot be found with the provided ID. This could occur if the ID does not exist in the system or if the user was already deleted. The response includes a timestamp, status code, error message, and the path of the request."
                            )
                    )
            )
    })
    @Parameters(value = {
            @io.swagger.v3.oas.annotations.Parameter(name = "id", description = "ID of the user to delete", required = true, example = "101")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}

