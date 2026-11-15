package com.nexus.rinde.subscription.platform.infrastructure;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Consulta nombres por HTTP; no accede a las tablas de IAM. */
@Component
public class PlatformCompanyDirectory {
  private final RestClient client;

  public PlatformCompanyDirectory(
      @Value("${rinde.iam.base-url:http://localhost:8081}") String baseUrl) {
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(5000);
    client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }

  public Map<UUID, String> names(Collection<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    try {
      Name[] names =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/api/internal/v1/tenant-directory")
                          .queryParam(
                              "ids",
                              ids.stream()
                                  .distinct()
                                  .map(UUID::toString)
                                  .collect(Collectors.joining(",")))
                          .build())
              .retrieve()
              .body(Name[].class);
      if (names == null) throw new IllegalStateException("Sin respuesta de IAM.");
      return Arrays.stream(names).collect(Collectors.toMap(Name::id, Name::tradeName));
    } catch (RuntimeException ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el directorio de empresas.");
    }
  }

  public record Name(UUID id, String tradeName) {}
}
