package com.nexus.rinde.expense.infrastructure.storage;

import com.nexus.rinde.expense.domain.services.EvidenceStorage;
import com.nexus.rinde.shared.domain.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adaptador de comprobantes en una carpeta local. En Docker la carpeta es un volumen. */
@Component
public class LocalFileSystemEvidenceStorage implements EvidenceStorage {

  private static final Logger log = LoggerFactory.getLogger(LocalFileSystemEvidenceStorage.class);

  private final Path root;

  public LocalFileSystemEvidenceStorage(
      @Value("${rinde.evidence.storage.local-path:./data/evidences}") String localPath) {
    this.root = Path.of(localPath).toAbsolutePath().normalize();
  }

  @Override
  public void save(String storageKey, byte[] content) {
    Path target = resolve(storageKey);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo guardar el comprobante.", e);
    }
  }

  @Override
  public byte[] load(String storageKey) {
    Path target = resolve(storageKey);
    if (!Files.isRegularFile(target)) {
      throw new ResourceNotFoundException(
          "El archivo del comprobante no está disponible en el almacenamiento.");
    }
    try {
      return Files.readAllBytes(target);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo leer el comprobante.", e);
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException e) {
      log.warn("No se pudo borrar un comprobante reemplazado: {}", e.getMessage());
    }
  }

  /** Rechaza claves que salgan de la carpeta de comprobantes. */
  private Path resolve(String storageKey) {
    Path target = root.resolve(storageKey).normalize();
    if (!target.startsWith(root) || target.equals(root)) {
      throw new IllegalArgumentException("Clave de almacenamiento no válida.");
    }
    return target;
  }
}
