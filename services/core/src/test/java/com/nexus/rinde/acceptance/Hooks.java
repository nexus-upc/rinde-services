package com.nexus.rinde.acceptance;

import com.nexus.rinde.support.CapturedEvents;
import com.nexus.rinde.support.ContextDoubles;
import com.nexus.rinde.support.ExpenseDouble;
import com.nexus.rinde.support.IncidentDouble;
import com.nexus.rinde.support.TestDatabase;
import io.cucumber.java.Before;

/**
 * Deja la base y los dobles de los contextos separados limpios antes de cada escenario, para
 * poder repetir las pruebas sin datos previos.
 */
public class Hooks {

  private final TestDatabase testDatabase;
  private final CapturedEvents capturedEvents;
  private final ContextDoubles contextDoubles;
  private final ExpenseDouble expenseDouble;
  private final IncidentDouble incidentDouble;

  public Hooks(
      TestDatabase testDatabase,
      CapturedEvents capturedEvents,
      ContextDoubles contextDoubles,
      ExpenseDouble expenseDouble,
      IncidentDouble incidentDouble) {
    this.testDatabase = testDatabase;
    this.capturedEvents = capturedEvents;
    this.contextDoubles = contextDoubles;
    this.expenseDouble = expenseDouble;
    this.incidentDouble = incidentDouble;
  }

  @Before
  public void cleanData() {
    testDatabase.clean();
    capturedEvents.clear();
    contextDoubles.reset();
    expenseDouble.reset();
    incidentDouble.reset();
  }
}
