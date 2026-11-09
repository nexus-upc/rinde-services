package com.nexus.rinde.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class AnonymousRequestMatcherTest {

  private final AnonymousRequestMatcher matcher = new AnonymousRequestMatcher();

  @Test
  void allowsOnlyTheIamPostRoutesThatAreAnonymous() {
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/auth/sign-in")).isTrue();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/auth/password-reset")).isTrue();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/auth/password")).isTrue();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/tenants")).isTrue();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/tenants/abc/verification")).isTrue();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/public/demo-requests")).isTrue();

    assertThat(matcher.matches(HttpMethod.GET, "/api/v1/public/demo-requests")).isFalse();
    assertThat(matcher.matches(HttpMethod.GET, "/api/v1/platform/demo-requests")).isFalse();

    assertThat(matcher.matches(HttpMethod.GET, "/api/v1/auth/sign-in")).isFalse();
    assertThat(matcher.matches(HttpMethod.GET, "/api/v1/tenants/abc")).isFalse();
    assertThat(matcher.matches(HttpMethod.PATCH, "/api/v1/users/abc")).isFalse();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/tenants/abc/otra-cosa")).isFalse();
  }

  @Test
  void allowsWebhooksDocsHealthAndPreflight() {
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/billing/webhooks/payment")).isTrue();
    assertThat(matcher.matches(HttpMethod.GET, "/v3/api-docs/iam")).isTrue();
    assertThat(matcher.matches(HttpMethod.GET, "/swagger-ui/index.html")).isTrue();
    assertThat(matcher.matches(HttpMethod.GET, "/actuator/health")).isTrue();
    assertThat(matcher.matches(HttpMethod.OPTIONS, "/api/v1/trips")).isTrue();
  }

  @Test
  void requiresTokenForMonolithRoutes() {
    assertThat(matcher.matches(HttpMethod.GET, "/api/v1/trips/1")).isFalse();
    assertThat(matcher.matches(HttpMethod.POST, "/api/v1/expenses/sync")).isFalse();
  }
}
