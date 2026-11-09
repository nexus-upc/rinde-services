package com.nexus.rinde.fleet.domain.services;

import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.queries.CheckDriverEligibilityQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetDriverByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetDriverByUserIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListDriversQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.DriverEligibility;
import java.util.List;
import java.util.Optional;

/** Servicio de aplicación para consultas sobre el agregado Driver. */
public interface DriverQueryService {

  Optional<Driver> handle(GetDriverByIdQuery query);

  Optional<Driver> handle(GetDriverByUserIdQuery query);

  List<Driver> handle(ListDriversQuery query);

  DriverEligibility handle(CheckDriverEligibilityQuery query);
}
