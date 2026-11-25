package com.nexus.rinde.expense.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.expense.domain.model.events.ExpenseRegistered;
import com.nexus.rinde.support.AbstractIntegrationTest;
import com.nexus.rinde.support.RelativeDates;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Comprueba que expense.registered se publique una vez por gasto nuevo y nunca en duplicados. */
class ExpenseRegisteredEventIntegrationTest extends AbstractIntegrationTest {

  @Test
  @DisplayName("Publica ExpenseRegistered al registrar un gasto")
  void publishesExpenseRegisteredOnDirectCreation() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    String token = tokens.issue(driverId, tenantId, "DRIVER", "luis@andes.pe");
    tripStub.setTripStatus(tenantId, tripId, "IN_ROUTE");

    String body =
        """
        {
          "category": "FUEL",
          "amount": 180.50,
          "currency": "PEN",
          "expenseDate": "%s",
          "idempotencyKey": "EXP-EVT-01"
        }
        """
            .formatted(RelativeDates.date("HOY"));

    mockMvc
        .perform(
            post("/api/v1/trips/{tripId}/expenses", tripId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    assertThat(capturedEvents.all(ExpenseRegistered.class))
        .singleElement()
        .satisfies(
            evt -> {
              assertThat(evt.tenantId()).isEqualTo(tenantId);
              assertThat(evt.tripId()).isEqualTo(tripId);
              assertThat(evt.driverId()).isEqualTo(driverId);
              assertThat(evt.amount()).isEqualByComparingTo(new BigDecimal("180.50"));
              assertThat(evt.currency()).isEqualTo("PEN");
              assertThat(evt.expenseId()).isNotNull();
              assertThat(evt.eventId()).isNotNull();
              assertThat(evt.occurredAt()).isNotNull();
            });
  }

  @Test
  @DisplayName("Publica ExpenseRegistered en sync nuevo y no repite en reenvío idempotente")
  void publishesOnceOnSyncAndIgnoresIdempotentReplay() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID tripId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    String token = tokens.issue(driverId, tenantId, "DRIVER", "luis@andes.pe");
    tripStub.setTripStatus(tenantId, tripId, "IN_ROUTE");

    String payload =
        """
        [
          {
            "tripId": "%s",
            "category": "TOLL",
            "amount": 25.00,
            "currency": "PEN",
            "expenseDate": "%s",
            "idempotencyKey": "SYNC-EVT-01"
          }
        ]
        """
            .formatted(tripId, RelativeDates.date("HOY"));

    mockMvc
        .perform(
            post("/api/v1/expenses/sync")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    assertThat(capturedEvents.all(ExpenseRegistered.class)).hasSize(1);
    capturedEvents.clear();

    mockMvc
        .perform(
            post("/api/v1/expenses/sync")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    assertThat(capturedEvents.all(ExpenseRegistered.class)).isEmpty();
  }
}
