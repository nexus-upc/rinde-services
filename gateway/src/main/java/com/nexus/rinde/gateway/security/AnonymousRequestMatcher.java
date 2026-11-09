package com.nexus.rinde.gateway.security;

import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * Rutas que no exigen JWT. Replican los permitAll de IAM, el webhook firmado de facturación y la
 * documentación. Las preflight CORS (OPTIONS) también pasan.
 */
@Component
public class AnonymousRequestMatcher {

  private static final AntPathMatcher PATHS = new AntPathMatcher();

  private static final List<Rule> RULES =
      List.of(
          new Rule(HttpMethod.GET, "/api/v1/demo/features"),
          new Rule(HttpMethod.POST, "/api/v1/platform/auth/sign-in"),
          new Rule(HttpMethod.POST, "/api/v1/tenants"),
          new Rule(HttpMethod.POST, "/api/v1/tenants/*/verification"),
          new Rule(HttpMethod.POST, "/api/v1/auth/sign-in"),
          new Rule(HttpMethod.POST, "/api/v1/auth/password-reset"),
          new Rule(HttpMethod.POST, "/api/v1/auth/password"),
          new Rule(HttpMethod.POST, "/api/v1/public/demo-requests"),
          new Rule(null, "/api/v1/billing/webhooks/**"),
          new Rule(null, "/v3/api-docs/**"),
          new Rule(null, "/swagger-ui.html"),
          new Rule(null, "/swagger-ui/**"),
          new Rule(null, "/webjars/**"),
          new Rule(null, "/actuator/health"),
          new Rule(null, "/actuator/health/**"));

  /** Indica si la petición puede pasar sin token. La ruta debe venir ya decodificada. */
  public boolean matches(HttpMethod method, String path) {
    if (HttpMethod.OPTIONS.equals(method)) {
      return true;
    }
    return RULES.stream().anyMatch(rule -> rule.matches(method, path));
  }

  private record Rule(HttpMethod method, String pattern) {

    boolean matches(HttpMethod requested, String path) {
      boolean sameMethod = method == null || method.equals(requested);
      return sameMethod && PATHS.match(pattern, path);
    }
  }
}
