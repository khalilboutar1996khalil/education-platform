package com.example.education_platform.security;

import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Resolves the authenticated user from the JWT subject, which carries the user id. */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository users;

    /** The signed-in user, or empty on a public endpoint. */
    public Optional<User> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            return Optional.empty();
        }
        return users.findById(Long.valueOf(jwt.getToken().getSubject()));
    }

    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new InsufficientAuthenticationException("Not authenticated");
        }
        Long id = Long.valueOf(jwt.getToken().getSubject());
        return users.findById(id)
                .orElseThrow(() -> new InsufficientAuthenticationException("User no longer exists"));
    }
}
