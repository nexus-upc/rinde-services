package com.nexus.rinde.notification.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.notification.infrastructure.persistence.jpa.entities.DriverDeviceEntity;
import com.nexus.rinde.notification.infrastructure.persistence.jpa.repositories.DriverDeviceRepository;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class DriverDevicesIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DriverDeviceRepository driverDeviceRepository;

  @Test
  void registersDeviceIdempotentlyAndReassignsOwnerWithoutDuplicatingRows() throws Exception {
    UUID firstTenant = UUID.randomUUID();
    UUID firstDriver = UUID.randomUUID();
    String firstToken = tokens.issue(firstDriver, firstTenant, "DRIVER", "luis@andes.pe");

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isNoContent());

    DriverDeviceEntity created = driverDeviceRepository.findByToken("fcm-token-01").orElseThrow();
    assertThat(created.getTenantId()).isEqualTo(firstTenant);
    assertThat(created.getUserId()).isEqualTo(firstDriver);
    assertThat(created.getPlatform()).isEqualTo("ANDROID");
    assertThat(driverDeviceRepository.count()).isEqualTo(1);

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isNoContent());

    assertThat(driverDeviceRepository.count()).isEqualTo(1);

    UUID secondTenant = UUID.randomUUID();
    UUID secondDriver = UUID.randomUUID();
    String secondToken = tokens.issue(secondDriver, secondTenant, "DRIVER", "carlos@sur.pe");

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + secondToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isNoContent());

    DriverDeviceEntity reassigned = driverDeviceRepository.findByToken("fcm-token-01").orElseThrow();
    assertThat(reassigned.getId()).isEqualTo(created.getId());
    assertThat(reassigned.getTenantId()).isEqualTo(secondTenant);
    assertThat(reassigned.getUserId()).isEqualTo(secondDriver);
    assertThat(driverDeviceRepository.count()).isEqualTo(1);
  }

  @Test
  void deletesOwnDeviceIdempotentlyWithoutAffectingOtherUsersOrTenants() throws Exception {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    UUID driverOne = UUID.randomUUID();
    UUID driverTwoSameTenant = UUID.randomUUID();
    UUID driverOtherTenant = UUID.randomUUID();

    String driverOneJwt = tokens.issue(driverOne, tenantA, "DRIVER", "driver1@andes.pe");
    String driverTwoJwt = tokens.issue(driverTwoSameTenant, tenantA, "DRIVER", "driver2@andes.pe");
    String driverOtherTenantJwt = tokens.issue(driverOtherTenant, tenantB, "DRIVER", "driver@sur.pe");

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + driverOneJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-shared:01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isNoContent());

    // Otro conductor de la misma empresa no puede borrar el token ajeno.
    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-shared:01")
                .header("Authorization", "Bearer " + driverTwoJwt))
        .andExpect(status().isNoContent());
    assertThat(driverDeviceRepository.findByToken("fcm-token-shared:01")).isPresent();

    // Un conductor de otra empresa tampoco puede borrar el token ajeno.
    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-shared:01")
                .header("Authorization", "Bearer " + driverOtherTenantJwt))
        .andExpect(status().isNoContent());
    assertThat(driverDeviceRepository.findByToken("fcm-token-shared:01")).isPresent();

    // El propietario sí elimina su token y repetir la baja sigue devolviendo 204.
    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-shared:01")
                .header("Authorization", "Bearer " + driverOneJwt))
        .andExpect(status().isNoContent());
    assertThat(driverDeviceRepository.findByToken("fcm-token-shared:01")).isEmpty();

    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-shared:01")
                .header("Authorization", "Bearer " + driverOneJwt))
        .andExpect(status().isNoContent());
  }

  @Test
  void validatesDevicePayloadAndEnforcesDriverRole() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String driverJwt = tokens.issue(UUID.randomUUID(), tenantId, "DRIVER", "driver@andes.pe");
    String adminJwt = tokens.issue(UUID.randomUUID(), tenantId, "ADMINISTRATOR", "admin@andes.pe");
    String opsJwt =
        tokens.issue(UUID.randomUUID(), tenantId, "OPERATIONS_MANAGER", "ops@andes.pe");

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + driverJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("La solicitud tiene datos inválidos."));

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + driverJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + "a".repeat(513) + "\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("La solicitud tiene datos inválidos."));

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + driverJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"IOS\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("La plataforma debe ser ANDROID."));

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + adminJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .header("Authorization", "Bearer " + opsJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-01")
                .header("Authorization", "Bearer " + adminJwt))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/v1/notifications/devices/{token}", "fcm-token-01")
                .header("Authorization", "Bearer " + opsJwt))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/notifications/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-token-01\",\"platform\":\"ANDROID\"}"))
        .andExpect(status().isUnauthorized());
  }
}
