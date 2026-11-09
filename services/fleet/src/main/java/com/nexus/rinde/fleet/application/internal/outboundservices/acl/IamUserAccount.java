package com.nexus.rinde.fleet.application.internal.outboundservices.acl;

import java.util.UUID;

/** Datos de una cuenta de IAM que Fleet necesita para validar el vínculo con un conductor. */
public record IamUserAccount(UUID userId, String role) {}
