package com.nexus.rinde.settlement.infrastructure.persistence.flyway;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Migraciones del esquema settlement, con historial separado por contexto. */
@Configuration
public class SettlementFlywayConfiguration {

  @Bean(initMethod = "migrate")
  public Flyway settlementFlyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .schemas("settlement")
        .defaultSchema("settlement")
        .createSchemas(true)
        .locations("classpath:db/migration/settlement")
        .load();
  }

  /** Hibernate debe arrancar después de que Flyway cree las tablas. */
  @Bean
  public static EntityManagerFactoryDependsOnPostProcessor settlementFlywayDependsOn() {
    return new EntityManagerFactoryDependsOnPostProcessor("settlementFlyway");
  }
}
