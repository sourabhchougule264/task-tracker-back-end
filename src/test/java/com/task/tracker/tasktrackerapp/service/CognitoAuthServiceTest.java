package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitoAuthServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private CognitoAuthService cognitoAuthService;

    private final String USER_POOL_ID = "us-east-1_testPool";
    private final String CLIENT_ID = "testClientId";
    private final String CLIENT_SECRET = "testClientSecret";
    private final String USERNAME = "testuser";
    private final String PASSWORD = "Password123!";
    private final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // Inject @Value fields manually since we aren't using a Spring Context
        ReflectionTestUtils.setField(cognitoAuthService, "userPoolId", USER_POOL_ID);
        ReflectionTestUtils.setField(cognitoAuthService, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(cognitoAuthService, "clientSecret", CLIENT_SECRET);
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Authenticate - Success")
        void authenticate_Success() {
            AuthenticationResultType authResult = AuthenticationResultType.builder()
                    .accessToken("access-token")
                    .idToken("id-token")
                    .refreshToken("refresh-token")
                    .expiresIn(3600)
                    .tokenType("Bearer")
                    .build();

            InitiateAuthResponse response = InitiateAuthResponse.builder()
                    .authenticationResult(authResult)
                    .build();

            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(response);

            AuthResponse result = cognitoAuthService.authenticate(USERNAME, PASSWORD);

            assertNotNull(result);
            assertEquals("access-token", result.getAccessToken());
            verify(cognitoClient).initiateAuth(any(InitiateAuthRequest.class));
        }

        @Test
        @DisplayName("Authenticate - Not Authorized")
        void authenticate_NotAuthorized() {
            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                    .thenThrow(NotAuthorizedException.builder().message("Invalid details").build());

            assertThrows(RuntimeException.class, () -> cognitoAuthService.authenticate(USERNAME, PASSWORD));
        }

        @Test
        @DisplayName("Authenticate - User Not Found")
        void authenticate_UserNotFound() {
            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                    .thenThrow(UserNotFoundException.builder().message("User does not exist").build());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> cognitoAuthService.authenticate(USERNAME, PASSWORD));
            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Authenticate - Generic Exception")
        void authenticate_GenericException() {
            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.authenticate(USERNAME, PASSWORD));
        }
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Register User - Success")
        void registerUser_Success() {
            when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder().build());

            cognitoAuthService.registerUser(USERNAME, PASSWORD, EMAIL);

            verify(cognitoClient).signUp(any(SignUpRequest.class));
            verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
        }

        @Test
        @DisplayName("Register User - Username Already Exists")
        void registerUser_UsernameExists() {
            when(cognitoClient.signUp(any(SignUpRequest.class)))
                    .thenThrow(UsernameExistsException.builder().message("Username already exists").build());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.registerUser(USERNAME, PASSWORD, EMAIL));
            assertEquals("Username already exists", exception.getMessage());
        }

        @Test
        @DisplayName("Register User - Invalid Password")
        void registerUser_InvalidPassword() {
            when(cognitoClient.signUp(any(SignUpRequest.class)))
                    .thenThrow(InvalidPasswordException.builder().message("Password too weak").build());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.registerUser(USERNAME, PASSWORD, EMAIL));
            assertTrue(exception.getMessage().contains("Invalid password"));
        }

        @Test
        @DisplayName("Register User - Generic Exception")
        void registerUser_GenericException() {
            when(cognitoClient.signUp(any(SignUpRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.registerUser(USERNAME, PASSWORD, EMAIL));
        }
    }

    @Nested
    @DisplayName("Add User To Group Tests")
    class AddUserToGroupTests {

        @Test
        @DisplayName("Add User To Group - Success")
        void addUserToGroup_Success() {
            cognitoAuthService.addUserToGroup(USERNAME, "ADMIN");

            verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
        }

        @Test
        @DisplayName("Add User To Group - Group Not Found")
        void addUserToGroup_GroupNotFound() {
            when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                    .thenThrow(ResourceNotFoundException.builder().message("Group not found").build());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.addUserToGroup(USERNAME, "NONEXISTENT_GROUP"));
            assertTrue(exception.getMessage().contains("Group not found"));
        }

        @Test
        @DisplayName("Add User To Group - Generic Exception")
        void addUserToGroup_GenericException() {
            when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.addUserToGroup(USERNAME, "ADMIN"));
        }
    }

    @Nested
    @DisplayName("Remove User From Group Tests")
    class RemoveUserFromGroupTests {

        @Test
        @DisplayName("Remove User From Group - Success")
        void removeUserFromGroup_Success() {
            cognitoAuthService.removeUserFromGroup(USERNAME, "ADMIN");

            verify(cognitoClient).adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class));
        }

        @Test
        @DisplayName("Remove User From Group - Generic Exception")
        void removeUserFromGroup_GenericException() {
            when(cognitoClient.adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.removeUserFromGroup(USERNAME, "ADMIN"));
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleTests {

        @Test
        @DisplayName("Replace User Role - Success")
        void replaceUserRole_Success() {
            GroupType group1 = GroupType.builder().groupName("OLD_ROLE").build();
            AdminListGroupsForUserResponse listResponse = AdminListGroupsForUserResponse.builder()
                    .groups(List.of(group1))
                    .build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(listResponse);

            cognitoAuthService.replaceUserRole(USERNAME, "NEW_ROLE");

            verify(cognitoClient).adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class));
            verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
        }

        @Test
        @DisplayName("Replace User Role - No Existing Groups")
        void replaceUserRole_NoExistingGroups() {
            AdminListGroupsForUserResponse listResponse = AdminListGroupsForUserResponse.builder()
                    .groups(List.of())
                    .build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(listResponse);

            cognitoAuthService.replaceUserRole(USERNAME, "NEW_ROLE");

            verify(cognitoClient, never()).adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class));
            verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
        }

        @Test
        @DisplayName("Replace User Role - Multiple Existing Groups")
        void replaceUserRole_MultipleGroups() {
            GroupType group1 = GroupType.builder().groupName("ADMIN").build();
            GroupType group2 = GroupType.builder().groupName("EDITOR").build();
            AdminListGroupsForUserResponse listResponse = AdminListGroupsForUserResponse.builder()
                    .groups(List.of(group1, group2))
                    .build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(listResponse);

            cognitoAuthService.replaceUserRole(USERNAME, "VIEWER");

            verify(cognitoClient, times(2)).adminRemoveUserFromGroup(any(AdminRemoveUserFromGroupRequest.class));
            verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
        }

        @Test
        @DisplayName("Replace User Role - Group Not Found")
        void replaceUserRole_GroupNotFound() {
            GroupType group1 = GroupType.builder().groupName("OLD_ROLE").build();
            AdminListGroupsForUserResponse listResponse = AdminListGroupsForUserResponse.builder()
                    .groups(List.of(group1))
                    .build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(listResponse);
            when(cognitoClient.adminAddUserToGroup(any(AdminAddUserToGroupRequest.class)))
                    .thenThrow(ResourceNotFoundException.builder().message("Group not found").build());

            assertThrows(RuntimeException.class, () -> cognitoAuthService.replaceUserRole(USERNAME, "NONEXISTENT"));
        }

        @Test
        @DisplayName("Replace User Role - Generic Exception")
        void replaceUserRole_GenericException() {
            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.replaceUserRole(USERNAME, "NEW_ROLE"));
        }

        @Test
        @DisplayName("Get User Groups - Success")
        void getUserGroups_Success() {
            GroupType group = GroupType.builder().groupName("ADMIN").build();
            AdminListGroupsForUserResponse response = AdminListGroupsForUserResponse.builder().groups(group).build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(response);

            List<String> groups = cognitoAuthService.getUserGroups(USERNAME);

            assertEquals(1, groups.size());
            assertEquals("ADMIN", groups.get(0));
        }

        @Test
        @DisplayName("Get User Groups - User Not Found")
        void getUserGroups_UserNotFound() {
            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
                    .thenThrow(UserNotFoundException.builder().message("User not found").build());

            List<String> groups = cognitoAuthService.getUserGroups(USERNAME);

            assertTrue(groups.isEmpty());
        }

        @Test
        @DisplayName("Get User Groups - Generic Exception")
        void getUserGroups_GenericException() {
            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            List<String> groups = cognitoAuthService.getUserGroups(USERNAME);

            assertTrue(groups.isEmpty());
        }

        @Test
        @DisplayName("Get User Groups - Empty List")
        void getUserGroups_EmptyList() {
            AdminListGroupsForUserResponse response = AdminListGroupsForUserResponse.builder()
                    .groups(List.of())
                    .build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(response);

            List<String> groups = cognitoAuthService.getUserGroups(USERNAME);

            assertTrue(groups.isEmpty());
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh Token - Success")
        void refreshToken_Success() {
            AuthenticationResultType authResult = AuthenticationResultType.builder()
                    .accessToken("new-access-token")
                    .idToken("new-id-token")
                    .expiresIn(3600)
                    .tokenType("Bearer")
                    .build();

            InitiateAuthResponse response = InitiateAuthResponse.builder()
                    .authenticationResult(authResult)
                    .build();

            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(response);

            AuthResponse result = cognitoAuthService.refreshToken("refresh-token");

            assertNotNull(result);
            assertEquals("new-access-token", result.getAccessToken());
        }

        @Test
        @DisplayName("Refresh Token - Generic Exception")
        void refreshToken_GenericException() {
            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                    .thenThrow(new RuntimeException("Token refresh failed"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.refreshToken("invalid-token"));
        }
    }

    @Nested
    @DisplayName("Confirm User Registration Tests")
    class ConfirmRegistrationTests {

        @Test
        @DisplayName("Confirm User Registration - Success")
        void confirmUserRegistration_Success() {
            cognitoAuthService.confirmUserRegistration(USERNAME, "123456");

            verify(cognitoClient).confirmSignUp(any(ConfirmSignUpRequest.class));
        }

        @Test
        @DisplayName("Confirm User Registration - Generic Exception")
        void confirmUserRegistration_GenericException() {
            when(cognitoClient.confirmSignUp(any(ConfirmSignUpRequest.class)))
                    .thenThrow(new RuntimeException("Confirmation failed"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.confirmUserRegistration(USERNAME, "123456"));
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Delete User - Success")
        void deleteUser_Success() {
            cognitoAuthService.deleteUser(USERNAME);

            verify(cognitoClient).adminDeleteUser(any(AdminDeleteUserRequest.class));
        }

        @Test
        @DisplayName("Delete User - Generic Exception")
        void deleteUser_GenericException() {
            when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                    .thenThrow(new RuntimeException("Deletion failed"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.deleteUser(USERNAME));
        }
    }

    @Nested
    @DisplayName("JWT Sync Tests")
    class JwtSyncTests {

        @Test
        @DisplayName("Sync User To Database - Success with cognito:username")
        void syncUserToDatabase_SuccessWithCognitoUsername() {
            String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getEncoder().encodeToString(
                    "{\"cognito:username\":\"testuser\", \"email\":\"test@example.com\", \"sub\":\"uuid-123\"}".getBytes());
            String idToken = header + "." + payload + ".signature";

            cognitoAuthService.syncUserToDatabase(idToken, userService);

            verify(userService).syncUserFromCognito("testuser", "test@example.com", "uuid-123");
        }

        @Test
        @DisplayName("Sync User To Database - Success with standard username")
        void syncUserToDatabase_SuccessWithStandardUsername() {
            String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getEncoder().encodeToString(
                    "{\"username\":\"testuser\", \"email\":\"test@example.com\", \"sub\":\"uuid-456\"}".getBytes());
            String idToken = header + "." + payload + ".signature";

            cognitoAuthService.syncUserToDatabase(idToken, userService);

            verify(userService).syncUserFromCognito("testuser", "test@example.com", "uuid-456");
        }

        @Test
        @DisplayName("Sync User To Database - Missing Email")
        void syncUserToDatabase_MissingEmail() {
            String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getEncoder().encodeToString(
                    "{\"cognito:username\":\"testuser\", \"sub\":\"uuid-123\"}".getBytes());
            String idToken = header + "." + payload + ".signature";

            cognitoAuthService.syncUserToDatabase(idToken, userService);

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Sync User To Database - Missing Username")
        void syncUserToDatabase_MissingUsername() {
            String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getEncoder().encodeToString(
                    "{\"email\":\"test@example.com\", \"sub\":\"uuid-123\"}".getBytes());
            String idToken = header + "." + payload + ".signature";

            cognitoAuthService.syncUserToDatabase(idToken, userService);

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Sync User To Database - Invalid Token Format")
        void syncUserToDatabase_InvalidTokenFormat() {
            String idToken = "invalid-token-no-dots";

            cognitoAuthService.syncUserToDatabase(idToken, userService);

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Sync User To Database - Exception During Parsing")
        void syncUserToDatabase_ExceptionDuringParsing() {
            String idToken = "invalid.payload.signature";

            assertThrows(RuntimeException.class, () -> cognitoAuthService.syncUserToDatabase(idToken, userService));
        }
    }

    @Nested
    @DisplayName("Password Recovery Tests")
    class PasswordTests {

        @Test
        @DisplayName("Forgot Password - Success")
        void forgotPassword_Success() {
            cognitoAuthService.forgotPassword(USERNAME);
            verify(cognitoClient).forgotPassword(any(ForgotPasswordRequest.class));
        }

        @Test
        @DisplayName("Forgot Password - User Not Found")
        void forgotPassword_UserNotFound() {
            when(cognitoClient.forgotPassword(any(ForgotPasswordRequest.class)))
                    .thenThrow(UserNotFoundException.builder().message("User not found").build());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.forgotPassword(USERNAME));
            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Forgot Password - Generic Exception")
        void forgotPassword_GenericException() {
            when(cognitoClient.forgotPassword(any(ForgotPasswordRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class, () -> cognitoAuthService.forgotPassword(USERNAME));
        }

        @Test
        @DisplayName("Confirm Forgot Password - Success")
        void confirmForgotPassword_Success() {
            cognitoAuthService.confirmForgotPassword(USERNAME, "123456", "NewPassword123!");
            verify(cognitoClient).confirmForgotPassword(any(ConfirmForgotPasswordRequest.class));
        }

        @Test
        @DisplayName("Confirm Forgot Password - Code Mismatch")
        void confirmForgotPassword_CodeMismatch() {
            when(cognitoClient.confirmForgotPassword(any(ConfirmForgotPasswordRequest.class)))
                    .thenThrow(CodeMismatchException.builder().message("Invalid code").build());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.confirmForgotPassword(USERNAME, "invalid-code", "NewPassword123!"));
            assertEquals("Invalid confirmation code", exception.getMessage());
        }

        @Test
        @DisplayName("Confirm Forgot Password - Expired Code")
        void confirmForgotPassword_ExpiredCode() {
            when(cognitoClient.confirmForgotPassword(any(ConfirmForgotPasswordRequest.class)))
                    .thenThrow(ExpiredCodeException.builder().message("Code expired").build());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.confirmForgotPassword(USERNAME, "expired-code", "NewPassword123!"));
            assertEquals("Confirmation code has expired", exception.getMessage());
        }

        @Test
        @DisplayName("Confirm Forgot Password - Generic Exception")
        void confirmForgotPassword_GenericException() {
            when(cognitoClient.confirmForgotPassword(any(ConfirmForgotPasswordRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            assertThrows(RuntimeException.class,
                    () -> cognitoAuthService.confirmForgotPassword(USERNAME, "123456", "NewPassword123!"));
        }
    }
}