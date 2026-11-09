package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.IamStub;
import com.nexus.rinde.support.TestDatabase;
import io.cucumber.java.Before;

/** Deja la base limpia antes de cada escenario para poder repetir las pruebas sin datos previos. */
public class Hooks {

  private final TestDatabase testDatabase;
  private final CapturedEvents capturedEvents;
  private final IamStub iamStub;

  public Hooks(TestDatabase testDatabase, CapturedEvents capturedEvents, IamStub iamStub) {
    this.testDatabase = testDatabase;
    this.capturedEvents = capturedEvents;
    this.iamStub = iamStub;
  }

  @Before
  public void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    iamStub.reset();
  }
}
