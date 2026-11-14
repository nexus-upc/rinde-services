package com.nexus.rinde.subscription.domain.model.commands;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentStatus;
import java.util.UUID;

/** Resultado de prueba para un intento de pago que pertenece a la empresa autenticada. */
public record SimulatePaymentCommand(UUID tenantId, UUID subscriptionId, UUID paymentId, PaymentStatus status) {}
