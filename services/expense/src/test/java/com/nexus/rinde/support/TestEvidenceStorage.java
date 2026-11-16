package com.nexus.rinde.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Carpeta temporal de comprobantes para las pruebas de integración y de aceptación. */
public final class TestEvidenceStorage {

  private static final Path DIRECTORY = createDirectory();

  private TestEvidenceStorage() {}

  public static void registerPath(DynamicPropertyRegistry registry) {
    registry.add("rinde.evidence.storage.local-path", () -> DIRECTORY.toString());
  }

  private static Path createDirectory() {
    try {
      return Files.createTempDirectory("rinde-evidence-test-");
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo crear la carpeta de comprobantes de prueba.", e);
    }
  }
}
