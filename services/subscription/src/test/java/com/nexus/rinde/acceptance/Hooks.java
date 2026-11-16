package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.FleetStub;
import com.nexus.rinde.support.IamStub;
import com.nexus.rinde.support.TenantAccounts;
import com.nexus.rinde.support.TestDatabase;
import io.cucumber.java.Before;

/**
 * Deja la base, las cuentas y los dobles de IAM y Fleet limpios antes de cada escenario, para poder
 * repetir las pruebas sin datos previos.
 */
public class Hooks {

  private final TestDatabase testDatabase;
  private final CapturedEvents capturedEvents;
  private final TenantAccounts tenantAccounts;
  private final IamStub iamStub;
  private final FleetStub fleetStub;

  public Hooks(
      TestDatabase testDatabase,
      CapturedEvents capturedEvents,
      TenantAccounts tenantAccounts,
      IamStub iamStub,
      FleetStub fleetStub) {
    this.testDatabase = testDatabase;
    this.capturedEvents = capturedEvents;
    this.tenantAccounts = tenantAccounts;
    this.iamStub = iamStub;
    this.fleetStub = fleetStub;
  }

  @Before
  public void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    tenantAccounts.reset();
    iamStub.reset();
    fleetStub.reset();
  }
}
