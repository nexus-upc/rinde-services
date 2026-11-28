package com.nexus.rinde.trip.interfaces.rest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rechaza instantes de captura más de cinco minutos por delante del reloj del servidor. */
@Documented
@Constraint(validatedBy = NotFutureLocationTimeValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotFutureLocationTime {

  String message() default "La hora del punto no puede estar en el futuro.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
