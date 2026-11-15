package com.nexus.rinde.subscription.infrastructure.persistence.flyway;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Migraciones del esquema subscription, con historial separado por contexto. */
@Configuration
public class SubscriptionFlywayConfiguration {

  @Bean(initMethod = "migrate")
  public Flyway subscriptionFlyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .schemas("subscription")
        .defaultSchema("subscription")
        .createSchemas(true)
        .locations("classpath:db/migration/subscription")
        .load();
  }

  /** Hibernate debe arrancar después de que Flyway cree las tablas. */
  @Bean
  public static EntityManagerFactoryDependsOnPostProcessor subscriptionFlywayDependsOn() {
    return new EntityManagerFactoryDependsOnPostProcessor("subscriptionFlyway");
  }
}
