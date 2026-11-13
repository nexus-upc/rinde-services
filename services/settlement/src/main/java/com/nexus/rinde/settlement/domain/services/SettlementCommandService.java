package com.nexus.rinde.settlement.domain.services;

import com.nexus.rinde.settlement.domain.model.aggregates.Settlement;
import com.nexus.rinde.settlement.domain.model.commands.CloseSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RecalculateSettlementCommand;
import com.nexus.rinde.settlement.domain.model.commands.RegisterAdvanceCommand;

/** Puerto de entrada para los comandos del contexto Settlement. */
public interface SettlementCommandService {

  Settlement handle(RegisterAdvanceCommand command);

  Settlement handle(RecalculateSettlementCommand command);

  Settlement handle(CloseSettlementCommand command);
}
