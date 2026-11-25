package com.nexus.rinde.incident.domain.model.valueobjects;

/** Archivo binario y metadatos de la evidencia de una incidencia. */
public record IncidentEvidenceContent(String fileName, String contentType, byte[] content) {}
