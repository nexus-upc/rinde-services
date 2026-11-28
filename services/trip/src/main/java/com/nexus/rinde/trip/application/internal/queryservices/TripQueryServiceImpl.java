package com.nexus.rinde.trip.application.internal.queryservices;

import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import com.nexus.rinde.trip.domain.model.aggregates.Trip;
import com.nexus.rinde.trip.domain.model.entities.TripLocation;
import com.nexus.rinde.trip.domain.model.queries.GetLatestTripLocationsQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripByIdQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripsAssignedToMeQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripsByTenantQuery;
import com.nexus.rinde.trip.domain.model.valueobjects.TripStatus;
import com.nexus.rinde.trip.domain.services.FleetAvailabilityService;
import com.nexus.rinde.trip.domain.services.LatestTripLocation;
import com.nexus.rinde.trip.domain.services.TripQueryService;
import com.nexus.rinde.trip.infrastructure.persistence.jpa.repositories.TripLocationRepository;
import com.nexus.rinde.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta viajes solo dentro de la empresa y limita al conductor a sus propios viajes. */
@Service
public class TripQueryServiceImpl implements TripQueryService {

  private final TripRepository tripRepository;
  private final TripLocationRepository tripLocationRepository;
  private final FleetAvailabilityService fleetAvailabilityService;

  public TripQueryServiceImpl(
      TripRepository tripRepository,
      TripLocationRepository tripLocationRepository,
      FleetAvailabilityService fleetAvailabilityService) {
    this.tripRepository = tripRepository;
    this.tripLocationRepository = tripLocationRepository;
    this.fleetAvailabilityService = fleetAvailabilityService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Trip> handle(GetTripsByTenantQuery query) {
    if (query.statuses() == null || query.statuses().isEmpty()) {
      return tripRepository.findByTenantId(query.tenantId());
    }
    return tripRepository.findByTenantIdAndStatusIn(query.tenantId(), query.statuses());
  }

  @Override
  @Transactional(readOnly = true)
  public Trip handle(GetTripByIdQuery query) {
    Trip trip =
        tripRepository
            .findByIdAndTenantId(query.tripId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));
    if ("DRIVER".equals(query.viewerRole())) {
      fleetAvailabilityService
          .findDriverIdByUser(query.tenantId(), query.viewerUserId())
          .filter(trip::isAssignedTo)
          .orElseThrow(() -> new ResourceNotFoundException("El viaje no existe."));
    }
    return trip;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Trip> handle(GetTripsAssignedToMeQuery query) {
    return fleetAvailabilityService
        .findDriverIdByUser(query.tenantId(), query.userId())
        .map(driverId -> tripRepository.findByTenantAndDriver(query.tenantId(), driverId))
        .orElseGet(List::of);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LatestTripLocation> handle(GetLatestTripLocationsQuery query) {
    List<Trip> inRouteTrips =
        tripRepository.findByTenantIdAndStatusIn(query.tenantId(), Set.of(TripStatus.IN_ROUTE));
    if (inRouteTrips.isEmpty()) {
      return List.of();
    }
    List<Trip> sortedTrips =
        inRouteTrips.stream().sorted(Comparator.comparing(Trip::getCode)).toList();
    List<UUID> tripIds = sortedTrips.stream().map(Trip::getId).toList();
    Map<UUID, TripLocation> latestByTripId = new HashMap<>();
    for (TripLocation location :
        tripLocationRepository.findLatestByTenantIdAndTripIdIn(query.tenantId(), tripIds)) {
      latestByTripId.putIfAbsent(location.getTripId(), location);
    }
    return sortedTrips.stream()
        .map(trip -> toLatestLocation(trip, latestByTripId.get(trip.getId())))
        .toList();
  }

  private static LatestTripLocation toLatestLocation(Trip trip, TripLocation location) {
    UUID vehicleId = trip.getAssignment() != null ? trip.getAssignment().vehicleId() : null;
    if (location == null) {
      return new LatestTripLocation(
          trip.getId(), trip.getCode(), vehicleId, null, null, null, null);
    }
    return new LatestTripLocation(
        trip.getId(),
        trip.getCode(),
        vehicleId,
        location.getLatitude(),
        location.getLongitude(),
        location.getAccuracyMeters(),
        location.getRecordedAt());
  }
}
