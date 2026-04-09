package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.UserDTO;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CognitoAuthService cognitoAuthService;

    /**
     * Create or sync user from Cognito
     * This is typically called after Cognito registration
     */
    @Transactional
    public UserDTO syncUserFromCognito(String username, String email, String cognitoSub) {
        User user = null;
        
        // First, try to find by cognitoSub (most reliable identifier)
        if (cognitoSub != null && !cognitoSub.isEmpty()) {
            user = userRepository.findByCognitoSub(cognitoSub).orElse(null);
        }
        
        // If not found by cognitoSub, try by email (to handle existing users)
        if (user == null && email != null && !email.isEmpty()) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        
        // If still not found, try by username
        if (user == null && username != null && !username.isEmpty()) {
            user = userRepository.findByUsername(username).orElse(null);
        }
        
        // If user exists, update their information
        if (user != null) {
            user.setUsername(username);
            user.setEmail(email);
            user.setCognitoSub(cognitoSub);
            user.setIsActive(true);
        } else {
            // Create new user if doesn't exist
            user = User.builder()
                    .username(username)
                    .email(email)
                    .cognitoSub(cognitoSub)
                    .isActive(true)
                    .build();
        }

        user = userRepository.save(user);
        return convertToDTO(user);
    }

    /**
     * Create user profile (without password - managed by Cognito)
     */
    @Transactional
    public UserDTO createUserProfile(UserDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("Username already exists: " + userDTO.getUsername());
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists: " + userDTO.getEmail());
        }

        User user = User.builder()
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .cognitoSub(userDTO.getCognitoSub())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    UserDTO dto = convertToDTO(user);
                    // Fetch user's roles from Cognito
                    try {
                        List<String> roles = cognitoAuthService.getUserGroups(user.getUsername());
                        dto.setRoles(roles);
                    } catch (Exception e) {
                        // If fetching roles fails, set empty list
                        dto.setRoles(new java.util.ArrayList<>());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Check if username is being changed and if it already exists
        if (!user.getUsername().equals(userDTO.getUsername()) &&
            userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("Username already exists: " + userDTO.getUsername());
        }

        // Check if email is being changed and if it already exists
        if (!user.getEmail().equals(userDTO.getEmail()) &&
            userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists: " + userDTO.getEmail());
        }

        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.deleteById(id);
            cognitoAuthService.deleteUser(user.get().getUsername());
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .cognitoSub(user.getCognitoSub())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .build();
    }
}
