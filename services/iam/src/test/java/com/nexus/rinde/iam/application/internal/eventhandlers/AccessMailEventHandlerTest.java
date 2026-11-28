package com.nexus.rinde.iam.application.internal.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nexus.rinde.iam.application.internal.outboundservices.AccessMailPort;
import com.nexus.rinde.iam.domain.model.aggregates.Tenant;
import com.nexus.rinde.iam.domain.model.events.PasswordResetRequested;
import com.nexus.rinde.iam.domain.model.events.TenantRegistered;
import com.nexus.rinde.iam.domain.model.events.UserInvited;
import com.nexus.rinde.iam.infrastructure.mail.AccessMailComposer;
import com.nexus.rinde.iam.infrastructure.mail.AzureEmailAccessMailAdapter;
import com.nexus.rinde.iam.infrastructure.mail.MailDeliveryException;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.TenantRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class AccessMailEventHandlerTest {

  private static final UUID TENANT = UUID.fromString("7d1c0f3e-5a2b-4c8d-9e6f-1a2b3c4d5e6f");
  private static final String BASE = "http://localhost:4200";

  private final AccessMailPort port = mock(AccessMailPort.class);
  private final TenantRepository tenants = mock(TenantRepository.class);
  private final AccessMailEventHandler handler = new AccessMailEventHandler(port, tenants);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger logger = (Logger) LoggerFactory.getLogger(AccessMailEventHandler.class);

  @BeforeEach
  void attachAppender() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  private static TenantRegistered tenantRegistered(UUID eventId, String verificationToken) {
    return new TenantRegistered(
        eventId,
        Instant.now(),
        TENANT,
        "Andes SAC",
        "ana@prueba.invalid",
        verificationToken,
        Instant.now());
  }

  private static UserInvited userInvited(String invitationToken) {
    return new UserInvited(
        UUID.randomUUID(),
        Instant.now(),
        TENANT,
        UUID.randomUUID(),
        "Luis Quispe",
        "luis@prueba.invalid",
        "DRIVER",
        invitationToken,
        Instant.now());
  }

  private static PasswordResetRequested passwordResetRequested(String resetToken) {
    return new PasswordResetRequested(
        UUID.randomUUID(),
        Instant.now(),
        TENANT,
        UUID.randomUUID(),
        "ana@prueba.invalid",
        resetToken,
        Instant.now());
  }

  private ILoggingEvent onlyError() {
    List<ILoggingEvent> errors =
        appender.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
    assertThat(errors).hasSize(1);
    return errors.get(0);
  }

  @Test
  void sendsTheVerificationWithTheTenantData() {
    handler.on(tenantRegistered(UUID.randomUUID(), "token-verificacion"));

    verify(port)
        .sendTenantVerification(TENANT, "Andes SAC", "ana@prueba.invalid", "token-verificacion");
  }

  @Test
  void sendsTheInvitationWithTheCompanyNameFromTheTenant() {
    Tenant tenant = mock(Tenant.class);
    when(tenant.getTradeName()).thenReturn("Andes SAC");
    when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant));

    UserInvited event = userInvited("token-invitacion");
    handler.on(event);

    verify(port)
        .sendUserInvitation(TENANT, "Andes SAC", "luis@prueba.invalid", "token-invitacion");
  }

  @Test
  void sendsThePasswordResetToTheUser() {
    handler.on(passwordResetRequested("token-recuperacion"));

    verify(port).sendPasswordReset(TENANT, "ana@prueba.invalid", "token-recuperacion");
  }

  @Test
  void everyMailIsSentOnlyAfterTheTransactionCommits() throws NoSuchMethodException {
    for (Class<?> type :
        List.of(TenantRegistered.class, UserInvited.class, PasswordResetRequested.class)) {
      Method method = AccessMailEventHandler.class.getMethod("on", type);
      TransactionalEventListener listener =
          method.getAnnotation(TransactionalEventListener.class);

      assertThat(listener).isNotNull();
      assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
  }

  @Test
  void aFailedDeliveryIsLoggedWithoutTokenLinkOrFullAddressAndNotRethrown() {
    UUID eventId = UUID.randomUUID();
    AccessMailEventHandler failing =
        new AccessMailEventHandler(
            new AzureEmailAccessMailAdapter(
                (to, subject, plainText, html) -> {
                  throw new MailDeliveryException(
                      "No se pudo enviar el correo con Azure Communication Services.");
                },
                new AccessMailComposer(BASE)),
            tenants);

    assertThatCode(() -> failing.on(tenantRegistered(eventId, "token-verificacion")))
        .doesNotThrowAnyException();

    assertThat(onlyError().getFormattedMessage())
        .contains("verificación de empresa")
        .contains(eventId.toString())
        .contains("a***@prueba.invalid")
        .contains("No se pudo enviar el correo con Azure Communication Services.")
        .doesNotContain("token-verificacion")
        .doesNotContain("verify-email")
        .doesNotContain(BASE)
        .doesNotContain("ana@prueba.invalid");
  }

  @Test
  void anUnexpectedErrorIsLoggedByItsTypeOnly() {
    AccessMailPort broken = mock(AccessMailPort.class);
    doThrow(new IllegalStateException("token-secreto en el mensaje"))
        .when(broken)
        .sendPasswordReset(any(), any(), any());
    AccessMailEventHandler brokenHandler = new AccessMailEventHandler(broken, tenants);

    assertThatCode(() -> brokenHandler.on(passwordResetRequested("token-recuperacion")))
        .doesNotThrowAnyException();

    assertThat(onlyError().getFormattedMessage())
        .contains("recuperación de acceso")
        .contains("IllegalStateException")
        .doesNotContain("token-secreto")
        .doesNotContain("token-recuperacion");
  }

  @Test
  void aMissingCompanyIsLoggedAndNotRethrown() {
    when(tenants.findById(TENANT)).thenReturn(Optional.empty());

    assertThatCode(() -> handler.on(userInvited("token-invitacion")))
        .doesNotThrowAnyException();

    assertThat(onlyError().getFormattedMessage())
        .contains("invitación de usuario")
        .doesNotContain("token-invitacion");
  }

  @Test
  void masksTheRecipientKeepingFirstLetterAndDomain() {
    assertThat(AccessMailEventHandler.maskEmail("ana@andes.pe")).isEqualTo("a***@andes.pe");
    assertThat(AccessMailEventHandler.maskEmail("sin-arroba")).isEqualTo("***");
    assertThat(AccessMailEventHandler.maskEmail(null)).isEqualTo("***");
  }
}
