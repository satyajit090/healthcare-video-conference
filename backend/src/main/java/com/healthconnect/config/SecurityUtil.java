package com.healthconnect.config;

import com.healthconnect.common.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw ApiException.forbidden("Not authenticated");
        }
        return id;
    }
}
