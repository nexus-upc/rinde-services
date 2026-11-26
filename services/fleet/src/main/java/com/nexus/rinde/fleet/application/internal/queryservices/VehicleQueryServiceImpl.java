package com.nexus.rinde.fleet.application.internal.queryservices;

import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalIncidentSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.ExternalTripSummary;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.IncidentHistoryClient;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.RemoteLookupResult;
import com.nexus.rinde.fleet.application.internal.outboundservices.acl.TripHistoryClient;
import com.nexus.rinde.fleet.domain.model.aggregates.Vehicle;
import com.nexus.rinde.fleet.domain.model.entities.Maintenance;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHealthStatusQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetVehicleHistoryQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListVehiclesQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHealthStatus;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHistory;
import com.nexus.rinde.fleet.domain.model.valueobjects.VehicleHistory.VehicleHistoryItem;
import com.nexus.rinde.fleet.domain.services.VehicleQueryService;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.MaintenanceRepository;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio de aplicación de consultas para Vehicle. */
@Service
@Transactional(readOnly = true)
public class VehicleQueryServiceImpl implements VehicleQueryService {

  private static final int DEFAULT_HISTORY_DAYS = 90;

  private final VehicleRepository vehicleRepository;
  private final MaintenanceRepository maintenanceRepository;
  private final TripHistoryClient tripHistoryClient;
  private final IncidentHistoryClient incidentHistoryClient;

  @Autowired
  public VehicleQueryServiceImpl(
      VehicleRepository vehicleRepository,
      MaintenanceRepository maintenanceRepository,
      TripHistoryClient tripHistoryClient,
      IncidentHistoryClient incidentHistoryClient) {
    this.vehicleRepository = vehicleRepository;
    this.maintenanceRepository = maintenanceRepository;
    this.tripHistoryClient = tripHistoryClient;
    this.incidentHistoryClient = incidentHistoryClient;
  }

  @Override
  public Optional<Vehicle> handle(GetVehicleByIdQuery query) {
    return vehicleRepository.findByIdAndTenantId(query.vehicleId(), query.tenantId());
  }

  @Override
  public List<Vehicle> handle(ListVehiclesQuery query) {
    return vehicleRepository.findByTenantId(query.tenantId());
  }

  @Override
  public long countByTenantId(UUID tenantId) {
    return vehicleRepository.countByTenantId(tenantId);
  }

  @Override
  public VehicleHealthStatus handle(GetVehicleHealthStatusQuery query) {
    Vehicle vehicle =
        vehicleRepository
            .findByIdAndTenantId(query.vehicleId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El vehículo no existe."));

    List<Maintenance> maintenances =
        maintenanceRepository.findByTenantIdAndVehicleId(query.tenantId(), query.vehicleId());

    return vehicle.evaluateHealth(LocalDate.now(), maintenances);
  }

  @Override
  public VehicleHistory handle(GetVehicleHistoryQuery query) {
    return handle(query, LocalDate.now(ZoneOffset.UTC));
  }

  VehicleHistory handle(GetVehicleHistoryQuery query, LocalDate todayUtc) {
    LocalDate parsedFrom = parseDate(query.from(), "La fecha de inicio");
    LocalDate parsedTo = parseDate(query.to(), "La fecha de fin");
    LocalDate toDate = parsedTo != null ? parsedTo : todayUtc;
    LocalDate fromDate = parsedFrom != null ? parsedFrom : toDate.minusDays(DEFAULT_HISTORY_DAYS);

    if (fromDate.isAfter(toDate)) {
      throw new BusinessRuleException("La fecha final no puede ser anterior a la inicial.");
    }

    Vehicle vehicle =
        vehicleRepository
            .findByIdAndTenantId(query.vehicleId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El vehículo no existe."));

    List<Maintenance> maintenances =
        maintenanceRepository.findByTenantIdAndVehicleId(query.tenantId(), query.vehicleId());
    LocalDate nextMaintenanceDate = resolveNextMaintenanceDate(maintenances);

    List<VehicleHistoryItem> items = new ArrayList<>();
    appendMaintenances(items, maintenances, fromDate, toDate);

    List<String> unavailableSources = new ArrayList<>();
    appendTrips(items, unavailableSources, query.tenantId(), vehicle.getId(), fromDate, toDate);
    appendIncidents(items, unavailableSources, query.tenantId(), vehicle.getId(), fromDate, toDate);

    items.sort(Comparator.comparing(VehicleHistoryItem::date).reversed());

    return new VehicleHistory(
        vehicle.getId(),
        nextMaintenanceDate,
        List.copyOf(items),
        !unavailableSources.isEmpty(),
        List.copyOf(unavailableSources));
  }

  private static LocalDate parseDate(String value, String label) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException ex) {
      throw new BusinessRuleException(label + " debe tener el formato AAAA-MM-DD.");
    }
  }

  private static LocalDate resolveNextMaintenanceDate(List<Maintenance> maintenances) {
    return maintenances.stream()
        .max(
            Comparator.comparing(Maintenance::getExecutionDate)
                .thenComparing(
                    Maintenance::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
        .map(Maintenance::getNextMaintenanceDate)
        .orElse(null);
  }

  private static void appendMaintenances(
      List<VehicleHistoryItem> items,
      List<Maintenance> maintenances,
      LocalDate fromDate,
      LocalDate toDate) {
    for (Maintenance maintenance : maintenances) {
      LocalDate executionDate = maintenance.getExecutionDate();
      if (!isWithinRange(executionDate, fromDate, toDate)) {
        continue;
      }
      items.add(
          new VehicleHistoryItem(
              "MAINTENANCE",
              maintenance.getId(),
              executionDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
              "Mantenimiento " + maintenance.getMaintenanceType(),
              null,
              maintenance.getNotes()));
    }
  }

  private void appendTrips(
      List<VehicleHistoryItem> items,
      List<String> unavailableSources,
      UUID tenantId,
      UUID vehicleId,
      LocalDate fromDate,
      LocalDate toDate) {
    RemoteLookupResult<List<ExternalTripSummary>> result =
        tripHistoryClient.findByTenantAndVehicle(tenantId, vehicleId);
    if (!result.available()) {
      unavailableSources.add("TRIP");
      return;
    }
    if (result.data() == null) {
      return;
    }
    for (ExternalTripSummary trip : result.data()) {
      LocalDate tripDate = resolveTripDate(trip);
      if (tripDate == null || !isWithinRange(tripDate, fromDate, toDate)) {
        continue;
      }
      Instant eventDate = resolveTripInstant(trip, tripDate);
      items.add(
          new VehicleHistoryItem(
              "TRIP",
              trip.tripId(),
              eventDate,
              buildTripTitle(trip),
              trip.status(),
              trip.cargoDescription()));
    }
  }

  private void appendIncidents(
      List<VehicleHistoryItem> items,
      List<String> unavailableSources,
      UUID tenantId,
      UUID vehicleId,
      LocalDate fromDate,
      LocalDate toDate) {
    RemoteLookupResult<List<ExternalIncidentSummary>> result =
        incidentHistoryClient.findByTenantAndVehicle(tenantId, vehicleId);
    if (!result.available()) {
      unavailableSources.add("INCIDENT");
      return;
    }
    if (result.data() == null) {
      return;
    }
    for (ExternalIncidentSummary incident : result.data()) {
      Instant reportedAt = incident.reportedAt();
      if (reportedAt == null) {
        continue;
      }
      LocalDate incidentDate = LocalDate.ofInstant(reportedAt, ZoneOffset.UTC);
      if (!isWithinRange(incidentDate, fromDate, toDate)) {
        continue;
      }
      items.add(
          new VehicleHistoryItem(
              "INCIDENT",
              incident.id(),
              reportedAt,
              "Incidencia " + incident.type(),
              incident.status(),
              incident.description()));
    }
  }

  private static LocalDate resolveTripDate(ExternalTripSummary trip) {
    if (trip.departureDate() != null) {
      return trip.departureDate();
    }
    Instant fallback = trip.startedAt() != null ? trip.startedAt() : trip.finishedAt();
    return fallback != null ? LocalDate.ofInstant(fallback, ZoneOffset.UTC) : null;
  }

  private static Instant resolveTripInstant(ExternalTripSummary trip, LocalDate tripDate) {
    if (trip.startedAt() != null) {
      return trip.startedAt();
    }
    if (trip.finishedAt() != null) {
      return trip.finishedAt();
    }
    return tripDate.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static String buildTripTitle(ExternalTripSummary trip) {
    String code = trip.code() != null ? trip.code().trim() : "";
    String origin = trip.origin() != null ? trip.origin().trim() : "";
    String destination = trip.destination() != null ? trip.destination().trim() : "";
    if (origin.isEmpty() && destination.isEmpty()) {
      return code;
    }
    return (code + " " + origin + " a " + destination).trim();
  }

  private static boolean isWithinRange(LocalDate date, LocalDate fromDate, LocalDate toDate) {
    return !date.isBefore(fromDate) && !date.isAfter(toDate);
  }
}
