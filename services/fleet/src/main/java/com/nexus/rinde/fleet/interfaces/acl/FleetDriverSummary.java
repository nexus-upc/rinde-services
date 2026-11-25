package com.nexus.rinde.fleet.interfaces.acl;

import java.util.UUID;

/** Vista de un conductor para otros contextos. El estado viaja como nombre del enum. */
public record FleetDriverSummary(
    UUID id,
    UUID userId,
    String fullName,
    String licenseNumber,
    String licenseCategory,
    String status) {}
