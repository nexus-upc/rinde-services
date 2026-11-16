package com.nexus.rinde.incident.infrastructure.persistence.flyway;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Migraciones del esquema incident, con historial separado por contexto. */
@Configuration
public class IncidentFlywayConfiguration {

  @Bean(initMethod = "migrate")
  public Flyway incidentFlyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .schemas("incident")
        .defaultSchema("incident")
        .createSchemas(true)
        .locations("classpath:db/migration/incident")
        .load();
  }

  /** Hibernate debe arrancar después de que Flyway cree las tablas. */
  @Bean
  public static EntityManagerFactoryDependsOnPostProcessor incidentFlywayDependsOn() {
    return new EntityManagerFactoryDependsOnPostProcessor("incidentFlyway");
  }
}
