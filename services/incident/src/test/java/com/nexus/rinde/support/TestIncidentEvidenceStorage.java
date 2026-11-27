package com.nexus.rinde.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Carpeta temporal aislada para las pruebas de evidencias de incidencias. */
public final class TestIncidentEvidenceStorage {

  private static final Path DIRECTORY = createDirectory();

  private TestIncidentEvidenceStorage() {}

  public static void registerPath(DynamicPropertyRegistry registry) {
    registry.add("rinde.evidence.storage.local-path", () -> DIRECTORY.toString());
  }

  public static Path directory() {
    return DIRECTORY;
  }

  private static Path createDirectory() {
    try {
      return Files.createTempDirectory("rinde-incident-evidence-test-");
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo crear la carpeta temporal de evidencias.", ex);
    }
  }
}
