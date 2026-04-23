package com.task.tracker.tasktrackerapp.utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("JWT Principal Tests")
    class JwtPrincipalTests {
        @Test
        @DisplayName("Should extract username from JWT claim when principal is Jwt")
        void getUsernameFromAuth_withJwtPrincipal() {
            Authentication authentication = mock(Authentication.class);
            Jwt jwt = mock(Jwt.class);
            String expectedUsername = "test_user_jwt";

            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getClaim("username")).thenReturn(expectedUsername);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertEquals(expectedUsername, result, "Should return the username from JWT claim");
        }

        @Test
        @DisplayName("Should return null if principal is Jwt but claim is missing")
        void getUsernameFromAuth_withJwtPrincipalMissingClaim() {
            Authentication authentication = mock(Authentication.class);
            Jwt jwt = mock(Jwt.class);

            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getClaim("username")).thenReturn(null);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertNull(result, "Should return null if the claim is not present in the JWT");
        }

        @Test
        @DisplayName("Should return empty string when JWT claim is empty")
        void getUsernameFromAuth_withJwtPrincipalEmptyClaim() {
            Authentication authentication = mock(Authentication.class);
            Jwt jwt = mock(Jwt.class);
            String emptyUsername = "";

            when(authentication.getPrincipal()).thenReturn(jwt);
            when(jwt.getClaim("username")).thenReturn(emptyUsername);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertEquals(emptyUsername, result, "Should return empty string from JWT claim if that's what's stored");
        }
    }

    @Nested
    @DisplayName("Non-JWT Principal Tests")
    class NonJwtPrincipalTests {
        @Test
        @DisplayName("Should fall back to authentication.getName() when principal is NOT Jwt")
        void getUsernameFromAuth_withNonJwtPrincipal() {
            Authentication authentication = mock(Authentication.class);
            String expectedUsername = "standard_user";

            when(authentication.getPrincipal()).thenReturn("not-a-jwt-object");
            when(authentication.getName()).thenReturn(expectedUsername);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertEquals(expectedUsername, result, "Should return authentication.getName() for non-JWT principals");
        }

        @Test
        @DisplayName("Should return null from getName() when principal is not JWT")
        void getUsernameFromAuth_withNonJwtPrincipalNullName() {
            Authentication authentication = mock(Authentication.class);

            when(authentication.getPrincipal()).thenReturn("not-a-jwt-object");
            when(authentication.getName()).thenReturn(null);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertNull(result, "Should return null from getName() for non-JWT principals if getName() returns null");
        }

        @Test
        @DisplayName("Should return empty string from getName() when principal is not JWT")
        void getUsernameFromAuth_withNonJwtPrincipalEmptyName() {
            Authentication authentication = mock(Authentication.class);
            String emptyName = "";

            when(authentication.getPrincipal()).thenReturn("not-a-jwt-object");
            when(authentication.getName()).thenReturn(emptyName);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertEquals(emptyName, result, "Should return empty string from getName() for non-JWT principals");
        }

        @Test
        @DisplayName("Should handle null principal object")
        void getUsernameFromAuth_withNullPrincipal() {
            Authentication authentication = mock(Authentication.class);
            String expectedUsername = "fallback_user";

            when(authentication.getPrincipal()).thenReturn(null);
            when(authentication.getName()).thenReturn(expectedUsername);

            String result = AuthUtility.getUsernameFromAuth(authentication);

            assertEquals(expectedUsername, result, "Should fall back to getName() when principal is null");
        }
    }
}