package com.nexus.rinde.trip.domain.services;

import com.nexus.rinde.trip.domain.model.aggregates.Trip;
import com.nexus.rinde.trip.domain.model.queries.GetLatestTripLocationsQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripByIdQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripsAssignedToMeQuery;
import com.nexus.rinde.trip.domain.model.queries.GetTripsByTenantQuery;
import java.util.List;

/** Consultas de viajes, siempre filtradas por la empresa autenticada. */
public interface TripQueryService {

  List<Trip> handle(GetTripsByTenantQuery query);

  Trip handle(GetTripByIdQuery query);

  List<Trip> handle(GetTripsAssignedToMeQuery query);

  List<LatestTripLocation> handle(GetLatestTripLocationsQuery query);
}
