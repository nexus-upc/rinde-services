package com.nexus.rinde.iam.demomail.application;

import com.nexus.rinde.iam.demomail.domain.DemoMail;
import com.nexus.rinde.iam.demomail.infrastructure.*;
import com.nexus.rinde.iam.domain.model.valueobjects.TokenPurpose;
import com.nexus.rinde.shared.domain.exceptions.*;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "rinde.demo.mailbox.enabled", havingValue = "true")
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "demo", matchIfMissing = true)
public class DemoMailboxService {
  private final DemoMailRepository messages;
  private final DemoMailCipher cipher;
  private final Clock clock;

  public DemoMailboxService(DemoMailRepository messages, DemoMailCipher cipher, Clock clock) {
    this.messages = messages;
    this.cipher = cipher;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResource<Summary> list(String query, TokenPurpose purpose, int page, int size) {
    if (query.length() > 150 || page < 0 || page > 1000000 || size < 1 || size > 100)
      throw new BusinessRuleException("Los filtros del buzón no son válidos.");
    PageResource<DemoMail> result = messages.list(query.trim(), purpose, page, size);
    Instant now = clock.instant();
    return new PageResource<>(
        result.items().stream().map(m -> summary(m, now)).toList(), page, size, result.total());
  }

  @Transactional
  public Detail open(UUID id) {
    DemoMail message =
        messages.find(id).orElseThrow(() -> new ResourceNotFoundException("El mensaje no existe."));
    Instant now = clock.instant();
    messages.markRead(id, now);
    message = messages.find(id).orElseThrow();
    String action = null;
    if ("ACTIVE".equals(message.state(now))) {
      String token =
          URLEncoder.encode(
              cipher.decrypt(message.encryptedToken(), message.encryptionContext()),
              StandardCharsets.UTF_8);
      action =
          switch (message.purpose()) {
            case EMAIL_VERIFICATION ->
                "/verify-email?tenant=" + message.tenantId() + "&token=" + token;
            case INVITATION -> "/accept-invitation?token=" + token;
            case PASSWORD_RESET -> "/reset-password?token=" + token;
          };
    }
    return new Detail(summary(message, now), action);
  }

  private Summary summary(DemoMail message, Instant now) {
    String subject =
        switch (message.purpose()) {
          case EMAIL_VERIFICATION -> "Verifica tu empresa en Rinde";
          case INVITATION -> "Tu invitación a " + message.companyName();
          case PASSWORD_RESET -> "Recupera tu acceso a Rinde";
        };
    return new Summary(
        message.id(),
        message.recipient(),
        message.recipientName(),
        message.companyName(),
        message.purpose().name(),
        subject,
        message.createdAt(),
        message.expiresAt(),
        message.readAt(),
        message.state(now));
  }

  public record Summary(
      UUID id,
      String recipient,
      String recipientName,
      String companyName,
      String purpose,
      String subject,
      Instant createdAt,
      Instant expiresAt,
      Instant readAt,
      String state) {}

  public record Detail(Summary message, String actionPath) {}
}
