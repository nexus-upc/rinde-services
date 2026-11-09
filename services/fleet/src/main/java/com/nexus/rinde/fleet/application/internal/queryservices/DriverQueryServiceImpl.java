package com.nexus.rinde.fleet.application.internal.queryservices;

import com.nexus.rinde.fleet.domain.model.aggregates.Driver;
import com.nexus.rinde.fleet.domain.model.queries.CheckDriverEligibilityQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetDriverByIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.GetDriverByUserIdQuery;
import com.nexus.rinde.fleet.domain.model.queries.ListDriversQuery;
import com.nexus.rinde.fleet.domain.model.valueobjects.DriverEligibility;
import com.nexus.rinde.fleet.domain.services.DriverQueryService;
import com.nexus.rinde.fleet.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio de aplicación de consultas para Driver. */
@Service
@Transactional(readOnly = true)
public class DriverQueryServiceImpl implements DriverQueryService {

  private final DriverRepository driverRepository;

  public DriverQueryServiceImpl(DriverRepository driverRepository) {
    this.driverRepository = driverRepository;
  }

  @Override
  public Optional<Driver> handle(GetDriverByIdQuery query) {
    return driverRepository.findByIdAndTenantId(query.driverId(), query.tenantId());
  }

  @Override
  public Optional<Driver> handle(GetDriverByUserIdQuery query) {
    return driverRepository.findByTenantIdAndUserId(query.tenantId(), query.userId());
  }

  @Override
  public List<Driver> handle(ListDriversQuery query) {
    return driverRepository.findByTenantId(query.tenantId());
  }

  @Override
  public DriverEligibility handle(CheckDriverEligibilityQuery query) {
    Driver driver =
        driverRepository
            .findByIdAndTenantId(query.driverId(), query.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("El conductor no existe."));

    return driver.checkEligibility(LocalDate.now());
  }
}
