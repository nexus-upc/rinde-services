package com.nexus.rinde.support;

import com.nexus.rinde.shared.domain.model.events.IntegrationEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Guarda los eventos de integración publicados, para que las pruebas lean los enlaces enviados. */
@Component
public class CapturedEvents {

  private final List<IntegrationEvent> events = new CopyOnWriteArrayList<>();

  @EventListener
  void on(IntegrationEvent event) {
    events.add(event);
  }

  public void clear() {
    events.clear();
  }

  public <T extends IntegrationEvent> List<T> all(Class<T> type) {
    return events.stream().filter(type::isInstance).map(type::cast).toList();
  }

  public <T extends IntegrationEvent> Optional<T> last(Class<T> type) {
    List<T> matching = all(type);
    return matching.isEmpty() ? Optional.empty() : Optional.of(matching.get(matching.size() - 1));
  }
}
