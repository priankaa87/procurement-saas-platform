package com.procurementsaas.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A permissioned capability, e.g. {@code FEATURE_USER_MANAGE}. Roles grant features;
 * features map to {@code @PreAuthorize("hasAuthority('...')")} checks in each service.
 */
@Entity
@Table(name = "feature")
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String module;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Feature() {
    }

    public Feature(String code, String name, String module) {
        this.code = code;
        this.name = name;
        this.module = module;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public Instant getCreatedAt() { return createdAt; }
}
