package com.nexus.rinde.iam.demorequest.domain;

/** El mismo correo ya envió una solicitud en la ventana de repetición; no se registra otra. */
public class DemoRequestAlreadyReceivedException extends RuntimeException {

  public DemoRequestAlreadyReceivedException() {
    super("Ya recibimos tu solicitud. Te contactaremos pronto.");
  }
}
