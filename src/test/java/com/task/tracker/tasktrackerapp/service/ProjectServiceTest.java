package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.ProjectDTO;
import com.task.tracker.tasktrackerapp.entity.Project;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.repository.ProjectRepository;
import com.task.tracker.tasktrackerapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User mockOwner;
    private Project mockProject;
    private ProjectDTO projectDTO;

    @BeforeEach
    void setUp() {
        mockOwner = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        mockProject = Project.builder()
                .id(100L)
                .name("Test Project")
                .description("Sample Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .owner(mockOwner)
                .build();

        projectDTO = ProjectDTO.builder()
                .id(100L)
                .name("Test Project")
                .description("Sample Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .ownerId(1L)
                .build();
    }

    @Nested
    @DisplayName("Create Project Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project successfully using Owner ID")
        void createProject_ById_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockOwner));
            when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

            ProjectDTO result = projectService.createProject(projectDTO);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(projectDTO.getName());
            verify(projectRepository, times(1)).save(any(Project.class));
        }

        @Test
        @DisplayName("Should create project successfully using Username")
        void createProject_ByUsername_Success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockOwner));
            when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

            ProjectDTO result = projectService.createProject(projectDTO, "testuser");

            assertThat(result.getOwnerUsername()).isEqualTo("testuser");
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should throw exception when owner is not found")
        void createProject_OwnerNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.createProject(projectDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Owner not found");
        }
    }

    @Nested
    @DisplayName("Retrieve Project Tests")
    class GetProjectTests {

        @Test
        @DisplayName("Should return project when valid ID is provided")
        void getProjectById_Success() {
            when(projectRepository.findById(100L)).thenReturn(Optional.of(mockProject));

            ProjectDTO result = projectService.getProjectById(100L);

            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should throw exception when project ID doesn't exist")
        void getProjectById_NotFound() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found");
        }

        @Test
        @DisplayName("Should return all projects")
        void getAllProjects_Success() {
            when(projectRepository.findAll()).thenReturn(List.of(mockProject));

            List<ProjectDTO> results = projectService.getAllProjects();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Test Project");
        }
    }

    @Nested
    @DisplayName("Update Project Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project details successfully")
        void updateProject_Success() {
            ProjectDTO updateInfo = ProjectDTO.builder()
                    .name("Updated Name")
                    .description("Updated Desc")
                    .ownerId(1L)
                    .build();

            when(projectRepository.findById(100L)).thenReturn(Optional.of(mockProject));
            when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

            ProjectDTO result = projectService.updateProject(100L, updateInfo);

            assertThat(result.getName()).isEqualTo("Updated Name");
            verify(projectRepository).save(mockProject);
        }

        @Test
        @DisplayName("Should update owner if a different owner ID is provided")
        void updateProject_ChangeOwner() {
            User newOwner = User.builder().id(2L).username("newuser").build();
            ProjectDTO updateInfo = ProjectDTO.builder().ownerId(2L).name("New").build();

            when(projectRepository.findById(100L)).thenReturn(Optional.of(mockProject));
            when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
            when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

            projectService.updateProject(100L, updateInfo);

            verify(userRepository).findById(2L);
            assertThat(mockProject.getOwner()).isEqualTo(newOwner);
        }
    }

    @Nested
    @DisplayName("Delete Project Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project when it exists")
        void deleteProject_Success() {
            when(projectRepository.existsById(100L)).thenReturn(true);

            projectService.deleteProject(100L);

            verify(projectRepository, times(1)).deleteById(100L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent project")
        void deleteProject_NotFound() {
            when(projectRepository.existsById(100L)).thenReturn(false);

            assertThatThrownBy(() -> projectService.deleteProject(100L))
                    .isInstanceOf(RuntimeException.class);

            verify(projectRepository, never()).deleteById(anyLong());
        }
    }
}
