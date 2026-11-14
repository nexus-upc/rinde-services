package com.nexus.rinde.subscription.domain.services;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentGatewaySession;
import java.math.BigDecimal;

/** Puerto hacia el procesador de pagos; la entrega universitaria usa un adaptador simulado. */
public interface PaymentGatewayAdapter {

  PaymentGatewaySession createCheckout(BigDecimal amount);
}
