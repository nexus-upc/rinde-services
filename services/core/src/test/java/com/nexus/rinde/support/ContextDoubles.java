package com.nexus.rinde.support;

import com.nexus.rinde.fleet.interfaces.acl.FleetContextFacade;
import com.nexus.rinde.fleet.interfaces.acl.FleetDriverSummary;
import com.nexus.rinde.fleet.interfaces.acl.FleetVehicleSummary;
import com.nexus.rinde.iam.interfaces.acl.IamContextFacade;
import com.nexus.rinde.trip.interfaces.acl.TripContextFacade;
import com.nexus.rinde.trip.interfaces.acl.TripSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

/**
 * Doble en memoria de IAM, Trip y Fleet. En producción core los consulta por HTTP; las pruebas
 * cargan aquí la empresa, la flota y los viajes que cada escenario necesita.
 */
@Component
public class ContextDoubles implements IamContextFacade, TripContextFacade, FleetContextFacade {

  /** Cuenta de usuario con la que se emite un token de prueba. */
  public record Account(UUID userId, UUID tenantId, String role, String email) {}

  private final Map<UUID, String> tenantStatuses = new ConcurrentHashMap<>();
  private final Map<UUID, String> administratorEmails = new ConcurrentHashMap<>();
  private final Map<String, Account> accounts = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, TripSummary>> trips = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> tripTenants = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, FleetVehicleSummary>> vehicles = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, FleetDriverSummary>> drivers = new ConcurrentHashMap<>();

  public void reset() {
    tenantStatuses.clear();
    administratorEmails.clear();
    accounts.clear();
    trips.clear();
    tripTenants.clear();
    vehicles.clear();
    drivers.clear();
  }

  // Carga de datos de prueba

  public void registerTenant(UUID tenantId, String administratorEmail) {
    tenantStatuses.put(tenantId, "ACTIVE");
    administratorEmails.put(tenantId, administratorEmail);
  }

  public void addAccount(Account account) {
    accounts.put(account.email(), account);
  }

  public Optional<Account> accountByEmail(String email) {
    return Optional.ofNullable(accounts.get(email));
  }

  public void addVehicle(UUID tenantId, UUID vehicleId, String plate, String brand, String model) {
    tenantMap(vehicles, tenantId)
        .put(vehicleId, new FleetVehicleSummary(vehicleId, plate, brand, model, "AVAILABLE"));
  }

  public void addDriver(
      UUID tenantId, UUID driverId, String fullName, String license, String category) {
    tenantMap(drivers, tenantId)
        .put(driverId, new FleetDriverSummary(driverId, fullName, license, category, "ENABLED"));
  }

  public void scheduleTrip(
      UUID tenantId,
      UUID tripId,
      String origin,
      String destination,
      String cargo,
      LocalDate departureDate) {
    String code = "TRP-" + String.format("%06d", tenantMap(trips, tenantId).size() + 1);
    tenantMap(trips, tenantId)
        .put(
            tripId,
            new TripSummary(
                tripId, code, "SCHEDULED", origin, destination, cargo, null, departureDate, null,
                null, null, null));
    tripTenants.put(tripId, tenantId);
  }

  public void assignTrip(UUID tripId, UUID vehicleId, UUID driverId) {
    updateTrip(tripId, trip -> withStatus(trip, "ASSIGNED", vehicleId, driverId, null, null));
  }

  public void startTrip(UUID tripId, Instant at) {
    updateTrip(
        tripId,
        trip -> withStatus(trip, "IN_ROUTE", trip.vehicleId(), trip.driverId(), at, null));
  }

  public void finishTrip(UUID tripId, Instant at) {
    updateTrip(
        tripId,
        trip ->
            withStatus(trip, "FINISHED", trip.vehicleId(), trip.driverId(), trip.startedAt(), at));
  }

  public Optional<TripSummary> trip(UUID tripId) {
    return Optional.ofNullable(tripTenants.get(tripId))
        .flatMap(tenantId -> findByIdAndTenantId(tripId, tenantId));
  }

  public UUID tenantOfTrip(UUID tripId) {
    return tripTenants.get(tripId);
  }

  // Fachadas consultadas por core

  @Override
  public Optional<String> findTenantStatus(UUID tenantId) {
    return Optional.ofNullable(tenantStatuses.get(tenantId));
  }

  @Override
  public Optional<String> findAdministratorEmail(UUID tenantId) {
    return Optional.ofNullable(administratorEmails.get(tenantId));
  }

  @Override
  public Optional<TripSummary> findByIdAndTenantId(UUID tripId, UUID tenantId) {
    return Optional.ofNullable(trips.getOrDefault(tenantId, Map.of()).get(tripId));
  }

  @Override
  public List<TripSummary> findByTenantId(UUID tenantId) {
    return List.copyOf(trips.getOrDefault(tenantId, Map.of()).values());
  }

  @Override
  public long countVehiclesByTenantId(UUID tenantId) {
    return vehicles.getOrDefault(tenantId, Map.of()).size();
  }

  @Override
  public List<FleetVehicleSummary> findVehiclesByTenantId(UUID tenantId) {
    return List.copyOf(vehicles.getOrDefault(tenantId, Map.of()).values());
  }

  @Override
  public List<FleetDriverSummary> findDriversByTenantId(UUID tenantId) {
    return List.copyOf(drivers.getOrDefault(tenantId, Map.of()).values());
  }

  private void updateTrip(UUID tripId, UnaryOperator<TripSummary> change) {
    tenantMap(trips, tripTenants.get(tripId)).computeIfPresent(tripId, (id, trip) -> change.apply(trip));
  }

  private static TripSummary withStatus(
      TripSummary trip,
      String status,
      UUID vehicleId,
      UUID driverId,
      Instant startedAt,
      Instant finishedAt) {
    return new TripSummary(
        trip.id(), trip.code(), status, trip.origin(), trip.destination(), trip.cargoDescription(),
        trip.cargoWeightKg(), trip.departureDate(), vehicleId, driverId, startedAt, finishedAt);
  }

  private static <T> Map<UUID, T> tenantMap(Map<UUID, Map<UUID, T>> byTenant, UUID tenantId) {
    return byTenant.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>());
  }
}
