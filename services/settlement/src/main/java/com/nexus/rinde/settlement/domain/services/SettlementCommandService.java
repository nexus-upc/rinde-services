package com.nexus.rinde.settlement.domain.services;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.aggregates.TripAdvance;
import com.nexus.rinde.settlement.domain.model.commands.CloseSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RecalculateSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RegisterTripAdvanceCommand;
import com.nexus.rinde.settlement.domain.model.valueobjects.SettlementRecalculation;

/** Puerto de entrada para los comandos del contexto Settlement. */
public interface SettlementCommandService {

  TripAdvance handle(RegisterTripAdvanceCommand command);

  SettlementRecalculation handle(RecalculateSettlementCommand command);

  Settlement handle(CloseSettlementCommand command);
}
