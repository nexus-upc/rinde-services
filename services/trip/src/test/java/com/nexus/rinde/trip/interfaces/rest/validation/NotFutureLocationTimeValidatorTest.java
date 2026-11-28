package com.nexus.rinde.trip.interfaces.rest.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class NotFutureLocationTimeValidatorTest {

  private static final Instant NOW = Instant.parse("2026-11-28T14:30:00Z");
  private final NotFutureLocationTimeValidator validator =
      new NotFutureLocationTimeValidator(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void acceptsPastPresentAndUpToFiveMinutesAhead() {
    assertThat(validator.isValid(NOW.minusSeconds(600), null)).isTrue();
    assertThat(validator.isValid(NOW, null)).isTrue();
    assertThat(validator.isValid(NOW.plusSeconds(300), null)).isTrue();
    assertThat(validator.isValid(null, null)).isTrue();
  }

  @Test
  void rejectsTimestampsMoreThanFiveMinutesInTheFuture() {
    assertThat(validator.isValid(NOW.plusSeconds(301), null)).isFalse();
  }
}
