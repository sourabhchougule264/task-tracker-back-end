package com.task.tracker.tasktrackerapp.service;

import com.task.tracker.tasktrackerapp.dto.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CognitoAuthService {

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Value("${aws.cognito.clientSecret}")
    private String clientSecret;

    @Value("${aws.cognito.region}")
    private String region;

    @Value("${aws.accessKeyId:#{null}}")
    private String accessKeyId;

    @Value("${aws.secretKey:#{null}}")
    private String secretKey;

    private CognitoIdentityProviderClient getCognitoClient() {
        AwsCredentialsProvider credentialsProvider;

        // Use explicit credentials if provided, otherwise use default chain
        if (accessKeyId != null && !accessKeyId.isEmpty() &&
            secretKey != null && !secretKey.isEmpty()) {
            credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretKey)
            );
        } else {
            credentialsProvider = DefaultCredentialsProvider.create();
        }
    

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Authenticate user with AWS Cognito
     */
    public AuthResponse authenticate(String username, String password) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            String secretHash = calculateSecretHash(username);

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", password);
            authParams.put("SECRET_HASH", secretHash);

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            log.info("User {} authenticated successfully", username);

            return AuthResponse.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .idToken(authResponse.authenticationResult().idToken())
                    .refreshToken(authResponse.authenticationResult().refreshToken())
                    .expiresIn(authResponse.authenticationResult().expiresIn())
                    .tokenType(authResponse.authenticationResult().tokenType())
                    .build();

        } catch (NotAuthorizedException e) {
            log.error("Authentication failed for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Invalid username or password");
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        } catch (Exception e) {
            log.error("Authentication error for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Register a new user in AWS Cognito
     */
    public void registerUser(String username, String password, String email) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            AttributeType emailAttribute = AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build();

            String secretHash = calculateSecretHash(username);

            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(username)
                    .password(password)
                    .userAttributes(emailAttribute)
                    .secretHash(secretHash)
                    .build();

            cognitoClient.signUp(signUpRequest);

            log.info("User {} registered successfully", username);

            // Add user to READ_ONLY group by default
            addUserToGroup(username, "READ_ONLY");

        } catch (UsernameExistsException e) {
            log.error("Username already exists: {}", username);
            throw new RuntimeException("Username already exists");
        } catch (InvalidPasswordException e) {
            log.error("Invalid password for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Invalid password. Password must meet requirements.");
        } catch (Exception e) {
            log.error("User registration failed for {}: {}", username, e.getMessage());
            throw new RuntimeException("User registration failed: " + e.getMessage());
        }
    }

    /**
     * Add user to a Cognito group (role)
     */
    public void addUserToGroup(String username, String groupName) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(groupName)
                    .build();

            cognitoClient.adminAddUserToGroup(request);

            log.info("User {} added to group {}", username, groupName);

        } catch (ResourceNotFoundException e) {
            log.error("Group {} not found", groupName);
            throw new RuntimeException("Group not found: " + groupName);
        } catch (Exception e) {
            log.error("Failed to add user {} to group {}: {}", username, groupName, e.getMessage());
            throw new RuntimeException("Failed to add user to group: " + e.getMessage());
        }
    }

    /**
     * Remove user from a Cognito group (role)
     */
    public void removeUserFromGroup(String username, String groupName) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            AdminRemoveUserFromGroupRequest request = AdminRemoveUserFromGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(groupName)
                    .build();

            cognitoClient.adminRemoveUserFromGroup(request);

            log.info("User {} removed from group {}", username, groupName);

        } catch (Exception e) {
            log.error("Failed to remove user {} from group {}: {}", username, groupName, e.getMessage());
            throw new RuntimeException("Failed to remove user from group: " + e.getMessage());
        }
    }

    /**
     * Replace user's role - removes all existing roles and assigns new one
     * This ensures user has only one role at a time
     */
    public void replaceUserRole(String username, String newRole) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            log.info("Replacing role for user {} with {}", username, newRole);

            // Step 1: Get all current groups for the user
            AdminListGroupsForUserRequest listRequest = AdminListGroupsForUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            AdminListGroupsForUserResponse listResponse = cognitoClient.adminListGroupsForUser(listRequest);

            // Step 2: Remove user from all existing groups
            for (GroupType group : listResponse.groups()) {
                String groupName = group.groupName();
                log.info("Removing user {} from group {}", username, groupName);
                
                AdminRemoveUserFromGroupRequest removeRequest = AdminRemoveUserFromGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .groupName(groupName)
                        .build();

                cognitoClient.adminRemoveUserFromGroup(removeRequest);
            }

            // Step 3: Add user to new group
            log.info("Adding user {} to group {}", username, newRole);
            
            AdminAddUserToGroupRequest addRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(newRole)
                    .build();

            cognitoClient.adminAddUserToGroup(addRequest);

            log.info("Successfully replaced role for user {} with {}", username, newRole);

        } catch (ResourceNotFoundException e) {
            log.error("Group {} not found", newRole);
            throw new RuntimeException("Group not found: " + newRole);
        } catch (Exception e) {
            log.error("Failed to replace role for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to replace user role: " + e.getMessage());
        }
    }

    /**
     * Get user's groups (roles) from Cognito
     * Returns a list of group names the user belongs to
     */
    public List<String> getUserGroups(String username) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            AdminListGroupsForUserRequest listRequest = AdminListGroupsForUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            AdminListGroupsForUserResponse listResponse = cognitoClient.adminListGroupsForUser(listRequest);

            return listResponse.groups().stream()
                    .map(GroupType::groupName)
                    .collect(Collectors.toList());

        } catch (UserNotFoundException e) {
            log.warn("User {} not found in Cognito when fetching groups", username);
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get groups for user {}: {}", username, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Refresh authentication token
     */
    public AuthResponse refreshToken(String refreshToken) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            log.info("Token refreshed successfully");

            return AuthResponse.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .idToken(authResponse.authenticationResult().idToken())
                    .expiresIn(authResponse.authenticationResult().expiresIn())
                    .tokenType(authResponse.authenticationResult().tokenType())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: " + e.getMessage());
        }
    }

    /**
     * Confirm user registration (for email verification)
     */
    public void confirmUserRegistration(String username, String confirmationCode) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            String secretHash = calculateSecretHash(username);

            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .username(username)
                    .confirmationCode(confirmationCode)
                    .secretHash(secretHash)
                    .build();

            cognitoClient.confirmSignUp(confirmRequest);

            log.info("User {} confirmed successfully", username);

        } catch (Exception e) {
            log.error("User confirmation failed for {}: {}", username, e.getMessage());
            throw new RuntimeException("User confirmation failed: " + e.getMessage());
        }
    }

    /**
     * Delete user from Cognito
     */
    public void deleteUser(String username) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            AdminDeleteUserRequest deleteRequest = AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            cognitoClient.adminDeleteUser(deleteRequest);

            log.info("User {} deleted successfully", username);

        } catch (Exception e) {
            log.error("User deletion failed for {}: {}", username, e.getMessage());
            throw new RuntimeException("User deletion failed: " + e.getMessage());
        }
    }

    /**
     * Sync user to database from JWT ID token
     * Extracts user information from the token and creates/updates database record
     */
    public void syncUserToDatabase(String idToken, UserService userService) {
        try {
            // Decode JWT token to extract user information
            String[] tokenParts = idToken.split("\\.");
            if (tokenParts.length < 2) {
                log.error("Invalid JWT token format");
                return;
            }

            // Decode the payload (second part of JWT)
            String payload = new String(Base64.getDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            
            // Parse JSON manually to extract fields
            String username = extractJsonValue(payload, "cognito:username");
            String email = extractJsonValue(payload, "email");
            String sub = extractJsonValue(payload, "sub");

            if (username != null && email != null) {
                log.info("Syncing user {} to database with sub: {}", username, sub);
                userService.syncUserFromCognito(username, email, sub);
                log.info("User {} synced to database successfully", username);
            } else {
                log.warn("Could not extract username or email from token for sync");
            }

        } catch (Exception e) {
            log.error("Error syncing user to database from token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sync user to database: " + e.getMessage());
        }
    }

    /**
     * Simple JSON value extractor (without using external JSON library)
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return null;
        }
        
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) {
            return null;
        }
        
        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) {
            return null;
        }
        
        return json.substring(valueStart + 1, valueEnd);
    }

    /**
     * Initiate forgot password flow - sends reset code to user's email
     */
    public void forgotPassword(String username) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            String secretHash = calculateSecretHash(username);

            ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder()
                    .clientId(clientId)
                    .username(username)
                    .secretHash(secretHash)
                    .build();

            cognitoClient.forgotPassword(forgotPasswordRequest);

            log.info("Forgot password flow initiated for user: {}", username);

        } catch (UserNotFoundException e) {
            log.error("User not found for forgot password: {}", username);
            throw new RuntimeException("User not found");
        } catch (Exception e) {
            log.error("Forgot password failed for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Forgot password failed: " + e.getMessage());
        }
    }

    /**
     * Confirm forgot password - verify code and set new password
     */
    public void confirmForgotPassword(String username, String confirmationCode, String newPassword) {
        try (CognitoIdentityProviderClient cognitoClient = getCognitoClient()) {

            String secretHash = calculateSecretHash(username);

            ConfirmForgotPasswordRequest confirmRequest = ConfirmForgotPasswordRequest.builder()
                    .clientId(clientId)
                    .username(username)
                    .confirmationCode(confirmationCode)
                    .password(newPassword)
                    .secretHash(secretHash)
                    .build();

            cognitoClient.confirmForgotPassword(confirmRequest);

            log.info("Password reset successfully for user: {}", username);

        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user {}", username);
            throw new RuntimeException("Invalid confirmation code");
        } catch (ExpiredCodeException e) {
            log.error("Confirmation code expired for user {}", username);
            throw new RuntimeException("Confirmation code has expired");
        } catch (Exception e) {
            log.error("Confirm forgot password failed for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to reset password: " + e.getMessage());
        }
    }

    /**
     * Calculate secret hash for Cognito authentication
     * Required when app client has a secret
     */
    private String calculateSecretHash(String username) {
        try {
            String message = username + clientId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash: " + e.getMessage());
        }
    }
}
