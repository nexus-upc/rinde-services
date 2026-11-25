package com.nexus.rinde.notification.infrastructure.services;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del proveedor de notificaciones push para conductores. */
@ConfigurationProperties(prefix = "rinde.push")
public class PushProperties {

  private String provider = "unavailable";
  private FcmProperties fcm = new FcmProperties();

  @PostConstruct
  void validate() {
    String normalized = provider == null ? "unavailable" : provider.trim();
    if ("fcm".equals(normalized)) {
      String credentials = fcm == null ? null : fcm.getCredentialsJson();
      if (credentials == null || credentials.isBlank()) {
        throw new IllegalStateException(
            "Debe configurar rinde.push.fcm.credentials-json cuando rinde.push.provider es fcm.");
      }
      return;
    }
    if (!"unavailable".equals(normalized)) {
      throw new IllegalStateException("Proveedor push no válido: " + normalized);
    }
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public FcmProperties getFcm() {
    return fcm;
  }

  public void setFcm(FcmProperties fcm) {
    this.fcm = fcm;
  }

  /** Propiedades de credenciales para Firebase Cloud Messaging. */
  public static class FcmProperties {

    private String credentialsJson = "";

    public String getCredentialsJson() {
      return credentialsJson;
    }

    public void setCredentialsJson(String credentialsJson) {
      this.credentialsJson = credentialsJson;
    }
  }
}
