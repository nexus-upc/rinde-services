package com.nexus.rinde.iam.infrastructure.mail;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Configuración del proveedor de correos transaccionales de IAM. */
@ConfigurationProperties(prefix = "rinde.mail")
public class MailProperties {

  private String provider = "demo";
  private String acsConnectionString = "";
  private String sender = "";
  private Environment environment;

  @Autowired
  void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    String normalized = provider == null ? "demo" : provider.trim();
    if (environment != null
        && environment.acceptsProfiles(Profiles.of("prod", "production"))
        && !"azure".equals(normalized)) {
      throw new IllegalStateException("En producción el correo debe usar el proveedor azure.");
    }
    if ("azure".equals(normalized)) {
      throw new IllegalStateException(
          "El proveedor de correo azure está pendiente de implementar en S4-27.");
    }
    if (!"demo".equals(normalized)) {
      throw new IllegalStateException("Proveedor de correo no válido: " + normalized);
    }
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getAcsConnectionString() {
    return acsConnectionString;
  }

  public void setAcsConnectionString(String acsConnectionString) {
    this.acsConnectionString = acsConnectionString;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }
}
