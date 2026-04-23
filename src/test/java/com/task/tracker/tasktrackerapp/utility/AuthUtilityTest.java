package com.task.tracker.tasktrackerapp.utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUtilityTest {

    @Test
    @DisplayName("Should extract username from JWT claim when principal is Jwt")
    void getUsernameFromAuth_withJwtPrincipal() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        String expectedUsername = "test_user_jwt";

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("username")).thenReturn(expectedUsername);

        // Act
        String result = AuthUtility.getUsernameFromAuth(authentication);

        // Assert
        assertEquals(expectedUsername, result, "Should return the username from JWT claim");
    }

    @Test
    @DisplayName("Should fall back to authentication.getName() when principal is NOT Jwt")
    void getUsernameFromAuth_withNonJwtPrincipal() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        String expectedUsername = "standard_user";

        // Principal is just a String or a standard UserDetails object
        when(authentication.getPrincipal()).thenReturn("not-a-jwt-object");
        when(authentication.getName()).thenReturn(expectedUsername);

        // Act
        String result = AuthUtility.getUsernameFromAuth(authentication);

        // Assert
        assertEquals(expectedUsername, result, "Should return authentication.getName() for non-JWT principals");
    }

    @Test
    @DisplayName("Should return null if principal is Jwt but claim is missing")
    void getUsernameFromAuth_withJwtPrincipalMissingClaim() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("username")).thenReturn(null);

        // Act
        String result = AuthUtility.getUsernameFromAuth(authentication);

        // Assert
        assertNull(result, "Should return null if the claim is not present in the JWT");
    }
}