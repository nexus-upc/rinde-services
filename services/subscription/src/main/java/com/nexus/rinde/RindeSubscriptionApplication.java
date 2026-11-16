package com.nexus.rinde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada del servicio de Subscriptions &amp; Billing. El paquete raíz com.nexus.rinde carga los
 * beans de rinde-shared-kernel. La autenticación es solo por JWT. Habilita las tareas programadas
 * (por ejemplo, el ciclo de vida de las suscripciones).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
public class RindeSubscriptionApplication {

  public static void main(String[] args) {
    SpringApplication.run(RindeSubscriptionApplication.class, args);
  }
}
