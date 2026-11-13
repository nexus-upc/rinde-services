package com.nexus.rinde.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Servidor WireMock compartido por las pruebas. Arranca una sola vez por proceso en un puerto libre.
 */
public final class WireMockServices {

  private static final WireMockServer SERVER = start();

  private WireMockServices() {}

  public static WireMockServer server() {
    return SERVER;
  }

  public static String baseUrl() {
    return "http://localhost:" + SERVER.port();
  }

  private static WireMockServer start() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    return server;
  }
}
