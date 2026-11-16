package com.nexus.rinde.iam.demorequest.domain;

import java.time.Instant;
import java.util.UUID;

/** Solicitud de demostración enviada desde la landing. El correo se guarda en minúsculas. */
public record DemoRequest(
    UUID id,
    String fullName,
    String companyName,
    String email,
    String phone,
    int fleetSize,
    String message,
    DemoRequestStatus status,
    Instant receivedAt) {}
