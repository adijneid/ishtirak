package org.example.ishtirak.server.security;

import org.example.ishtirak.common.model.Role;
import org.example.ishtirak.server.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Stateless login token: base64url(userId:username:role:expiry) + "." + HMAC-SHA256 signature.
 * Nothing is kept in memory, so tokens survive a server restart and need no extra table.
 */
@Service
public class TokenService {
    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] secret;
    private final Duration ttl;

    public TokenService(@Value("${ishtirak.token.secret}") String secret,
                        @Value("${ishtirak.token.ttl-hours}") long ttlHours) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = Duration.ofHours(ttlHours);
    }

    public String createToken(Integer userId, String username, Role role) {
        long expiry = Instant.now().plus(ttl).getEpochSecond();
        String payload = userId + ":" + username + ":" + role.name() + ":" + expiry;
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }

    public SessionUser verify(String token) {
        int dot = token.lastIndexOf('.');
        if (dot <= 0) {
            throw invalid();
        }
        String encoded = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        if (!MessageDigest.isEqual(sign(encoded).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw invalid();
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split(":");
            if (parts.length != 4) {
                throw invalid();
            }
            if (Instant.now().getEpochSecond() > Long.parseLong(parts[3])) {
                throw new UnauthorizedException("TOKEN_EXPIRED", "Session expired, please log in again");
            }
            return new SessionUser(Integer.valueOf(parts[0]), parts[1], Role.valueOf(parts[2]));
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign token", e);
        }
    }

    private UnauthorizedException invalid() {
        return new UnauthorizedException("INVALID_TOKEN", "Invalid token, please log in again");
    }
}
