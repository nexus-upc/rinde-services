package com.nexus.rinde.incident.domain.services;

/** Puerto de almacenamiento binario para evidencias de incidencias. */
public interface IncidentEvidenceStorage {

  void save(String storageKey, byte[] content);

  byte[] load(String storageKey);

  void delete(String storageKey);
}
