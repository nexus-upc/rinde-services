package com.nexus.rinde.support;

import com.nexus.rinde.configuration.remote.RemoteServiceUnavailableException;
import com.nexus.rinde.incident.interfaces.acl.IncidentContextFacade;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Doble en memoria de Incident: las incidencias abiertas de cada empresa. Equivale al campo count
 * de GET .../open-count. Puede simular que el servicio no responde, para comprobar la respuesta
 * parcial del Dashboard.
 */
@Component
public class IncidentDouble implements IncidentContextFacade {

  private final Map<UUID, Long> openCounts = new ConcurrentHashMap<>();
  private volatile boolean unavailable;

  public void reset() {
    openCounts.clear();
    unavailable = false;
  }

  public void setOpenCount(UUID tenantId, long count) {
    openCounts.put(tenantId, count);
  }

  public void makeUnavailable() {
    unavailable = true;
  }

  @Override
  public long countOpenByTenantId(UUID tenantId) {
    if (unavailable) {
      throw new RemoteServiceUnavailableException("incidencias", null);
    }
    return openCounts.getOrDefault(tenantId, 0L);
  }
}
