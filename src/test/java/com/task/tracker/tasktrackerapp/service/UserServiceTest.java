package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.UserDTO;
import com.task.tracker.tasktrackerapp.entity.User;
import com.task.tracker.tasktrackerapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CognitoAuthService cognitoAuthService;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("jdoe")
                .email("jane@example.com")
                .cognitoSub("sub-123")
                .firstName("Jane")
                .lastName("Doe")
                .isActive(true)
                .build();

        userDTO = UserDTO.builder()
                .username("jdoe")
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .cognitoSub("sub-123")
                .build();
    }

    @Nested
    @DisplayName("Sync User From Cognito Tests")
    class SyncTests {

        @Test
        @DisplayName("Should update existing user if found by Cognito Sub")
        void syncUser_UpdateExistingBySub() {
            when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            UserDTO result = userService.syncUserFromCognito("jdoe", "jane@example.com", "sub-123");

            assertThat(result.getUsername()).isEqualTo("jdoe");
            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Should create new user if no match found by sub, email, or username")
        void syncUser_CreateNew() {
            when(userRepository.findByCognitoSub(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

            UserDTO result = userService.syncUserFromCognito("newuser", "new@example.com", "new-sub");

            assertThat(result.getUsername()).isEqualTo("newuser");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Create User Profile Tests")
    class CreateProfileTests {

        @Test
        @DisplayName("Should throw exception if username already exists")
        void createUser_DuplicateUsername() {
            when(userRepository.existsByUsername("jdoe")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUserProfile(userDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("Should save user if data is unique")
        void createUser_Success() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            UserDTO result = userService.createUserProfile(userDTO);

            assertThat(result.getId()).isEqualTo(1L);
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Retrieve User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should return user by ID")
        void getUserById_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            UserDTO result = userService.getUserById(1L);
            assertThat(result.getUsername()).isEqualTo("jdoe");
        }

        @Test
        @DisplayName("getAllUsers should fetch roles from Cognito for each user")
        void getAllUsers_WithRoles() {
            when(userRepository.findAll()).thenReturn(List.of(mockUser));
            when(cognitoAuthService.getUserGroups("jdoe")).thenReturn(List.of("ADMIN", "USER"));

            List<UserDTO> results = userService.getAllUsers();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRoles()).containsExactly("ADMIN", "USER");
        }

        @Test
        @DisplayName("getAllUsers should handle Cognito failure gracefully")
        void getAllUsers_CognitoFails() {
            when(userRepository.findAll()).thenReturn(List.of(mockUser));
            when(cognitoAuthService.getUserGroups(anyString())).thenThrow(new RuntimeException("AWS Down"));

            List<UserDTO> results = userService.getAllUsers();

            assertThat(results.get(0).getRoles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update and Delete Tests")
    class UpdateDeleteTests {

        @Test
        @DisplayName("Update user should detect duplicate email change")
        void updateUser_DuplicateEmail() {
            User existingUser = User.builder().id(1L).email("old@ex.com").username("user1").build();
            UserDTO updateDto = UserDTO.builder().email("taken@ex.com").username("user1").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByEmail("taken@ex.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("Delete user should remove from DB and Cognito")
        void deleteUser_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
            verify(cognitoAuthService).deleteUser("jdoe");
        }

        @Test
        @DisplayName("Delete non-existent user should throw exception")
        void deleteUser_NotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(1L))
                    .isInstanceOf(RuntimeException.class);

            verify(cognitoAuthService, never()).deleteUser(anyString());
        }
    }
}