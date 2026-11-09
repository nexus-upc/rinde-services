package com.nexus.rinde.expense.domain.services;

/**
 * Puerto de almacenamiento de comprobantes. La clave es opaca: la asigna el servicio y no es una
 * ruta del sistema de archivos, así que un adaptador de objetos puede reemplazar al local.
 */
public interface EvidenceStorage {

  /** Guarda el contenido bajo la clave indicada. No sobrescribe un archivo existente. */
  void save(String storageKey, byte[] content);

  /** Lee el contenido; lanza ResourceNotFoundException si no existe. */
  byte[] load(String storageKey);

  /** Borra el contenido si existe. No falla si el archivo ya no está. */
  void delete(String storageKey);
}
