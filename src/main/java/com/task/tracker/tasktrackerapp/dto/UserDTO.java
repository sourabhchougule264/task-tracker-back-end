package com.task.tracker.tasktrackerapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String cognitoSub;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private List<String> roles;  // User's Cognito groups/roles

    // Note: Roles are managed by AWS Cognito Groups
    // They are fetched from Cognito and included in the DTO
}
