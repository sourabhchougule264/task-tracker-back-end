package com.task.tracker.tasktrackerapp.repository;

import com.task.tracker.tasktrackerapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);
    List<Project> findByOwnerUsername(String username);
    List<Project> findByNameContainingIgnoreCase(String name);
}
