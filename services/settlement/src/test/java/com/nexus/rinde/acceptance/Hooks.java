package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.ExternalStubs;
import com.nexus.rinde.support.TestDatabase;
import io.cucumber.java.Before;

/** Deja la base y los stubs limpios antes de cada escenario para poder repetir las pruebas. */
public class Hooks {

  private final TestDatabase testDatabase;
  private final CapturedEvents capturedEvents;
  private final ExternalStubs externalStubs;

  public Hooks(TestDatabase testDatabase, CapturedEvents capturedEvents, ExternalStubs externalStubs) {
    this.testDatabase = testDatabase;
    this.capturedEvents = capturedEvents;
    this.externalStubs = externalStubs;
  }

  @Before
  public void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    externalStubs.reset();
  }
}
