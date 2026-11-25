package com.nexus.rinde.support;

import com.nexus.rinde.configuration.remote.RemoteServiceUnavailableException;
import com.nexus.rinde.incident.interfaces.acl.IncidentContextFacade;
import com.nexus.rinde.incident.interfaces.acl.OpenIncidentSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Doble en memoria de Incident: las incidencias abiertas de cada empresa. Equivale al campo count
 * de GET .../open-count y a GET .../open. Puede simular que el servicio no responde, para comprobar
 * la respuesta parcial del Dashboard.
 */
@Component
public class IncidentDouble implements IncidentContextFacade {

  private final Map<UUID, Long> openCounts = new ConcurrentHashMap<>();
  private final Map<UUID, List<OpenIncidentSummary>> openIncidents = new ConcurrentHashMap<>();
  private volatile boolean unavailable;

  public void reset() {
    openCounts.clear();
    openIncidents.clear();
    unavailable = false;
  }

  public void setOpenCount(UUID tenantId, long count) {
    openCounts.put(tenantId, count);
  }

  public void addOpenIncident(UUID tenantId, OpenIncidentSummary incident) {
    openIncidents.computeIfAbsent(tenantId, id -> new CopyOnWriteArrayList<>()).add(incident);
  }

  public void makeUnavailable() {
    unavailable = true;
  }

  @Override
  public long countOpenByTenantId(UUID tenantId) {
    if (unavailable) {
      throw new RemoteServiceUnavailableException("incidencias", null);
    }
    if (openCounts.containsKey(tenantId)) {
      return openCounts.get(tenantId);
    }
    return openIncidents.getOrDefault(tenantId, List.of()).size();
  }

  @Override
  public List<OpenIncidentSummary> findOpenByTenantId(UUID tenantId) {
    if (unavailable) {
      throw new RemoteServiceUnavailableException("incidencias", null);
    }
    return List.copyOf(openIncidents.getOrDefault(tenantId, List.of()));
  }
}
