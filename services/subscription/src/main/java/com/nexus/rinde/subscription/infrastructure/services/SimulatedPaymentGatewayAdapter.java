package com.nexus.rinde.subscription.infrastructure.services;

import com.nexus.rinde.subscription.domain.model.valueobjects.PaymentGatewaySession;
import com.nexus.rinde.subscription.domain.services.PaymentGatewayAdapter;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Crea referencias locales de prueba y nunca recibe ni persiste datos de tarjeta. */
@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayAdapter {

  @Override
  public PaymentGatewaySession createCheckout(BigDecimal amount) {
    return new PaymentGatewaySession("sim_" + UUID.randomUUID());
  }
}
