package com.nexus.rinde.incident.infrastructure.security;

import com.nexus.rinde.shared.infrastructure.security.JwtAuthenticationFilter;
import com.nexus.rinde.shared.infrastructure.security.JwtTokenReader;
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
 * API sin sesión, autenticada con JWT. Son públicas las consultas internas, el health y Swagger.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenReader tokenReader,
      TenantStatusProvider tenantStatusProvider,
      ProblemDetailSecurityHandler errorHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Sin JWT: el gateway no enruta /api/internal, solo lo llaman otros servicios.
                    .requestMatchers(HttpMethod.GET, "/api/internal/v1/**")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
        .addFilterBefore(
            new JwtAuthenticationFilter(tokenReader, tenantStatusProvider),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
