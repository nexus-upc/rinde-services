package com.nexus.rinde.trip.domain.services;

import com.nexus.rinde.trip.domain.model.aggregates.Trip;
import com.nexus.rinde.trip.domain.model.commands.AssignTripCommand;
import com.nexus.rinde.trip.domain.model.commands.FinishTripCommand;
import com.nexus.rinde.trip.domain.model.commands.RecordTripLocationsCommand;
import com.nexus.rinde.trip.domain.model.commands.ScheduleTripCommand;
import com.nexus.rinde.trip.domain.model.commands.StartTripCommand;

/** Casos de uso que modifican el ciclo de vida de los viajes. */
public interface TripCommandService {

  Trip handle(ScheduleTripCommand command);

  TripAssignmentResult handle(AssignTripCommand command);

  Trip handle(StartTripCommand command);

  Trip handle(FinishTripCommand command);

  TripLocationBatchResult handle(RecordTripLocationsCommand command);
}
