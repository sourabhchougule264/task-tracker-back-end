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
            // Mocking the complex AWS response structure
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
        void authenticate_Failed() {
            when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                    .thenThrow(NotAuthorizedException.builder().message("Invalid details").build());

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

            // Note: registerUser calls addUserToGroup internally
            cognitoAuthService.registerUser(USERNAME, PASSWORD, EMAIL);

            verify(cognitoClient).signUp(any(SignUpRequest.class));
            verify(cognitoClient).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleTests {

        @Test
        @DisplayName("Replace User Role - Success")
        void replaceUserRole_Success() {
            // Mock existing groups
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
        @DisplayName("Get User Groups - Success")
        void getUserGroups_Success() {
            GroupType group = GroupType.builder().groupName("ADMIN").build();
            AdminListGroupsForUserResponse response = AdminListGroupsForUserResponse.builder().groups(group).build();

            when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class))).thenReturn(response);

            List<String> groups = cognitoAuthService.getUserGroups(USERNAME);

            assertEquals(1, groups.size());
            assertEquals("ADMIN", groups.get(0));
        }
    }

    @Nested
    @DisplayName("JWT Sync Tests")
    class JwtSyncTests {

        @Test
        @DisplayName("Sync User To Database - Success")
        void syncUserToDatabase_Success() {
            // Create a fake JWT payload
            String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
            String payload = Base64.getEncoder().encodeToString(
                    "{\"cognito:username\":\"testuser\", \"email\":\"test@example.com\", \"sub\":\"uuid-123\"}".getBytes());
            String idToken = header + "." + payload + ".signature";

            cognitoAuthService.syncUserToDatabase(idToken, userService);

            verify(userService).syncUserFromCognito("testuser", "test@example.com", "uuid-123");
        }

        @Test
        @DisplayName("Sync User To Database - Invalid Token")
        void syncUserToDatabase_InvalidToken() {
            String idToken = "invalid-token-no-dots";
            cognitoAuthService.syncUserToDatabase(idToken, userService);
            verifyNoInteractions(userService);
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
        @DisplayName("Confirm Forgot Password - Success")
        void confirmForgotPassword_Success() {
            cognitoAuthService.confirmForgotPassword(USERNAME, "123456", "NewPassword123!");
            verify(cognitoClient).confirmForgotPassword(any(ConfirmForgotPasswordRequest.class));
        }
    }
}