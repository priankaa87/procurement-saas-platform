package com.procurementsaas.vendor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A named contact person at a supplier. */
@Entity
@Table(name = "supplier_contact")
public class SupplierContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact = false;

    protected SupplierContact() {
    }

    public SupplierContact(Supplier supplier, String name, String email, String phone,
                           boolean primaryContact) {
        this.supplier = supplier;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.primaryContact = primaryContact;
    }

    public Long getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isPrimaryContact() { return primaryContact; }
    public void setPrimaryContact(boolean primaryContact) { this.primaryContact = primaryContact; }
}
