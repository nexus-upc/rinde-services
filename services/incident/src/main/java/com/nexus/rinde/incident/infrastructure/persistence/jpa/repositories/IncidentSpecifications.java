package com.nexus.rinde.incident.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.valueobjects.IncidentStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales del listado. La empresa siempre es obligatoria. */
public final class IncidentSpecifications {

  private IncidentSpecifications() {}

  public static Specification<Incident> matching(
      UUID tenantId, IncidentStatus status, UUID tripId, UUID vehicleId) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("tenantId"), tenantId));
      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }
      if (tripId != null) {
        predicates.add(builder.equal(root.get("tripId"), tripId));
      }
      if (vehicleId != null) {
        predicates.add(builder.equal(root.get("vehicleId"), vehicleId));
      }
      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
