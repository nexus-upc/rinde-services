package com.nexus.rinde.incident.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describe la API en Swagger UI y declara el esquema Bearer para probar los endpoints protegidos.
 */
@Configuration
public class OpenApiConfiguration {

  public static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI rindeOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("RINDE Incident API")
                .version("v1")
                .description(
                    "Servicio de Incident Management de RINDE by NEXUS: incidencias reportadas por el conductor durante el viaje y su atención."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
