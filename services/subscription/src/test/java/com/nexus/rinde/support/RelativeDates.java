package com.nexus.rinde.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resuelve en los escenarios los tokens HOY, HOY+N y HOY-N respecto de la fecha actual. */
public final class RelativeDates {

  private static final Pattern TOKEN = Pattern.compile("(?<![A-Za-z])HOY(?:([+-])(\\d+))?");

  private RelativeDates() {}

  /** Reemplaza cada token por su fecha ISO. El texto sin tokens se devuelve igual. */
  public static String resolve(String text) {
    if (text == null) {
      return null;
    }
    LocalDate today = LocalDate.now();
    Matcher matcher = TOKEN.matcher(text);
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

  /** Devuelve la fecha que representa el texto, con tokens o en formato ISO. */
  public static LocalDate date(String text) {
    return LocalDate.parse(resolve(text));
  }

  /** Devuelve el instante que representa el texto, por ejemplo "HOY+20T00:00:00Z". */
  public static Instant instant(String text) {
    return Instant.parse(resolve(text));
  }
}
