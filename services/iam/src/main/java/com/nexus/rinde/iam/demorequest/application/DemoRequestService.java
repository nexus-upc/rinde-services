package com.nexus.rinde.iam.demorequest.application;

import com.nexus.rinde.iam.demorequest.domain.DemoRequest;
import com.nexus.rinde.iam.demorequest.domain.DemoRequestAlreadyReceivedException;
import com.nexus.rinde.iam.demorequest.domain.DemoRequestStatus;
import com.nexus.rinde.iam.demorequest.infrastructure.DemoRequestRepository;
import com.nexus.rinde.shared.interfaces.rest.PageResource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra las solicitudes de la landing y no acepta otra del mismo correo en 10 minutos. */
@Service
public class DemoRequestService {

  static final Duration REPEAT_WINDOW = Duration.ofMinutes(10);

  private final DemoRequestRepository requests;
  private final Clock clock;

  public DemoRequestService(DemoRequestRepository requests, Clock clock) {
    this.requests = requests;
    this.clock = clock;
  }

  /** Datos del formulario ya validados por la capa REST. */
  public record Submission(
      String fullName,
      String companyName,
      String email,
      String phone,
      int fleetSize,
      String message) {}

  @Transactional
  public DemoRequest submit(Submission submission) {
    String email = submission.email().trim().toLowerCase(Locale.ROOT);
    // El bloqueo por correo evita que dos envíos casi simultáneos pasen la comprobación a la vez.
    requests.lockEmail(email);
    Instant now = clock.instant();
    if (requests.existsReceivedSince(email, now.minus(REPEAT_WINDOW))) {
      throw new DemoRequestAlreadyReceivedException();
    }
    DemoRequest request =
        new DemoRequest(
            UUID.randomUUID(),
            submission.fullName().trim(),
            submission.companyName().trim(),
            email,
            blankToNull(submission.phone()),
            submission.fleetSize(),
            blankToNull(submission.message()),
            DemoRequestStatus.RECEIVED,
            now);
    requests.insert(request);
    return request;
  }

  @Transactional(readOnly = true)
  public PageResource<DemoRequest> list(int page, int size) {
    return requests.list(page, size);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
