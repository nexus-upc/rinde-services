package com.nexus.rinde.support;

import com.nexus.rinde.shared.infrastructure.security.JwtClaims;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Emite tokens con la misma firma y los mismos claims que leen los servicios de RINDE. */
@Component
public class TestTokens {

  private final SecretKey key;

  public TestTokens(@Value("${rinde.security.jwt.secret}") String secret) {
    this.key = JwtClaims.signingKey(secret);
  }

  public String issue(UUID userId, UUID tenantId, String role, String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim(JwtClaims.TENANT_ID, tenantId.toString())
        .claim(JwtClaims.ROLE, role)
        .claim(JwtClaims.TENANT_STATUS, "ACTIVE")
        .claim(JwtClaims.EMAIL, email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofHours(1))))
        .signWith(key)
        .compact();
  }
}
