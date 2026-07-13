package com.procurementsaas.masterdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A country, keyed by ISO 3166-1 alpha-2 code. */
@Entity
@Table(name = "country")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2)
    private String iso2;

    @Column(nullable = false, length = 100)
    private String name;

    protected Country() {
    }

    public Country(String iso2, String name) {
        this.iso2 = iso2;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getIso2() { return iso2; }
    public void setIso2(String iso2) { this.iso2 = iso2; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
