package com.procurementsaas.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A report the platform knows how to produce.
 *
 * <p>Held as data rather than code so the catalogue is inspectable and a report can be
 * retired without a redeploy. {@code providerCode} names the data source that fills it.
 */
@Entity
@Table(name = "report_definition")
public class ReportDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportFormat format;

    /** Which data provider supplies this report's rows. */
    @Column(name = "provider_code", nullable = false, length = 60)
    private String providerCode;

    @Column(nullable = false)
    private boolean active = true;

    protected ReportDefinition() {
    }

    public ReportDefinition(String code, String name, String description, ReportFormat format,
                            String providerCode) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.format = format;
        this.providerCode = providerCode;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ReportFormat getFormat() { return format; }
    public String getProviderCode() { return providerCode; }
    public boolean isActive() { return active; }
}
