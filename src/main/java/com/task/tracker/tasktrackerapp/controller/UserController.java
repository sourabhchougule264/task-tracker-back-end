package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.Utility.AuthUtility;
import com.task.tracker.tasktrackerapp.dto.UserDTO;
import com.task.tracker.tasktrackerapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * Create user profile (no password - managed by Cognito)
     * Typically called after Cognito registration
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUserProfile(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.createUserProfile(userDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Get current authenticated user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        String username = AuthUtility.getUsernameFromAuth(authentication);
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user profile
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['sub']")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user
     * Admin only - Note: This only deletes from database, Cognito user still exists
     * To fully delete, use AuthController to delete from Cognito as well
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}

