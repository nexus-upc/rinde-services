package com.nexus.rinde.expense.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileSystemEvidenceStorageTest {

  @TempDir Path root;

  @Test
  @DisplayName("Guarda y lee el contenido bajo la clave indicada")
  void shouldSaveAndLoadContent() {
    LocalFileSystemEvidenceStorage storage = new LocalFileSystemEvidenceStorage(root.toString());
    byte[] content = "comprobante".getBytes(StandardCharsets.US_ASCII);

    storage.save("tenant/expense/archivo", content);

    assertThat(storage.load("tenant/expense/archivo")).isEqualTo(content);
  }

  @Test
  @DisplayName("El contenido sigue disponible con una instancia nueva, como tras reiniciar")
  void shouldKeepContentAfterRestart() {
    byte[] content = "recibo".getBytes(StandardCharsets.US_ASCII);
    new LocalFileSystemEvidenceStorage(root.toString()).save("t/e/a", content);

    LocalFileSystemEvidenceStorage restarted = new LocalFileSystemEvidenceStorage(root.toString());

    assertThat(restarted.load("t/e/a")).isEqualTo(content);
  }

  @Test
  @DisplayName("No sobrescribe un archivo que ya existe")
  void shouldNotOverwriteExistingFile() {
    LocalFileSystemEvidenceStorage storage = new LocalFileSystemEvidenceStorage(root.toString());
    storage.save("t/e/a", new byte[] {1});

    assertThatThrownBy(() -> storage.save("t/e/a", new byte[] {2}))
        .isInstanceOf(UncheckedIOException.class);
    assertThat(storage.load("t/e/a")).containsExactly(1);
  }

  @Test
  @DisplayName("Rechaza claves que salen de la carpeta de comprobantes")
  void shouldRejectKeysOutsideRoot() {
    LocalFileSystemEvidenceStorage storage = new LocalFileSystemEvidenceStorage(root.toString());

    assertThatThrownBy(() -> storage.save("../escape.txt", new byte[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.load("../../etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.load("/etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.load(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Leer un archivo inexistente responde como no encontrado")
  void shouldReportMissingFileAsNotFound() {
    LocalFileSystemEvidenceStorage storage = new LocalFileSystemEvidenceStorage(root.toString());

    assertThatThrownBy(() -> storage.load("t/e/inexistente"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Borrar elimina el archivo y no falla si ya no existe")
  void shouldDeleteFileAndIgnoreMissing() throws Exception {
    LocalFileSystemEvidenceStorage storage = new LocalFileSystemEvidenceStorage(root.toString());
    storage.save("t/e/a", new byte[] {1});

    storage.delete("t/e/a");
    storage.delete("t/e/a");

    assertThat(Files.exists(root.resolve("t/e/a"))).isFalse();
  }
}
