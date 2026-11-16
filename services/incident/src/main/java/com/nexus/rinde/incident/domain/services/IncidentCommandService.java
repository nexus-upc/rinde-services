package com.nexus.rinde.incident.domain.services;

import com.nexus.rinde.incident.domain.model.aggregates.Incident;
import com.nexus.rinde.incident.domain.model.commands.CloseIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.ReportIncidentCommand;
import com.nexus.rinde.incident.domain.model.commands.StartIncidentAttentionCommand;
import com.nexus.rinde.incident.domain.model.valueobjects.ReportOutcome;

/** Operaciones que cambian el estado de una incidencia. */
public interface IncidentCommandService {

  /** Registra la incidencia, o devuelve la existente si la clave de idempotencia ya se usó. */
  ReportOutcome report(ReportIncidentCommand command);

  Incident startAttention(StartIncidentAttentionCommand command);

  Incident close(CloseIncidentCommand command);
}
