package com.nexus.rinde.trip.interfaces.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Valida que la captura no supere cinco minutos en el futuro respecto al Clock inyectado. */
@Component
public class NotFutureLocationTimeValidator
    implements ConstraintValidator<NotFutureLocationTime, Instant> {

  private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

  private final Clock clock;

  public NotFutureLocationTimeValidator() {
    this(Clock.systemUTC());
  }

  @Autowired
  public NotFutureLocationTimeValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean isValid(Instant value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return !value.isAfter(clock.instant().plus(MAX_CLOCK_SKEW));
  }
}
