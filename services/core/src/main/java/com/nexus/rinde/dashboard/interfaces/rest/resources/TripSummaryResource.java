package com.nexus.rinde.dashboard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Resumen consolidado de un viaje para el dashboard (US17). */
public record TripSummaryResource(
    UUID tripId,
    String code,
    String status,
    String origin,
    String destination,
    String cargoDescription,
    BigDecimal cargoWeightKg,
    LocalDate departureDate,
    UUID vehicleId,
    UUID driverId,
    Instant startedAt,
    Instant finishedAt,
    int expenseCount,
    BigDecimal expenseTotal,
    String expenseCurrency,
    String settlementStatus,
    BigDecimal advanceAmount,
    BigDecimal advanceBalance,
    @Schema(nullable = true, example = "ABC-123") String vehiclePlate,
    @Schema(nullable = true, example = "Luis Quispe Mamani") String driverName,
    Map<String, Integer> expenseCountsByStatus,
    BigDecimal approvedTotal,
    int pendingReviewCount,
    @Schema(nullable = true, example = "TRANSFER") String advanceDeliveryMethod,
    @Schema(nullable = true) UUID settlementId,
    @Schema(nullable = true) Instant settlementClosedAt,
    List<OpenIncidentItem> openIncidents,
    boolean partial,
    List<String> unavailableSources) {}
