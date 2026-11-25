package com.nexus.rinde.settlement.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Vista de la liquidación de un viaje para otros contextos. El estado es el nombre del enum. */
public record SettlementSummary(
    UUID id, String status, BigDecimal advanceAmount, BigDecimal advanceBalance, Instant closedAt) {}
