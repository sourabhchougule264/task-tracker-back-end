package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.ProjectDTO;
import com.task.tracker.tasktrackerapp.entity.Project;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.repository.ProjectRepository;
import com.task.tracker.tasktrackerapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        User owner = userRepository.findById(projectDTO.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found with id: " + projectDTO.getOwnerId()));

        Project project = Project.builder()
                .name(projectDTO.getName())
                .description(projectDTO.getDescription())
                .startDate(projectDTO.getStartDate())
                .endDate(projectDTO.getEndDate())
                .owner(owner)
                .build();

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner not found with username: " + username));

        Project project = Project.builder()
                .name(projectDTO.getName())
                .description(projectDTO.getDescription())
                .startDate(projectDTO.getStartDate())
                .endDate(projectDTO.getEndDate())
                .owner(owner)
                .build();

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return convertToDTO(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByOwner(Long ownerId) {
        return projectRepository.findByOwnerId(ownerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjectsByOwnerUsername(String username) {
        return projectRepository.findByOwnerUsername(username).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        project.setName(projectDTO.getName());
        project.setDescription(projectDTO.getDescription());
        project.setStartDate(projectDTO.getStartDate());
        project.setEndDate(projectDTO.getEndDate());

        if (projectDTO.getOwnerId() != null && !project.getOwner().getId().equals(projectDTO.getOwnerId())) {
            User newOwner = userRepository.findById(projectDTO.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found with id: " + projectDTO.getOwnerId()));
            project.setOwner(newOwner);
        }

        Project updatedProject = projectRepository.save(project);
        return convertToDTO(updatedProject);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    private ProjectDTO convertToDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .ownerId(project.getOwner().getId())
                .ownerUsername(project.getOwner().getUsername())
                .build();
    }
}
