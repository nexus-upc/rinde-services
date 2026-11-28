package com.nexus.rinde.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resuelve tokens HOY[+-N] y AHORA[+-N(s|m|h)] respecto de la fecha y hora actuales. */
public final class RelativeDates {

  private static final Pattern DATE_TOKEN = Pattern.compile("(?<![A-Za-z])HOY(?:([+-])(\\d+))?");
  private static final Pattern INSTANT_TOKEN =
      Pattern.compile("(?<![A-Za-z])AHORA(?:([+-])(\\d+)([smh]))?");

  private RelativeDates() {}

  /** Reemplaza cada token de fecha o instante por su valor ISO. */
  public static String resolve(String text) {
    return resolve(text, Instant.now().truncatedTo(ChronoUnit.SECONDS));
  }

  /** Reemplaza tokens tomando un instante base fijo dentro de un escenario o prueba. */
  public static String resolve(String text, Instant referenceNow) {
    if (text == null) {
      return null;
    }
    String withInstants = replaceInstantTokens(text, referenceNow);
    return replaceDateTokens(withInstants);
  }

  /** Devuelve la fecha que representa el texto, con tokens o en formato ISO. */
  public static LocalDate date(String text) {
    return LocalDate.parse(resolve(text));
  }

  /** Devuelve el instante que representa el texto, por ejemplo "HOY+20T00:00:00Z" o "AHORA-10m". */
  public static Instant instant(String text) {
    return Instant.parse(resolve(text));
  }

  /** Devuelve el instante relativo respecto a un instante de referencia. */
  public static Instant instant(String text, Instant referenceNow) {
    return Instant.parse(resolve(text, referenceNow));
  }

  private static String replaceInstantTokens(String text, Instant referenceNow) {
    Instant base = referenceNow.truncatedTo(ChronoUnit.MICROS);
    Matcher matcher = INSTANT_TOKEN.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      Instant resolved = base;
      if (matcher.group(1) != null) {
        long amount = Long.parseLong(matcher.group(2));
        if ("-".equals(matcher.group(1))) {
          amount = -amount;
        }
        ChronoUnit unit =
            switch (matcher.group(3)) {
              case "s" -> ChronoUnit.SECONDS;
              case "m" -> ChronoUnit.MINUTES;
              case "h" -> ChronoUnit.HOURS;
              default -> throw new IllegalArgumentException("Unidad no soportada: " + matcher.group(3));
            };
        resolved = base.plus(amount, unit);
      }
      matcher.appendReplacement(result, resolved.toString());
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static String replaceDateTokens(String text) {
    LocalDate today = LocalDate.now();
    Matcher matcher = DATE_TOKEN.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      long days = matcher.group(1) == null ? 0 : Long.parseLong(matcher.group(2));
      if ("-".equals(matcher.group(1))) {
        days = -days;
      }
      matcher.appendReplacement(result, today.plusDays(days).toString());
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
