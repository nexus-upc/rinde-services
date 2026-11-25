package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

/** Resultado de consulta HTTP tolerante a indisponibilidad del servicio remoto. */
public record RemoteLookupResult<T>(boolean available, T data) {

  public static <T> RemoteLookupResult<T> available(T data) {
    return new RemoteLookupResult<>(true, data);
  }

  public static <T> RemoteLookupResult<T> unavailable() {
    return new RemoteLookupResult<>(false, null);
  }
}
