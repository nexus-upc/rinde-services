package com.nexus.rinde.trip.infrastructure.persistence.jpa.repositories;

import com.nexus.rinde.trip.domain.model.entities.TripLocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositorio de ubicaciones de viajes; todas las consultas filtran por empresa. */
@Repository
public interface TripLocationRepository extends JpaRepository<TripLocation, UUID> {

  @Modifying
  @Query(
      value =
          "insert into trip.trip_locations "
              + "(id, tenant_id, trip_id, latitude, longitude, accuracy_meters, recorded_at, received_at) "
              + "values (:id, :tenantId, :tripId, :latitude, :longitude, :accuracyMeters, :recordedAt, :receivedAt) "
              + "on conflict (trip_id, recorded_at) do nothing",
      nativeQuery = true)
  int insertIgnoringDuplicate(
      @Param("id") UUID id,
      @Param("tenantId") UUID tenantId,
      @Param("tripId") UUID tripId,
      @Param("latitude") BigDecimal latitude,
      @Param("longitude") BigDecimal longitude,
      @Param("accuracyMeters") BigDecimal accuracyMeters,
      @Param("recordedAt") Instant recordedAt,
      @Param("receivedAt") Instant receivedAt);

  @Query(
      "select location from TripLocation location "
          + "where location.tenantId = :tenantId and location.tripId in :tripIds "
          + "and location.recordedAt = ("
          + "select max(latest.recordedAt) from TripLocation latest "
          + "where latest.tenantId = :tenantId and latest.tripId = location.tripId)")
  List<TripLocation> findLatestByTenantIdAndTripIdIn(
      @Param("tenantId") UUID tenantId, @Param("tripIds") Collection<UUID> tripIds);
}
