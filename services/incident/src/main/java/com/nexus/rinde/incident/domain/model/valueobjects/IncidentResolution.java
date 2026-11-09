package com.nexus.rinde.incident.domain.model.valueobjects;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.UUID;

/** Resolución de la incidencia: la acción tomada y quién y cuándo la cerró. */
@Embeddable
public class IncidentResolution {

  private String action;
  private UUID closedBy;
  private Instant closedAt;

  protected IncidentResolution() {}

  public IncidentResolution(String action, UUID closedBy, Instant closedAt) {
    this.action = action;
    this.closedBy = closedBy;
    this.closedAt = closedAt;
  }

  public String getAction() {
    return action;
  }

  public UUID getClosedBy() {
    return closedBy;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
