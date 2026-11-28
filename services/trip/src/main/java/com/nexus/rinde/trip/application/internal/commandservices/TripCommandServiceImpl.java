package com.nexus.rinde.trip.application.internal.commandservices;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.exceptions.ConflictException;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.trip.domain.model.aggregates.Trip;
import com.nexus.rinde.trip.domain.model.commands.AssignTripCommand;
import com.nexus.rinde.trip.domain.model.commands.FinishTripCommand;
import com.nexus.rinde.trip.domain.model.commands.RecordTripLocationsCommand;
import com.nexus.rinde.trip.domain.model.commands.ScheduleTripCommand;
import com.nexus.rinde.trip.domain.model.commands.StartTripCommand;
import com.nexus.rinde.trip.domain.model.entities.TripLocation;
import com.nexus.rinde.trip.domain.model.valueobjects.Availability;
import com.nexus.rinde.trip.domain.model.valueobjects.Cargo;
import com.nexus.rinde.trip.domain.model.valueobjects.GeoCoordinates;
import com.nexus.rinde.trip.domain.model.valueobjects.MaintenanceState;
import com.nexus.rinde.trip.domain.model.valueobjects.Route;
import com.nexus.rinde.trip.domain.services.FleetAvailabilityService;
import com.nexus.rinde.trip.domain.services.TripAssignmentResult;
import com.nexus.rinde.trip.domain.services.TripCommandService;
import com.nexus.rinde.trip.domain.services.TripLocationBatchResult;
import com.nexus.rinde.trip.infrastructure.persistence.jpa.repositories.TripLocationRepository;
import com.nexus.rinde.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina las reglas del agregado con la persistencia y las fachadas de otros contextos. */
@Service
public class TripCommandServiceImpl implements TripCommandService {

  private static final int MAX_LOCATION_BATCH_SIZE = 50;
  private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);

  private final TripRepository tripRepository;
  private final TripLocationRepository tripLocationRepository;
  private final FleetAvailabilityService fleetAvailabilityService;
  private final Clock clock;

  public TripCommandServiceImpl(
      TripRepository tripRepository,
      TripLocationRepository tripLocationRepository,
      FleetAvailabilityService fleetAvailabilityService,
      Clock clock) {
    this.tripRepository = tripRepository;
    this.tripLocationRepository = tripLocationRepository;
    this.fleetAvailabilityService = fleetAvailabilityService;
    this.clock = clock;
  }

  @Override
  @Transactional
  public Trip handle(ScheduleTripCommand command) {
    Route route = new Route(command.origin(), command.destination());
    Cargo cargo = new Cargo(command.cargoDescription(), command.cargoWeightKg());
    Trip trip =
        Trip.schedule(
            command.tenantId(),
            tripRepository.nextCodeForTenant(command.tenantId()),
            route,
            cargo,
            command.departureDate(),
            command.confirmPastDate(),
            LocalDate.now(clock),
            clock.instant(),
            command.userId());
    return tripRepository.saveAndFlush(trip);
  }

  @Override
  @Transactional
  public TripAssignmentResult handle(AssignTripCommand command) {
    Trip trip = findTrip(command.tenantId(), command.tripId());
    trip.ensureAssignable();

    if (tripRepository.existsAssignmentOn(
        command.tenantId(),
        command.vehicleId(),
        trip.getDepartureDate(),
        true,
        trip.getId())) {
      throw new ConflictException("El vehículo ya está asignado a otro viaje en esa fecha.");
    }
    if (tripRepository.existsAssignmentOn(
        command.tenantId(),
        command.driverId(),
        trip.getDepartureDate(),
        false,
        trip.getId())) {
      throw new ConflictException("El conductor ya está asignado a otro viaje en esa fecha.");
    }

    Availability availability =
        fleetAvailabilityService.check(
            command.tenantId(), command.vehicleId(), command.driverId());
    trip.assign(
        command.vehicleId(),
        command.driverId(),
        availability,
        command.confirmOverdueMaintenance(),
        command.assignedBy(),
        clock.instant());
    tripRepository.saveAndFlush(trip);
    MaintenanceState alert =
        availability.maintenance() == MaintenanceState.DUE_SOON
            ? MaintenanceState.DUE_SOON
            : null;
    return new TripAssignmentResult(trip, alert);
  }

  @Override
  @Transactional
  public Trip handle(StartTripCommand command) {
    Trip trip = findAssignedTripForDriver(command.tenantId(), command.tripId(), command.userId());
    trip.start(command.userId(), clock.instant());
    return tripRepository.saveAndFlush(trip);
  }

  @Override
  @Transactional
  public Trip handle(FinishTripCommand command) {
    Trip trip = findAssignedTripForDriver(command.tenantId(), command.tripId(), command.userId());
    trip.finish(command.userId(), clock.instant());
    return tripRepository.saveAndFlush(trip);
  }

  @Override
  @Transactional
  public TripLocationBatchResult handle(RecordTripLocationsCommand command) {
    if (command == null
        || command.points() == null
        || command.points().isEmpty()
        || command.points().size() > MAX_LOCATION_BATCH_SIZE) {
      throw new BusinessRuleException("Debe enviar entre 1 y 50 puntos de ubicación.");
    }
    Trip trip = findAssignedTripForDriver(command.tenantId(), command.tripId(), command.userId());
    trip.ensureInRouteForLocation();

    Instant maxAllowedRecordedAt = clock.instant().plus(MAX_FUTURE_SKEW);
    Instant receivedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
    List<TripLocation> locations = new ArrayList<>(command.points().size());
    for (RecordTripLocationsCommand.LocationSample sample : command.points()) {
      if (sample == null || sample.recordedAt() == null) {
        throw new BusinessRuleException("Cada punto de ubicación debe incluir fecha de captura.");
      }
      if (sample.recordedAt().isAfter(maxAllowedRecordedAt)) {
        throw new BusinessRuleException("La hora del punto no puede estar en el futuro.");
      }
      GeoCoordinates coordinates =
          new GeoCoordinates(sample.latitude(), sample.longitude(), sample.accuracyMeters());
      locations.add(
          TripLocation.record(
              command.tenantId(), trip.getId(), coordinates, sample.recordedAt(), receivedAt));
    }

    int savedCount = 0;
    int duplicateCount = 0;
    for (TripLocation location : locations) {
      int inserted =
          tripLocationRepository.insertIgnoringDuplicate(
              location.getId(),
              location.getTenantId(),
              location.getTripId(),
              location.getLatitude(),
              location.getLongitude(),
              location.getAccuracyMeters(),
              location.getRecordedAt(),
              location.getReceivedAt());
      if (inserted > 0) {
        savedCount++;
      } else {
        duplicateCount++;
      }
    }
    return new TripLocationBatchResult(savedCount, duplicateCount);
  }

  private Trip findAssignedTripForDriver(UUID tenantId, UUID tripId, UUID userId) {
    Trip trip = findTrip(tenantId, tripId);
    fleetAvailabilityService
        .findDriverIdByUser(tenantId, userId)
        .filter(trip::isAssignedTo)
        .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));
    return trip;
  }

  private Trip findTrip(UUID tenantId, UUID tripId) {
    return tripRepository
        .findByIdAndTenantId(tripId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));
  }
}
