package com.nexus.rinde.iam.infrastructure.mail;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Envía con Azure Communication Services Email y espera el resultado con un límite de tiempo. */
@Component
@ConditionalOnProperty(name = "rinde.mail.provider", havingValue = "azure")
public class AzureSdkEmailGateway implements EmailGateway {

  private static final Duration SEND_TIMEOUT = Duration.ofSeconds(30);
  private static final String SEND_FAILED =
      "No se pudo enviar el correo con Azure Communication Services.";

  private final EmailClient client;
  private final String sender;

  public AzureSdkEmailGateway(MailProperties properties) {
    this.client = buildClient(properties.getAcsConnectionString());
    this.sender = properties.getSender();
  }

  @Override
  public void send(String to, String subject, String plainText, String html) {
    EmailMessage message =
        new EmailMessage()
            .setSenderAddress(sender)
            .setToRecipients(to)
            .setSubject(subject)
            .setBodyPlainText(plainText)
            .setBodyHtml(html);
    EmailSendResult result = awaitResult(message);
    if (!EmailSendStatus.SUCCEEDED.equals(result.getStatus())) {
      throw new MailDeliveryException(SEND_FAILED);
    }
  }

  /** Espera el envío; cualquier error del SDK se reemplaza por un mensaje sin datos del correo. */
  private EmailSendResult awaitResult(EmailMessage message) {
    try {
      SyncPoller<EmailSendResult, EmailSendResult> poller = client.beginSend(message);
      PollResponse<EmailSendResult> last = poller.waitForCompletion(SEND_TIMEOUT);
      if (last.getStatus() != LongRunningOperationStatus.SUCCESSFULLY_COMPLETED) {
        throw new MailDeliveryException(SEND_FAILED);
      }
      return poller.getFinalResult();
    } catch (MailDeliveryException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new MailDeliveryException(SEND_FAILED);
    }
  }

  private static EmailClient buildClient(String connectionString) {
    try {
      return new EmailClientBuilder().connectionString(connectionString).buildClient();
    } catch (RuntimeException ex) {
      // El mensaje del SDK podría repetir la cadena de conexión: no se propaga.
      throw new IllegalStateException(
          "La cadena de conexión de Azure Communication Services no es válida.");
    }
  }
}
