package com.nexus.rinde.settlement.interfaces.acl;

import java.math.BigDecimal;

/** Vista de la liquidación de un viaje para otros contextos. El estado es el nombre del enum. */
public record SettlementSummary(String status, BigDecimal advanceAmount, BigDecimal advanceBalance) {}
