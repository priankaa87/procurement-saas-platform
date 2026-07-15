package com.procurementsaas.tenantbilling.service;

import com.procurementsaas.tenantbilling.domain.Invoice;
import com.procurementsaas.tenantbilling.domain.Plan;
import com.procurementsaas.tenantbilling.domain.Tenant;
import com.procurementsaas.tenantbilling.dto.Dtos.InvoiceDto;
import com.procurementsaas.tenantbilling.dto.Dtos.PlanDto;
import com.procurementsaas.tenantbilling.dto.Dtos.TenantDto;

/** Maps control-plane entities to API DTOs. */
public final class TenantMapper {

    private TenantMapper() {
    }

    public static TenantDto toDto(Tenant t) {
        return new TenantDto(t.getId(), t.getTenantKey(), t.getName(), t.getSchemaName(),
            t.getStatus().name(), t.getPlanCode(), t.getContactEmail(), t.getCreatedAt(),
            t.getActivatedAt());
    }

    public static PlanDto toDto(Plan p) {
        return new PlanDto(p.getId(), p.getCode(), p.getName(), p.getPriceMonthly(),
            p.getCurrencyCode(), p.getMaxUsers(), p.getMaxTenders());
    }

    public static InvoiceDto toDto(Invoice i) {
        return new InvoiceDto(i.getId(), i.getTenant().getTenantKey(), i.getPlanCode(),
            i.getPeriodStart(), i.getPeriodEnd(), i.getAmount(), i.getCurrencyCode(),
            i.getStatus().name(), i.getIssuedAt(), i.getPaidAt());
    }
}
