package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.Utility.AuthUtility;
import com.task.tracker.tasktrackerapp.dto.AuthRequest;
import com.task.tracker.tasktrackerapp.dto.ProjectDTO;
import com.task.tracker.tasktrackerapp.service.ProjectService;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Project Management", description = "APIs for managing projects in the Task Tracker application")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project
     * Requires: ADMIN or TASK_CREATOR role
     */
    @Operation(summary = "Create a new project", description = "Creates a new project. Requires ADMIN or TASK_CREATOR role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Project created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDTO.class),
                            examples = @ExampleObject(
                                    name = "Created Project Response",
                                    value = """
                                            {
                                              "id": 12,
                                              "name": "Project Alpha",
                                              "description": "This is a sample project for demonstration purposes.",
                                              "startDate": "2024-01-01",
                                              "endDate": "2024-12-31",
                                              "ownerId": 5
                                            }""",
                                    summary = "Example of a standard response for a created project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Project name is required",
                                              "path": "/projects"
                                            }""",
                                    summary = "Example of a standard response for invalid input data"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to perform this action",
                                              "path": "/projects"
                                            }""",
                                    summary = "Example of a standard response for forbidden access"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Project details for project creation",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProjectDTO.class),
                    examples = @ExampleObject(
                            name = "Standard Project Details",
                            value = """
                                    {
                                      "name": "Project Alpha",
                                      "description": "This is a sample project for demonstration purposes.",
                                      "startDate": "2024-01-01",
                                      "endDate": "2024-12-31"
                                    }""",
                            summary = "Example of a standard Project Details for creation"
                    )
            )
    )
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
    @Operation(summary = "Get project by ID", description = "Retrieves a project by its ID. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Project created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDTO.class),
                            examples = @ExampleObject(
                                    name = "Get Project Response",
                                    value = """
                                            {
                                              "id": 12,
                                              "name": "Project Alpha",
                                              "description": "This is a sample project for demonstration purposes.",
                                              "startDate": "2024-01-01",
                                              "endDate": "2024-12-31",
                                              "ownerId": 5
                                            }""",
                                    summary = "Example of a standard response for retrieving a project by ID"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid project ID format",
                                              "path": "/projects/abc"
                                            }""",
                                    summary = "Example of a standard response for invalid input data when retrieving a project by ID"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access when retrieving a project by ID"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Project not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Project Not Found Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Project not found with id: 12",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for project not found when retrieving a project by ID"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(
                    name = "project id",
                    description = "ID of the project to retrieve",
                    required = true,
                    example = "12")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Get all projects
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get all projects", description = "Retrieves a list of all projects. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Projects retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDTO.class),
                            examples = @ExampleObject(
                                    name = "Get All Projects Response",
                                    value = """
                                            [
                                              {
                                                "id": 12,
                                                "name": "Project Alpha",
                                                "description": "This is a sample project for demonstration purposes.",
                                                "startDate": "2024-01-01",
                                                "endDate": "2024-12-31",
                                                "ownerId": 5
                                              },
                                              {
                                                "id": 13,
                                                "name": "Project Beta",
                                                "description": "This is another sample project for demonstration purposes.",
                                                "startDate": "2024-02-01",
                                                "endDate": "2024-11-30",
                                                "ownerId": 6
                                              }
                                            ]""",
                                    summary = "Example of a standard response for retrieving all projects"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access when retrieving all projects"
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get projects by owner username
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get projects by owner username", description = "Retrieves a list of projects owned by a specific user. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Projects retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDTO.class),
                            examples = @ExampleObject(
                                    name = "Get Projects by Owner Response",
                                    value = """
                                            [
                                              {
                                                "id": 12,
                                                "name": "Project Alpha",
                                                "description": "This is a sample project for demonstration purposes.",
                                                "startDate": "2024-01-01",
                                                "endDate": "2024-12-31",
                                                "ownerId": 5
                                              },
                                              {
                                                "id": 14,
                                                "name": "Project Gamma",
                                                "description": "This is a sample project owned by the same user.",
                                                "startDate": "2024-03-01",
                                                "endDate": "2024-10-31",
                                                "ownerId": 5
                                              }
                                            ]""",
                                    summary = "Example of a standard response for retrieving projects by owner username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects/owner/Sourdough"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access when retrieving projects by owner username"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User or project not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User or Project Not Found Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found with username: Sourdough",
                                              "path": "/projects/owner/Sourdough"
                                            }""",
                                    summary = "Example of a standard response for user or project not found when retrieving projects by owner username"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(
                    name = "User Name",
                    description = "Username of user whose projects are to be retrieved",
                    required = true,
                    example = "Sourdough")
    })
    @GetMapping("/owner/{username}")
    public ResponseEntity<List<ProjectDTO>> getProjectsByOwner(@PathVariable String username) {
        List<ProjectDTO> projects = projectService.getProjectsByOwnerUsername(username);
        return ResponseEntity.ok(projects);
    }

    /**
     * Update project
     * Requires: ADMIN or project owner (TASK_CREATOR)
     */
    @Operation(summary = "Update project", description = "Updates an existing project. Requires ADMIN or project owner (TASK_CREATOR).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Project updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDTO.class),
                            examples = @ExampleObject(
                                    name = "Updated Project Response",
                                    value = """
                                            {
                                              "id": 12,
                                              "name": "Updated Project Alpha",
                                              "description": "This is an updated description for the project.",
                                              "startDate": "2024-01-01",
                                              "endDate": "2024-12-31",
                                              "ownerId": 5
                                            }""",
                                    summary = "Example of a standard response for an updated project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Project name is required",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for invalid input data when updating a project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access when updating a project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to perform this action",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for forbidden access when updating a project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Project not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Project Not Found Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Project not found with id: 12",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for project not found when updating a project"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Project details for project update",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProjectDTO.class),
                    examples = @ExampleObject(
                            name = "Standard Project Update Details",
                            value = """
                                    {
                                      "name": "Updated Project Alpha",
                                      "description": "This is an updated description for the project.",
                                      "startDate": "2024-01-01",
                                      "endDate": "2024-12-31"
                                    }""",
                            summary = "Example of a standard Project Details for update"
                    )
            )
    )
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(
                    name = "Project Id",
                    description = "ID of the project to update",
                    required = true,
                    example = "12")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isProjectOwner(#id, authentication.name)")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDTO projectDTO) {

        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Delete project
     * Requires: ADMIN or project owner (TASK_CREATOR)
     */
    @Operation(summary = "Delete project", description = "Deletes an existing project. Requires ADMIN or project owner (TASK_CREATOR).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Project deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Project Deleted Response",
                                    value = "",
                                    summary = "Example of a standard response for a successfully deleted project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access when deleting a project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to perform this action",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for forbidden access when deleting a project"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Project not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Project Not Found Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Project not found with id: 12",
                                              "path": "/projects/12"
                                            }""",
                                    summary = "Example of a standard response for project not found when deleting a project"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(
                    name = "Project Id",
                    description = "ID of the project to delete",
                    required = true,
                    example = "12")
    })
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
    @Operation(summary = "Get current user's projects", description = "Retrieves a list of projects owned by the currently authenticated user. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Projects retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDTO.class),
                            examples = @ExampleObject(
                                    name = "Get My Projects Response",
                                    value = """
                                            [
                                              {
                                                "id": 12,
                                                "name": "Project Alpha",
                                                "description": "This is a sample project for demonstration purposes.",
                                                "startDate": "2024-01-01",
                                                "endDate": "2024-12-31",
                                                "ownerId": 5
                                              },
                                              {
                                                "id": 14,
                                                "name": "Project Gamma",
                                                "description": "This is a sample project owned by the same user.",
                                                "startDate": "2024-03-01",
                                                "endDate": "2024-10-31",
                                                "ownerId": 5
                                              }
                                            ]""",
                                    summary = "Example of a standard response for retrieving current user's projects"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required to access this resource",
                                              "path": "/projects/my-projects"
                                            }""",
                                    summary = "Example of a standard response for unauthorized access when retrieving current user's projects"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User or project not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User or Project Not Found Response",
                                    value = """
                                            {
                                              "timestamp": "2024-01-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found with username: Sourdough",
                                              "path": "/projects/my-projects"
                                            }""",
                                    summary = "Example of a standard response for user or project not found when retrieving current user's projects"
                            )
                    )
            )
    })
    @GetMapping("/my-projects")
    public ResponseEntity<List<ProjectDTO>> getMyProjects(Authentication authentication) {
        String username = AuthUtility.getUsernameFromAuth(authentication);
        List<ProjectDTO> projects = projectService.getProjectsByOwnerUsername(username);
        return ResponseEntity.ok(projects);
    }

}
