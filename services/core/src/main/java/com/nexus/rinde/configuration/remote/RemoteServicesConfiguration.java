package com.nexus.rinde.configuration.remote;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Clientes hacia Trip, Fleet, Expense e Incident separados, con los mismos tiempos de espera. */
@Configuration
public class RemoteServicesConfiguration {

  @Bean
  @ConditionalOnProperty(name = "rinde.incident.remote-enabled", havingValue = "true")
  public RestClient incidentRestClient(
      RestClient.Builder builder,
      @Value("${rinde.incident.base-url:http://localhost:8085}") String baseUrl) {
    return client(builder, baseUrl);
  }

  @Bean
  @ConditionalOnProperty(name = "rinde.trip.remote-enabled", havingValue = "true")
  public RestClient tripRestClient(
      RestClient.Builder builder,
      @Value("${rinde.trip.base-url:http://localhost:8082}") String baseUrl) {
    return client(builder, baseUrl);
  }

  @Bean
  @ConditionalOnProperty(name = "rinde.fleet.remote-enabled", havingValue = "true")
  public RestClient fleetRestClient(
      RestClient.Builder builder,
      @Value("${rinde.fleet.base-url:http://localhost:8083}") String baseUrl) {
    return client(builder, baseUrl);
  }

  @Bean
  @ConditionalOnProperty(name = "rinde.expense.remote-enabled", havingValue = "true")
  public RestClient expenseRestClient(
      RestClient.Builder builder,
      @Value("${rinde.expense.base-url:http://localhost:8084}") String baseUrl) {
    return client(builder, baseUrl);
  }

  private static RestClient client(RestClient.Builder builder, String baseUrl) {
    var requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(3_000);
    requests.setReadTimeout(5_000);
    return builder.clone().baseUrl(baseUrl).requestFactory(requests).build();
  }
}
