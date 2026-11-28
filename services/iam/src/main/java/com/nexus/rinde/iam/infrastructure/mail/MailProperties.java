package com.nexus.rinde.iam.infrastructure.mail;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Configuración del proveedor de correos transaccionales de IAM: demo o azure. */
@ConfigurationProperties(prefix = "rinde.mail")
public class MailProperties {

  static final String DEMO = "demo";
  static final String AZURE = "azure";
  private static final Profiles PRODUCTION = Profiles.of("prod", "production");

  private String provider = DEMO;
  private String acsConnectionString = "";
  private String sender = "";
  private Environment environment;

  @Autowired
  void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /** Valida el proveedor al arrancar; con azure exige la cadena de conexión y el remitente. */
  @PostConstruct
  void validate() {
    String selected = provider == null ? DEMO : provider;
    if (environment != null && environment.acceptsProfiles(PRODUCTION) && !AZURE.equals(selected)) {
      throw new IllegalStateException("En producción el correo debe usar el proveedor azure.");
    }
    if (!DEMO.equals(selected) && !AZURE.equals(selected)) {
      throw new IllegalStateException("Proveedor de correo no válido: " + selected);
    }
    if (AZURE.equals(selected) && (isBlank(acsConnectionString) || isBlank(sender))) {
      throw new IllegalStateException(
          "Con el proveedor azure se requieren RINDE_ACS_CONNECTION_STRING y RINDE_MAIL_SENDER.");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
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

  /** Nunca muestra la cadena de conexión: solo el proveedor elegido. */
  @Override
  public String toString() {
    return "MailProperties[provider=" + provider + ", acsConnectionString=****]";
  }
}
