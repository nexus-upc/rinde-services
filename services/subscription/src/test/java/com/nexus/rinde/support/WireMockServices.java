package com.nexus.rinde.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Servidores WireMock de los servicios vecinos, uno por dependencia (IAM y Fleet). Arrancan una sola
 * vez por proceso en puertos libres. Tener un servidor por dependencia evita que reiniciar los dobles
 * de IAM borre también los de Fleet.
 */
public final class WireMockServices {

  private static final WireMockServer IAM = start();
  private static final WireMockServer FLEET = start();

  private WireMockServices() {}

  public static WireMockServer iam() {
    return IAM;
  }

  public static WireMockServer fleet() {
    return FLEET;
  }

  public static String iamBaseUrl() {
    return "http://localhost:" + IAM.port();
  }

  public static String fleetBaseUrl() {
    return "http://localhost:" + FLEET.port();
  }

  private static WireMockServer start() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    return server;
  }
}
