package com.example.education_platform.security;

import com.example.education_platform.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues the stateless access token. Refresh tokens are the database's job, not this class'. */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public IssuedToken issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("eduflow")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                // Without a unique id, two tokens issued in the same second are byte-identical,
                // so a refresh would hand the client back the token it already had.
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name());
        if (user.getLevel() != null) {
            claims.claim("level", user.getLevel().code());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
