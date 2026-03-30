package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.Utility.AuthUtility;
import com.task.tracker.tasktrackerapp.dto.ProjectDTO;
import com.task.tracker.tasktrackerapp.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project
     * Requires: ADMIN or TASK_CREATOR role
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATOR')")
    public ResponseEntity<ProjectDTO> createProject(
            @RequestBody ProjectDTO projectDTO,
            Authentication authentication) {

        // Extract username from JWT token
        String username = AuthUtility.getUsernameFromAuth(authentication);

        ProjectDTO createdProject = projectService.createProject(projectDTO, username);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    /**
     * Get project by ID
     * Requires: Any authenticated user
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Get all projects
     * Requires: Any authenticated user
     */
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get projects by owner username
     * Requires: Any authenticated user
     */
    @GetMapping("/owner/{username}")
    public ResponseEntity<List<ProjectDTO>> getProjectsByOwner(@PathVariable String username) {
        List<ProjectDTO> projects = projectService.getProjectsByOwnerUsername(username);
        return ResponseEntity.ok(projects);
    }

    /**
     * Update project
     * Requires: ADMIN or project owner (TASK_CREATOR)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isProjectOwner(#id, authentication.name)")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDTO projectDTO,
            Authentication authentication) {

        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Delete project
     * Requires: ADMIN or project owner (TASK_CREATOR)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isProjectOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user's projects
     * Requires: Any authenticated user
     */
    @GetMapping("/my-projects")
    public ResponseEntity<List<ProjectDTO>> getMyProjects(Authentication authentication) {
        String username = AuthUtility.getUsernameFromAuth(authentication);
        List<ProjectDTO> projects = projectService.getProjectsByOwnerUsername(username);
        return ResponseEntity.ok(projects);
    }

}
