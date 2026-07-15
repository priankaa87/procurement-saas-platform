package com.procurementsaas.tenantbilling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A subscription plan: what it costs and what it entitles a tenant to. */
@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_monthly", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /** Entitlement limits. {@code -1} means unlimited. */
    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(name = "max_tenders", nullable = false)
    private int maxTenders;

    protected Plan() {
    }

    public Plan(String code, String name, BigDecimal priceMonthly, String currencyCode,
                int maxUsers, int maxTenders) {
        this.code = code;
        this.name = name;
        this.priceMonthly = priceMonthly;
        this.currencyCode = currencyCode;
        this.maxUsers = maxUsers;
        this.maxTenders = maxTenders;
    }

    /** Limit for a metric, or {@code -1} if the metric is not capped by this plan. */
    public int limitFor(String metric) {
        return switch (metric) {
            case "users" -> maxUsers;
            case "tenders" -> maxTenders;
            default -> -1;
        };
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getPriceMonthly() { return priceMonthly; }
    public String getCurrencyCode() { return currencyCode; }
    public int getMaxUsers() { return maxUsers; }
    public int getMaxTenders() { return maxTenders; }
}
