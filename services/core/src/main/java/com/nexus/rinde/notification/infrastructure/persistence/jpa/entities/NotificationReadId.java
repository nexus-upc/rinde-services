package com.nexus.rinde.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/** Clave compuesta para el registro de lectura por notificación y usuario. */
@Embeddable
public record NotificationReadId(
    @Column(name = "notification_id", nullable = false, updatable = false) UUID notificationId,
    @Column(name = "user_id", nullable = false, updatable = false) UUID userId)
    implements Serializable {}
