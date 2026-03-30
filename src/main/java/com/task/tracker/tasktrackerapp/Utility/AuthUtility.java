package com.task.tracker.tasktrackerapp.Utility;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthUtility {
    
    /**
     * Helper method to extract username from JWT authentication
     */
    public static String getUsernameFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("username");
        }
        return authentication.getName();
    }
}
