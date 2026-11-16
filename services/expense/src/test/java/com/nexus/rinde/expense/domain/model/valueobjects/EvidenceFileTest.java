package com.nexus.rinde.expense.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.expense.domain.exceptions.EvidenceTooLargeException;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.support.EvidenceFiles;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EvidenceFileTest {

  private static final long MAX_SIZE = 5L * 1024 * 1024;

  @ParameterizedTest(name = "{0} se reconoce como {1}")
  @CsvSource({"recibo.jpg,JPEG", "recibo.jpeg,JPEG", "foto.png,PNG", "foto.webp,WEBP", "factura.pdf,PDF"})
  @DisplayName("Acepta JPEG, PNG, WEBP y PDF por su firma inicial")
  void shouldAcceptAllowedFormats(String fileName, EvidenceFormat expected) {
    EvidenceFile file = EvidenceFile.inspect(fileName, EvidenceFiles.contentOf(fileName), MAX_SIZE);

    assertThat(file.format()).isEqualTo(expected);
    assertThat(file.fileName()).isEqualTo(fileName);
    assertThat(file.sizeBytes()).isEqualTo(EvidenceFiles.contentOf(fileName).length);
  }

  @Test
  @DisplayName("Rechaza un texto aunque tenga extensión de imagen")
  void shouldRejectUnsupportedFormat() {
    byte[] text = "MZ ejecutable disfrazado".getBytes(StandardCharsets.US_ASCII);

    assertThatThrownBy(() -> EvidenceFile.inspect("virus.jpg", text, MAX_SIZE))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF.");
  }

  @Test
  @DisplayName("Rechaza un contenedor RIFF que no es WEBP")
  void shouldRejectRiffThatIsNotWebp() {
    byte[] wave = "RIFF\u0000\u0000\u0000\u0000WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1);

    assertThatThrownBy(() -> EvidenceFile.inspect("audio.webp", wave, MAX_SIZE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Rechaza un archivo vacío o ausente")
  void shouldRejectEmptyContent() {
    assertThatThrownBy(() -> EvidenceFile.inspect("recibo.jpg", new byte[0], MAX_SIZE))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El archivo del comprobante está vacío.");
    assertThatThrownBy(() -> EvidenceFile.inspect("recibo.jpg", null, MAX_SIZE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("Acepta un archivo del tamaño máximo y rechaza uno byte mayor")
  void shouldApplyMaximumSize() {
    byte[] exact = EvidenceFiles.sized("recibo.jpg", (int) MAX_SIZE);
    byte[] oneOver = EvidenceFiles.sized("recibo.jpg", (int) MAX_SIZE + 1);

    assertThat(EvidenceFile.inspect("recibo.jpg", exact, MAX_SIZE).sizeBytes()).isEqualTo(MAX_SIZE);
    assertThatThrownBy(() -> EvidenceFile.inspect("recibo.jpg", oneOver, MAX_SIZE))
        .isInstanceOf(EvidenceTooLargeException.class)
        .hasMessage("El comprobante supera el tamaño máximo permitido de 5 MB.");
  }

  @Test
  @DisplayName("El mensaje de tamaño usa bytes cuando el límite no es múltiplo de un megabyte")
  void shouldReportLimitInBytesWhenNotWholeMegabytes() {
    assertThatThrownBy(() -> EvidenceFile.inspect("recibo.jpg", EvidenceFiles.sized("recibo.jpg", 2000), 1500))
        .isInstanceOf(EvidenceTooLargeException.class)
        .hasMessage("El comprobante supera el tamaño máximo permitido de 1500 bytes.");
  }

  @Test
  @DisplayName("La huella SHA-256 coincide con el vector conocido")
  void shouldComputeSha256() {
    assertThat(EvidenceFile.sha256Hex("abc".getBytes(StandardCharsets.US_ASCII)))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @ParameterizedTest(name = "\"{0}\" queda como \"{1}\"")
  @CsvSource(
      delimiter = '|',
      value = {
        "../../etc/recibo.jpg|recibo.jpg",
        "C:\\Users\\Windows10\\factura.pdf|factura.pdf",
        "   |comprobante",
        "..|comprobante",
        "foto\u0007.png|foto.png"
      })
  @DisplayName("Conserva solo el nombre base del archivo")
  void shouldSanitizeFileName(String originalName, String expected) {
    assertThat(EvidenceFile.sanitizeName(originalName)).isEqualTo(expected);
  }

  @Test
  @DisplayName("Un nombre nulo recibe un nombre por defecto")
  void shouldUseDefaultNameWhenMissing() {
    assertThat(EvidenceFile.sanitizeName(null)).isEqualTo("comprobante");
  }
}
