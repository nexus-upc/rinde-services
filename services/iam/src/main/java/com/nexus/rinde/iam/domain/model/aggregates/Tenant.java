package com.nexus.rinde.iam.domain.model.aggregates;

import com.nexus.rinde.iam.domain.model.events.TenantRegistered;
import com.nexus.rinde.iam.domain.model.valueobjects.Email;
import com.nexus.rinde.iam.domain.model.valueobjects.IssuedAccessToken;
import com.nexus.rinde.iam.domain.model.valueobjects.Ruc;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantId;
import com.nexus.rinde.iam.domain.model.valueobjects.TenantStatus;
import com.nexus.rinde.shared.domain.exceptions.BusinessRuleException;
import com.nexus.rinde.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Empresa de transporte suscrita a RINDE. Nace pendiente de verificación, pasa a activa al
 * confirmar su correo y luego alterna entre activa y restringida según su suscripción.
 */
@Entity
@Table(schema = "iam", name = "tenants")
public class Tenant extends AuditableAbstractAggregateRoot {

  private static final int MAX_TRADE_NAME_LENGTH = 120;

  @Column(name = "trade_name", nullable = false, length = MAX_TRADE_NAME_LENGTH)
  private String tradeName;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "ruc", nullable = false, length = 11)
  private Ruc ruc;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private TenantStatus status;

  @Column(name = "access_suspended", nullable = false)
  private boolean accessSuspended;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "address", length = 200)
  private String address;

  protected Tenant() {}

  public Tenant(String tradeName, Ruc ruc) {
    if (tradeName == null || tradeName.isBlank() || tradeName.length() > MAX_TRADE_NAME_LENGTH) {
      throw new BusinessRuleException(
          "El nombre comercial es obligatorio y admite hasta 120 caracteres.");
    }
    if (ruc == null) {
      throw new BusinessRuleException("El RUC es obligatorio.");
    }
    this.tradeName = tradeName.trim();
    this.ruc = ruc;
  }

  /**
   * Deja la empresa pendiente de verificación y anuncia el registro con el enlace para verificar el
   * correo del administrador.
   */
  public void register(Email administratorEmail, IssuedAccessToken verificationToken, Instant now) {
    if (status != null) {
      throw new BusinessRuleException("La empresa ya fue registrada.");
    }
    status = TenantStatus.PENDING_VERIFICATION;
    registerEvent(
        new TenantRegistered(
            UUID.randomUUID(),
            now,
            getId(),
            tradeName,
            administratorEmail.address(),
            verificationToken.rawValue(),
            verificationToken.token().expiresAt()));
  }

  /** Activa la empresa cuando su administrador confirma el correo. */
  public void verify() {
    if (status != TenantStatus.PENDING_VERIFICATION) {
      throw new BusinessRuleException("La empresa no está pendiente de verificación.");
    }
    status = TenantStatus.ACTIVE;
  }

  /** Restringe la empresa cuando su suscripción se suspende. */
  public void restrict() {
    if (status != TenantStatus.ACTIVE) {
      throw new BusinessRuleException("Solo una empresa activa puede restringirse.");
    }
    status = TenantStatus.RESTRICTED;
  }

  /** Reactiva la empresa cuando su suscripción vuelve a estar vigente. */
  public void reactivate() {
    if (status != TenantStatus.RESTRICTED) {
      throw new BusinessRuleException("Solo una empresa restringida puede reactivarse.");
    }
    status = TenantStatus.ACTIVE;
  }

  public TenantId getTenantId() {
    return new TenantId(getId());
  }

  public String getTradeName() {
    return tradeName;
  }

  public Ruc getRuc() {
    return ruc;
  }

  public boolean isAccessSuspended() {
    return accessSuspended;
  }

  public void changePlatformAccess(boolean suspended) {
    this.accessSuspended = suspended;
  }

  public TenantStatus getSubscriptionStatus() {
    return status;
  }

  public TenantStatus getStatus() {
    return status;
  }

  public TenantStatus getAccessStatus() {
    return accessSuspended ? TenantStatus.SUSPENDED : status;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddress() {
    return address;
  }
}
