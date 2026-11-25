package com.nexus.rinde.notification.interfaces.rest.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

class NotificationTypeFilterFromParameterAssemblerTest {

  @Test
  void parsesEmptyAndCommaSeparatedTypes() {
    assertThat(NotificationTypeFilterFromParameterAssembler.toTypes(null)).isEmpty();
    assertThat(NotificationTypeFilterFromParameterAssembler.toTypes("   ")).isEmpty();
    assertThat(
            NotificationTypeFilterFromParameterAssembler.toTypes(
                "EXPENSE_PENDING_REVIEW, INCIDENT_REPORTED"))
        .containsExactlyInAnyOrder(
            NotificationType.EXPENSE_PENDING_REVIEW, NotificationType.INCIDENT_REPORTED);
  }

  @Test
  void rejectsInvalidOrTrailingEmptyTypeValues() {
    assertThatThrownBy(
            () -> NotificationTypeFilterFromParameterAssembler.toTypes("EXPENSE_PENDING_REVIEW,FOO"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El parámetro type contiene un tipo de aviso inválido.");

    assertThatThrownBy(
            () -> NotificationTypeFilterFromParameterAssembler.toTypes("INCIDENT_REPORTED,"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El parámetro type contiene un tipo de aviso inválido.");
  }
}
