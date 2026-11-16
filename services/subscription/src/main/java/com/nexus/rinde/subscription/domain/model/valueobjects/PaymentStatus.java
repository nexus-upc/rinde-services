package com.nexus.rinde.subscription.domain.model.valueobjects;

/** Resultado registrado para un intento de pago procesado por la pasarela externa. */
public enum PaymentStatus {
  PENDING,
  CONFIRMED,
  REJECTED
}
