package com.procurementsaas.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A grant of authority: while {@code fromUser} is away, {@code toUser} may exercise
 * {@code roleCode} on their behalf, between two dates.
 *
 * <p>Bounded by design. An open-ended delegation is indistinguishable from simply giving
 * someone the role, which is a decision for whoever administers roles — not something to
 * be arranged quietly between two colleagues.
 */
@Entity
@Table(name = "delegation")
public class Delegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The person whose authority is being lent. */
    @Column(name = "from_user", nullable = false, length = 100)
    private String fromUser;

    /** The person who may use it. */
    @Column(name = "to_user", nullable = false, length = 100)
    private String toUser;

    @Column(name = "role_code", nullable = false, length = 60)
    private String roleCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Delegation() {
    }

    public Delegation(String fromUser, String toUser, String roleCode, LocalDate validFrom,
                      LocalDate validTo, String reason) {
        if (fromUser.equals(toUser)) {
            throw new IllegalArgumentException("A user cannot delegate to themselves: " + fromUser);
        }
        if (validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                "Delegation end date " + validTo + " is before its start " + validFrom);
        }
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.roleCode = roleCode;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.reason = reason;
    }

    /** Usable only while unrevoked and inside its window — both ends inclusive. */
    public boolean isValidOn(LocalDate date) {
        return !revoked && !date.isBefore(validFrom) && !date.isAfter(validTo);
    }

    /**
     * Ends the delegation early. Revoked rather than deleted: actions already taken under
     * it must remain explicable.
     */
    public void revoke() {
        this.revoked = true;
    }

    public Long getId() { return id; }
    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public String getRoleCode() { return roleCode; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public boolean isRevoked() { return revoked; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
