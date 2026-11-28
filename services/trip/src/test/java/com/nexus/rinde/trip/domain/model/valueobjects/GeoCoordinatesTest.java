package com.nexus.rinde.trip.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GeoCoordinatesTest {

  @Test
  void acceptsBoundaryCoordinatesAndNormalizesScale() {
    GeoCoordinates southWest =
        new GeoCoordinates(new BigDecimal("-90"), new BigDecimal("-180"), BigDecimal.ZERO);
    GeoCoordinates northEast =
        new GeoCoordinates(new BigDecimal("90"), new BigDecimal("180"), null);

    assertThat(southWest.latitude()).isEqualByComparingTo("-90.000000");
    assertThat(southWest.latitude().scale()).isEqualTo(6);
    assertThat(southWest.longitude()).isEqualByComparingTo("-180.000000");
    assertThat(southWest.longitude().scale()).isEqualTo(6);
    assertThat(southWest.accuracyMeters()).isEqualByComparingTo("0.00");
    assertThat(southWest.accuracyMeters().scale()).isEqualTo(2);

    assertThat(northEast.latitude()).isEqualByComparingTo("90.000000");
    assertThat(northEast.longitude()).isEqualByComparingTo("180.000000");
    assertThat(northEast.accuracyMeters()).isNull();
  }

  @Test
  void roundsCoordinatesToSixDecimalsAndAccuracyToTwoDecimals() {
    GeoCoordinates coordinates =
        new GeoCoordinates(
            new BigDecimal("-12.0463748"),
            new BigDecimal("-77.0427934"),
            new BigDecimal("12.345"));

    assertThat(coordinates.latitude()).isEqualByComparingTo("-12.046375");
    assertThat(coordinates.longitude()).isEqualByComparingTo("-77.042793");
    assertThat(coordinates.accuracyMeters()).isEqualByComparingTo("12.35");
  }

  @Test
  void rejectsNullLatitude() {
    assertThatThrownBy(() -> new GeoCoordinates(null, new BigDecimal("-77.04"), BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La latitud debe estar entre -90 y 90.");
  }

  @Test
  void rejectsLatitudeOutsideValidRange() {
    assertThatThrownBy(
            () ->
                new GeoCoordinates(
                    new BigDecimal("-90.000001"), new BigDecimal("-77.04"), BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La latitud debe estar entre -90 y 90.");

    assertThatThrownBy(
            () ->
                new GeoCoordinates(
                    new BigDecimal("90.000001"), new BigDecimal("-77.04"), BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La latitud debe estar entre -90 y 90.");
  }

  @Test
  void rejectsNullLongitude() {
    assertThatThrownBy(() -> new GeoCoordinates(new BigDecimal("-12.04"), null, BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La longitud debe estar entre -180 y 180.");
  }

  @Test
  void rejectsLongitudeOutsideValidRange() {
    assertThatThrownBy(
            () ->
                new GeoCoordinates(
                    new BigDecimal("-12.04"), new BigDecimal("-180.000001"), BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La longitud debe estar entre -180 y 180.");

    assertThatThrownBy(
            () ->
                new GeoCoordinates(
                    new BigDecimal("-12.04"), new BigDecimal("180.000001"), BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La longitud debe estar entre -180 y 180.");
  }

  @Test
  void rejectsNegativeAccuracy() {
    assertThatThrownBy(
            () ->
                new GeoCoordinates(
                    new BigDecimal("-12.04"), new BigDecimal("-77.04"), new BigDecimal("-0.01")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La precisión debe ser mayor o igual a 0.");
  }
}
