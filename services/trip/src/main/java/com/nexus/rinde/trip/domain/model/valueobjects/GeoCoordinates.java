package com.nexus.rinde.trip.domain.model.valueobjects;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Coordenadas geográficas validadas y normalizadas a seis decimales. */
public record GeoCoordinates(
    BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters) {

  private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
  private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
  private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
  private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

  public GeoCoordinates {
    if (latitude == null
        || latitude.compareTo(MIN_LATITUDE) < 0
        || latitude.compareTo(MAX_LATITUDE) > 0) {
      throw new BusinessRuleException("La latitud debe estar entre -90 y 90.");
    }
    if (longitude == null
        || longitude.compareTo(MIN_LONGITUDE) < 0
        || longitude.compareTo(MAX_LONGITUDE) > 0) {
      throw new BusinessRuleException("La longitud debe estar entre -180 y 180.");
    }
    if (accuracyMeters != null && accuracyMeters.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessRuleException("La precisión debe ser mayor o igual a 0.");
    }
    latitude = latitude.setScale(6, RoundingMode.HALF_UP);
    longitude = longitude.setScale(6, RoundingMode.HALF_UP);
    accuracyMeters =
        accuracyMeters == null ? null : accuracyMeters.setScale(2, RoundingMode.HALF_UP);
  }
}
