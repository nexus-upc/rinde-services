package com.nexus.rinde.support;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** Comprobantes de prueba con la firma inicial real de cada formato permitido. */
public final class EvidenceFiles {

  private EvidenceFiles() {}

  /** Contenido de tamaño mínimo válido para el nombre dado; un nombre .txt no es comprobante. */
  public static byte[] contentOf(String fileName) {
    return sized(fileName, 256);
  }

  /** Contenido con la firma del formato y relleno hasta el tamaño indicado. */
  public static byte[] sized(String fileName, int size) {
    byte[] signature = signature(fileName).getBytes(StandardCharsets.ISO_8859_1);
    byte[] content = new byte[Math.max(size, signature.length)];
    Arrays.fill(content, (byte) 'x');
    System.arraycopy(signature, 0, content, 0, signature.length);
    return content;
  }

  /** Tipo de contenido que declara el cliente al subir el archivo. */
  public static String contentTypeOf(String fileName) {
    return switch (extension(fileName)) {
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "webp" -> "image/webp";
      case "pdf" -> "application/pdf";
      default -> "text/plain";
    };
  }

  private static String signature(String fileName) {
    return switch (extension(fileName)) {
      case "jpg", "jpeg" -> "ÿØÿà";
      case "png" -> "\u0089PNG\r\n\u001A\n";
      case "webp" -> "RIFF\u0000\u0000\u0000\u0000WEBP";
      case "pdf" -> "%PDF-1.4\n";
      default -> "Texto plano, no es un comprobante";
    };
  }

  private static String extension(String fileName) {
    int dot = fileName.lastIndexOf('.');
    return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
