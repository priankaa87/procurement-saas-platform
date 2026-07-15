package com.procurementsaas.vendor.service;

import com.procurementsaas.vendor.domain.Supplier;
import com.procurementsaas.vendor.domain.SupplierContact;
import com.procurementsaas.vendor.domain.SupplierDebarment;
import com.procurementsaas.vendor.domain.SupplierDocument;
import com.procurementsaas.vendor.dto.Dtos.ContactDto;
import com.procurementsaas.vendor.dto.Dtos.DebarmentDto;
import com.procurementsaas.vendor.dto.Dtos.DocumentDto;
import com.procurementsaas.vendor.dto.Dtos.SupplierDto;

import java.util.TreeSet;

/** Maps vendor entities to API DTOs. */
public final class VendorMapper {

    private VendorMapper() {
    }

    public static SupplierDto toDto(Supplier s) {
        return new SupplierDto(s.getId(), s.getCode(), s.getName(), s.getLegalName(), s.getEmail(),
            s.getPhone(), s.getTaxId(), s.getStatus().name(), s.getCountryIso2(),
            new TreeSet<>(s.getCategoryCodes()));
    }

    public static ContactDto toDto(SupplierContact c) {
        return new ContactDto(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.isPrimaryContact());
    }

    public static DocumentDto toDto(SupplierDocument d) {
        return new DocumentDto(d.getId(), d.getDocType(), d.getFileName(), d.getStorageKey(),
            d.getExpiresAt(), d.isExpired());
    }

    public static DebarmentDto toDto(SupplierDebarment d) {
        return new DebarmentDto(d.getId(), d.getReason(), d.getDebarredFrom(),
            d.getDebarredUntil(), d.isActive());
    }
}
