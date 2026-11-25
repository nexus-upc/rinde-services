package com.nexus.rinde.notification.interfaces.rest.transform;

import com.nexus.rinde.notification.domain.model.valueobjects.NotificationType;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Convierte el filtro CSV de tipos de aviso y rechaza valores fuera del contrato C6. */
public final class NotificationTypeFilterFromParameterAssembler {

  private NotificationTypeFilterFromParameterAssembler() {}

  public static Set<NotificationType> toTypes(String parameter) {
    if (parameter == null || parameter.isBlank()) {
      return Set.of();
    }
    try {
      return Arrays.stream(parameter.split(",", -1))
          .map(String::trim)
          .map(NotificationType::valueOf)
          .collect(Collectors.toUnmodifiableSet());
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException("El parámetro type contiene un tipo de aviso inválido.");
    }
  }
}
