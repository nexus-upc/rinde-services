package com.nexus.rinde.iam.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.rinde.iam.application.internal.eventhandlers.AccessTokenLoggingEventHandler;
import com.nexus.rinde.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Con azure y el buzón deshabilitado, tampoco se registran los enlaces en el log. */
class AzureMailboxDisabledIntegrationTest extends AbstractIntegrationTest {

  @MockitoBean EmailGateway emailGateway;
  @Autowired ApplicationContext context;

  @DynamicPropertySource
  static void azureWithDemoMailboxDisabled(DynamicPropertyRegistry registry) {
    AzureMailTestSettings.register(registry, false);
  }

  @Test
  void theAccessTokenLogIsNotRegisteredWithAzure() {
    assertThat(context.getBeansOfType(AccessTokenLoggingEventHandler.class)).isEmpty();
    assertThat(context.getBeansOfType(AzureEmailAccessMailAdapter.class)).hasSize(1);
  }
}
