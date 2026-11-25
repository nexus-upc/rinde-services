package com.nexus.rinde.dashboard.domain.services;

import com.nexus.rinde.dashboard.domain.model.queries.GetDashboardChartsQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetFleetStatusQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetOperationalMetricsQuery;
import com.nexus.rinde.dashboard.domain.model.queries.GetTripSummaryQuery;
import com.nexus.rinde.dashboard.interfaces.rest.resources.DashboardChartsResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.FleetStatusResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.OperationalMetricsResource;
import com.nexus.rinde.dashboard.interfaces.rest.resources.TripSummaryResource;

/** Servicio de consulta del dashboard operativo. */
public interface DashboardQueryService {

  TripSummaryResource handle(GetTripSummaryQuery query);

  FleetStatusResource handle(GetFleetStatusQuery query);

  OperationalMetricsResource handle(GetOperationalMetricsQuery query);

  DashboardChartsResource handle(GetDashboardChartsQuery query);
}
