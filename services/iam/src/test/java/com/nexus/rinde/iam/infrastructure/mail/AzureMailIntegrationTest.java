package com.nexus.rinde.iam.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.rinde.iam.application.internal.eventhandlers.AccessMailEventHandler;
import com.nexus.rinde.iam.demomail.application.DemoMailCapture;
import com.nexus.rinde.iam.demomail.application.DemoMailboxService;
import com.nexus.rinde.iam.demomail.infrastructure.DemoMailCipher;
import com.nexus.rinde.iam.demomail.infrastructure.DemoMailRepository;
import com.nexus.rinde.iam.demomail.interfaces.DemoMailboxController;
import com.nexus.rinde.iam.domain.model.events.TenantRegistered;
import com.nexus.rinde.iam.domain.model.events.UserInvited;
import com.nexus.rinde.support.AbstractIntegrationTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.HtmlUtils;

/** Con azure, el contexto usa el adaptador real y ningún componente del buzón de demostración. */
class AzureMailIntegrationTest extends AbstractIntegrationTest {

  private static final String PASSWORD = "Clave-Segura-2026";
  private static final String ADMIN = "administrador@prueba.invalid";

  @MockitoBean EmailGateway emailGateway;
  @Autowired ApplicationContext context;

  @DynamicPropertySource
  static void azureWithDemoMailboxEnabled(DynamicPropertyRegistry registry) {
    AzureMailTestSettings.register(registry, true);
  }

  @Test
  void theContextUsesTheAzureAdapterAndNoDemoMailboxBeans() {
    assertThat(context.getBeansOfType(AzureEmailAccessMailAdapter.class)).hasSize(1);
    assertThat(context.getBeansOfType(AccessMailEventHandler.class)).hasSize(1);
    assertThat(context.getBeansOfType(DemoMailCapture.class)).isEmpty();
    assertThat(context.getBeansOfType(DemoMailboxService.class)).isEmpty();
    assertThat(context.getBeansOfType(DemoMailCipher.class)).isEmpty();
    assertThat(context.getBeansOfType(DemoMailRepository.class)).isEmpty();
    assertThat(context.getBeansOfType(DemoMailboxController.class)).isEmpty();
  }

  @Test
  void demoFeaturesReportsTheMailboxDisabledWithAzure() throws Exception {
    mockMvc
        .perform(get("/api/v1/demo/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mailboxEnabled").value(false));
  }

  @Test
  void registeringACompanySendsOneVerificationMailWithTheLink() throws Exception {
    String tenantId =
        api.json(
                api.registerTenant(
                        "Andes SAC", "20123456789", "Ana Administradora", ADMIN, PASSWORD)
                    .andExpect(status().isCreated()))
            .get("id")
            .asText();
    String token = capturedEvents.last(TenantRegistered.class).orElseThrow().verificationToken();
    String link =
        "http://localhost:4200/verify-email?tenant="
            + tenantId
            + "&token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);

    ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> plainText = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
    verify(emailGateway, times(1))
        .send(to.capture(), subject.capture(), plainText.capture(), html.capture());

    assertThat(to.getValue()).isEqualTo(ADMIN);
    assertThat(subject.getValue()).isEqualTo("Verifica tu empresa en Rinde");
    assertThat(plainText.getValue()).contains(link);
    assertThat(html.getValue()).contains(HtmlUtils.htmlEscape(link));
  }

  @Test
  void invitingAUserSendsOneInvitationNamedAfterTheCompany() throws Exception {
    api.registerAndVerifyTenant("20123456789", ADMIN, PASSWORD);
    String adminToken = api.signInForToken(ADMIN, PASSWORD);
    clearInvocations(emailGateway);

    api.inviteUser(adminToken, "Luis Quispe", "luis@prueba.invalid", "DRIVER")
        .andExpect(status().isCreated());

    String token = capturedEvents.last(UserInvited.class).orElseThrow().invitationToken();
    ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> plainText = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
    verify(emailGateway, times(1))
        .send(to.capture(), subject.capture(), plainText.capture(), html.capture());

    assertThat(to.getValue()).isEqualTo("luis@prueba.invalid");
    assertThat(subject.getValue()).isEqualTo("Tu invitación a Empresa 20123456789");
    assertThat(plainText.getValue())
        .contains(
            "http://localhost:4200/accept-invitation?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8));
  }
}
