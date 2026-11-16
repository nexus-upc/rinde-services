package com.nexus.rinde.subscription.infrastructure.security;

import com.nexus.rinde.shared.infrastructure.security.JwtAuthenticationFilter;
import com.nexus.rinde.shared.infrastructure.security.JwtTokenReader;
import com.nexus.rinde.shared.infrastructure.security.PlatformAuthenticationFilter;
import com.nexus.rinde.shared.infrastructure.security.PlatformTokenReader;
import com.nexus.rinde.shared.infrastructure.security.TenantStatusProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API sin sesión, autenticada con JWT. Son públicos el webhook de pago (su firma X-RINDE-Signature
 * la valida el controlador), Swagger y el health. La administración de plataforma exige el token de
 * PLATFORM_OWNER, con firma propia.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenReader tokenReader,
      PlatformTokenReader platformTokens,
      TenantStatusProvider tenantStatusProvider,
      ProblemDetailSecurityHandler errorHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/billing/webhooks/payment")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/v1/plans", "/api/v1/subscriptions/**")
                    .authenticated()
                    .requestMatchers(
                        "/api/v1/platform/billing/**",
                        "/api/v1/platform/plans/**",
                        "/api/v1/platform/subscriptions/**",
                        "/api/v1/platform/payments/**",
                        "/api/v1/platform/audit/billing")
                    .hasAuthority("ROLE_PLATFORM_OWNER")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
        .addFilterBefore(
            new JwtAuthenticationFilter(tokenReader, tenantStatusProvider),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(
            new PlatformAuthenticationFilter(platformTokens), JwtAuthenticationFilter.class);
    return http.build();
  }
}
