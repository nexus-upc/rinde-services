package com.nexus.rinde.incident.infrastructure.storage;

import com.nexus.rinde.incident.domain.services.IncidentEvidenceStorage;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptador local de evidencias de incidencias para entorno de desarrollo. */
@Component
@ConditionalOnProperty(
    name = "rinde.evidence.storage.type",
    havingValue = "local",
    matchIfMissing = true)
public class LocalFileSystemIncidentEvidenceStorage implements IncidentEvidenceStorage {

  private final Path root;

  public LocalFileSystemIncidentEvidenceStorage(
      @Value("${rinde.evidence.storage.local-path:./data/evidences}") String localPath) {
    this.root = Path.of(localPath).toAbsolutePath().normalize();
  }

  @Override
  public void save(String storageKey, byte[] content) {
    Path target = resolveSafe(storageKey);
    try {
      Files.createDirectories(target.getParent());
      Path temp = target.getParent().resolve(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
      try {
        Files.write(temp, content);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temp);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo guardar la evidencia en disco.", ex);
    }
  }

  @Override
  public byte[] load(String storageKey) {
    Path target = resolveSafe(storageKey);
    if (!Files.exists(target)) {
      throw new ResourceNotFoundException(
          "El archivo del comprobante no está disponible en el almacenamiento.");
    }
    try {
      return Files.readAllBytes(target);
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo leer la evidencia desde disco.", ex);
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(resolveSafe(storageKey));
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo eliminar la evidencia en disco.", ex);
    }
  }

  private Path resolveSafe(String storageKey) {
    Path resolved = root.resolve(storageKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Clave de almacenamiento fuera de la carpeta configurada.");
    }
    return resolved;
  }
}
