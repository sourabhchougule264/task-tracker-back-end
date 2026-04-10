package com.task.tracker.tasktrackerapp.controller;

import com.task.tracker.tasktrackerapp.Utility.AuthUtility;
import com.task.tracker.tasktrackerapp.dto.TaskDTO;
import com.task.tracker.tasktrackerapp.enums.TaskStatus;
import com.task.tracker.tasktrackerapp.service.TaskService;
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
@RequestMapping("/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Task Management API", description = "APIs for managing tasks in the Task Tracker application")
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task
     * Requires: ADMIN or TASK_CREATOR role
     */
    @Operation(summary = "Create a new task", description = "Create a new task under a project. Requires ADMIN or TASK_CREATOR role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Task created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Create Task Response Example",
                                    value = """
                                            {
                                              "id": 1,
                                              "title": "Design Database Schema",
                                              "description": "Design the database schema for the new project",
                                              "status": "TODO",
                                              "projectId": 1,
                                              "assignedUsernames": []
                                            }""",
                                    summary = "Example of a successful task creation response"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed for object='taskDTO'. Error count: 1",
                                              "path": "/tasks"
                                            }""",
                                    summary = "Example of a bad request response when input validation fails"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to create tasks",
                                              "path": "/tasks"
                                            }""",
                                    summary = "Example of a forbidden response when the user does not have the required role to create tasks"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Task data to create",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskDTO.class),
                    examples = @ExampleObject(
                            name = "Create Task Example",
                            value = """
                                    {
                                      "title": "Design Database Schema",
                                      "description": "Design the database schema for the new project",
                                      "status": "TODO",
                                      "projectId": 1
                                    }""",
                            summary = "Example of a request body to create a new task. The 'id' field is not included as it will be generated by the server."
                    )
            )
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATOR')")
    public ResponseEntity<TaskDTO> createTask(
            @RequestBody TaskDTO taskDTO,
            Authentication authentication) {

        String username = AuthUtility.getUsernameFromAuth(authentication);
        TaskDTO createdTask = taskService.createTask(taskDTO, username);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    /**
     * Get task by ID
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get task by ID", description = "Retrieve a task by its ID. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Task retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Get Task by ID Example",
                                    value = """
                                            {
                                              "id": 1,
                                              "title": "Design Database Schema",
                                              "description": "Design the database schema for the new project",
                                              "status": "TODO",
                                              "projectId": 1,
                                              "assignedUsernames": ["john_doe", "jane_smith"]
                                            }""",
                                    summary = "Example of a successful response when retrieving a task by its ID"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to retrieve a task by its ID"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task not found with the given ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Task not found with ID: 1",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of a not found response when there is no task with the specified ID"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "id", description = "ID of the task to retrieve", required = true, example = "1")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    /**
     * Get all tasks
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get all tasks", description = "Retrieve a list of all tasks. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Projects retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Get All Tasks Example",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "title": "Design Database Schema",
                                                "description": "Design the database schema for the new project",
                                                "status": "TODO",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe", "jane_smith"]
                                              },
                                              {
                                                "id": 2,
                                                "title": "Implement Authentication",
                                                "description": "Implement user authentication using JWT",
                                                "status": "IN_PROGRESS",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe"]
                                              }
                                            ]""",
                                    summary = "Example of a successful response when retrieving all tasks. The response is an array of task objects."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to retrieve all tasks"
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by project
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get tasks by project", description = "Retrieve a list of tasks under a specific project. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Get Tasks by Project Example",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "title": "Design Database Schema",
                                                "description": "Design the database schema for the new project",
                                                "status": "TODO",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe", "jane_smith"]
                                              },
                                              {
                                                "id": 2,
                                                "title": "Implement Authentication",
                                                "description": "Implement user authentication using JWT",
                                                "status": "IN_PROGRESS",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe"]
                                              }
                                            ]""",
                                    summary = "Example of a successful response when retrieving tasks by project ID. The response is an array of task objects that belong to the specified project."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/project/1"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to retrieve tasks by project ID"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Project not found with the given ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Project not found with ID: 1",
                                              "path": "/tasks/project/1"
                                            }""",
                                    summary = "Example of a not found response when there is no project with the specified ID while trying to retrieve tasks by project ID"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "projectId", description = "ID of the project to retrieve tasks for", required = true, example = "1")
    })
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(@PathVariable Long projectId) {
        List<TaskDTO> tasks = taskService.getTasksByProject(projectId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks assigned to a user
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get tasks assigned to a user", description = "Retrieve a list of tasks assigned to a specific user. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Get Tasks by Assigned User Example",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "title": "Design Database Schema",
                                                "description": "Design the database schema for the new project",
                                                "status": "TODO",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe", "jane_smith"]
                                              },
                                              {
                                                "id": 2,
                                                "title": "Implement Authentication",
                                                "description": "Implement user authentication using JWT",
                                                "status": "IN_PROGRESS",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe"]
                                              }
                                            ]""",
                                    summary = "Example of a successful response when retrieving tasks assigned to a specific user. The response is an array of task objects that are assigned to the specified user."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/assigned/john_doe"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to retrieve tasks assigned to a specific user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found with the given username",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found with username: john_doe",
                                              "path": "/tasks/assigned/john_doe"
                                            }""",
                                    summary = "Example of a not found response when there is no user with the specified username while trying to retrieve tasks assigned to a specific user"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "username", description = "Username of the user to retrieve assigned tasks for", required = true, example = "john_doe")})
    @GetMapping("/assigned/{username}")
    public ResponseEntity<List<TaskDTO>> getTasksByAssignedUser(@PathVariable String username) {
        List<TaskDTO> tasks = taskService.getTasksByAssignedUsername(username);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get current user's assigned tasks
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get current user's assigned tasks", description = "Retrieve a list of tasks assigned to the currently authenticated user. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Get My Tasks Example",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "title": "Design Database Schema",
                                                "description": "Design the database schema for the new project",
                                                "status": "TODO",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe", "jane_smith"]
                                              },
                                              {
                                                "id": 2,
                                                "title": "Implement Authentication",
                                                "description": "Implement user authentication using JWT",
                                                "status": "IN_PROGRESS",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe"]
                                              }
                                            ]""",
                                    summary = "Example of a successful response when retrieving tasks assigned to the currently authenticated user. The response is an array of task objects that are assigned to the authenticated user."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/my-tasks"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to retrieve tasks assigned to the currently authenticated user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "User not found with username: john_doe",
                                              "path": "/tasks/my-tasks"
                                            }""",
                                    summary = "Example of a not found response when the authenticated user does not exist in the system while trying to retrieve tasks assigned to the currently authenticated user"
                            )

                    )
            )
    })
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskDTO>> getMyTasks(Authentication authentication) {
        String username = AuthUtility.getUsernameFromAuth(authentication);
        List<TaskDTO> tasks = taskService.getTasksByAssignedUsername(username);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by status
     * Requires: Any authenticated user
     */
    @Operation(summary = "Get tasks by status", description = "Retrieve a list of tasks filtered by their status. Requires any authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Get Tasks by Status Example",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "title": "Design Database Schema",
                                                "description": "Design the database schema for the new project",
                                                "status": "TODO",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe", "jane_smith"]
                                              },
                                              {
                                                "id": 2,
                                                "title": "Implement Authentication",
                                                "description": "Implement user authentication using JWT",
                                                "status": "IN_PROGRESS",
                                                "projectId": 1,
                                                "assignedUsernames": ["john_doe"]
                                              }
                                            ]""",
                                    summary = "Example of a successful response when retrieving tasks by status. The response is an array of task objects that have the specified status."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/status/IN_PROGRESS"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to retrieve tasks by status"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid status value provided",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Status Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid status value: INVALID_STATUS. Allowed values are TODO, IN_PROGRESS, DONE.",
                                              "path": "/tasks/status/INVALID_STATUS"
                                            }""",
                                    summary = "Example of a bad request response when an invalid status value is provided while trying to retrieve tasks by status. The message indicates the allowed status values."
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "status", description = "Status to filter tasks by (e.g., NEW, IN_PROGRESS, BLOCKED, COMPLETED, NOT_STARTED)", required = true, example = "NEW")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskDTO>> getTasksByStatus(@PathVariable TaskStatus status) {
        List<TaskDTO> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Update task
     * Requires: ADMIN or task owner or assigned user
     */
    @Operation(summary = "Update a task", description = "Update the details of an existing task. Requires ADMIN role, or the user must be the task owner or assigned to the task.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Task updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Update Task Response Example",
                                    value = """
                                            {
                                              "id": 1,
                                              "title": "Design Database Schema - Updated",
                                              "description": "Design the database schema for the new project with additional tables",
                                              "status": "IN_PROGRESS",
                                              "projectId": 1,
                                              "assignedUsernames": ["john_doe", "jane_smith"]
                                            }""",
                                    summary = "Example of a successful response when updating a task. The response contains the updated task details."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed for object='taskDTO'. Error count: 1",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of a bad request response when the input data for updating a task is invalid. The message indicates that validation failed and may include details about the validation errors."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to update a task"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to update this task",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of a forbidden response when the authenticated user does not have sufficient permissions to update the task. The message indicates that the user does not have permission to perform the update operation on the specified task."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task not found with the given ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Task not found with ID: 1",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of a not found response when there is no task with the specified ID while trying to update a task"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "id", description = "ID of the task to update", required = true, example = "1")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated task data",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskDTO.class),
                    examples = @ExampleObject(
                            name = "Update Task Example",
                            value = """
                                    {
                                      "title": "Design Database Schema - Updated",
                                      "description": "Design the database schema for the new project with additional tables",
                                      "status": "IN_PROGRESS",
                                      "projectId": 1
                                    }""",
                            summary = "Example of the request body for updating a task. The JSON object contains the fields that can be updated for the task, such as title, description, status, and projectId."
                    )
            )
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name) or @authorizationService.isTaskAssignee(#id, authentication.name)")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO taskDTO,
            Authentication authentication) {

        TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Assign task to a user
     * Requires: ADMIN or TASK_CREATOR role
     */
    @Operation(summary = "Assign a task to a user", description = "Assign an existing task to a user. Requires ADMIN or TASK_CREATOR role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Task assigned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Assign Task Response Example",
                                    value = """
                                            {
                                              "id": 1,
                                              "title": "Design Database Schema",
                                              "description": "Design the database schema for the new project",
                                              "status": "TODO",
                                              "projectId": 1,
                                              "assignedUsernames": ["john_doe", "jane_smith"]
                                            }""",
                                    summary = "Example of a successful response when assigning a task to a user. The response contains the updated task details, including the list of assigned usernames which now includes the newly assigned user."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid task ID or username",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid task ID: 1 or username: john_doe",
                                              "path": "/tasks/1/assign/john_doe"
                                            }""",
                                    summary = "Example of a bad request response when the task ID or username provided for assigning a task is invalid. The message indicates that either the task ID does not exist or the username is not valid."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/1/assign/john_doe"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to assign a task to a user"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to assign this task",
                                              "path": "/tasks/1/assign/john_doe"
                                            }""",
                                    summary = "Example of a forbidden response when the authenticated user does not have sufficient permissions to assign the task. The message indicates that the user does not have permission to perform the assign operation on the specified task."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task or user not found with the given IDs",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Task not found with ID: 1 or user not found with username: john_doe",
                                              "path": "/tasks/1/assign/john_doe"
                                            }""",
                                    summary = "Example of a not found response when there is no task with the specified ID or no user with the specified username while trying to assign a task to a user"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "taskId", description = "ID of the task to assign", required = true, example = "1"),
            @io.swagger.v3.oas.annotations.Parameter(name = "username", description = "Username of the user to assign the task to", required = true, example = "john_doe")
    })
    @PatchMapping("/{taskId}/assign/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TASK_CREATOR')")
    public ResponseEntity<TaskDTO> assignTaskToUser(
            @PathVariable Long taskId,
            @PathVariable String username,
            Authentication authentication) {

        TaskDTO updatedTask = taskService.assignTaskToUserByUsername(taskId, username);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Update task status
     * READ_ONLY: Can only mark assigned tasks as complete
     * TASK_CREATOR: Can update status of owned or assigned tasks
     * ADMIN: Can update any task status
     */
    @Operation(summary = "Update task status", description = "Update the status of a task. READ_ONLY users can only mark assigned tasks as complete, TASK_CREATOR can update status of owned or assigned tasks, and ADMIN can update any task status.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Task status updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Update Task Status Response Example",
                                    value = """
                                            {
                                              "id": 1,
                                              "title": "Design Database Schema",
                                              "description": "Design the database schema for the new project",
                                              "status": "IN_PROGRESS",
                                              "projectId": 1,
                                              "assignedUsernames": ["john_doe", "jane_smith"]
                                            }""",
                                    summary = "Example of a successful response when updating a task's status. The response contains the updated task details, including the new status value."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid task ID or status value",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid task ID: 1 or status value: INVALID_STATUS. Allowed values are TODO, IN_PROGRESS, DONE.",
                                              "path": "/tasks/1/status/INVALID_STATUS"
                                            }""",
                                    summary = "Example of a bad request response when the task ID or status value provided for updating a task's status is invalid. The message indicates that either the task ID does not exist or the status value is not valid, along with the allowed status values."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/1/status/IN_PROGRESS"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to update a task's status"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Forbidden Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "You do not have permission to update the status of this task",
                                              "path": "/tasks/1/status/IN_PROGRESS"
                                            }""",
                                    summary = "Example of a forbidden response when the authenticated user does not have sufficient permissions to update the task's status. The message indicates that the user does not have permission to perform the status update operation on the specified task."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task not found with the given ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Task not found with ID: 1",
                                              "path": "/tasks/1/status/IN_PROGRESS"
                                            }""",
                                    summary = "Example of a not found response when there is no task with the specified ID while trying to update a task's status"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "taskId", description = "ID of the task to update status for", required = true, example = "1"),
            @io.swagger.v3.oas.annotations.Parameter(name = "status", description = "New status for the task (e.g., TODO, IN_PROGRESS, DONE)", required = true, example = "DONE")
    })
    @PatchMapping("/{taskId}/status/{status}")
    public ResponseEntity<TaskDTO> updateTaskStatus(
            @PathVariable Long taskId,
            @PathVariable TaskStatus status,
            Authentication authentication) {

        String username = AuthUtility.getUsernameFromAuth(authentication);
        TaskDTO updatedTask = taskService.updateTaskStatus(taskId, status, authentication);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Delete task
     * Requires: ADMIN or task owner (TASK_CREATOR)
     */
    @Operation(summary = "Delete a task", description = "Delete an existing task. Requires ADMIN role or the user must be the task owner (TASK_CREATOR).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Task deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Delete Task Response Example",
                                    value = "",
                                    summary = "Example of a successful response when deleting a task. The response has no content and indicates that the task was deleted successfully."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid task ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid Input Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid task ID: 1",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of a bad request response when the task ID provided for deleting a task is invalid. The message indicates that the task ID does not exist or is not valid."
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorised - authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorised Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication required to access this resource",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of an unauthorized response when authentication is missing or invalid while trying to delete a task"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions"
                    , content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Forbidden Response Example",
                            value = """
                                    {
                                      "timestamp": "2024-06-01T12:00:00Z",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "You do not have permission to delete this task",
                                      "path": "/tasks/1"
                                    }""",
                            summary = "Example of a forbidden response when the authenticated user does not have sufficient permissions to delete the task. The message indicates that the user does not have permission to perform the delete operation on the specified task."
                    )
            )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Task not found with the given ID",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Response Example",
                                    value = """
                                            {
                                              "timestamp": "2024-06-01T12:00:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Task not found with ID: 1",
                                              "path": "/tasks/1"
                                            }""",
                                    summary = "Example of a not found response when there is no task with the specified ID while trying to delete a task"
                            )
                    )
            )
    })
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "id", description = "ID of the task to delete", required = true, example = "1")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

}
