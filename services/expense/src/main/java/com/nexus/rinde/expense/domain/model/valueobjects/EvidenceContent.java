package com.nexus.rinde.expense.domain.model.valueobjects;

/** Archivo de un comprobante listo para responderse al usuario autorizado. */
public record EvidenceContent(String fileName, String contentType, byte[] content) {}
