package com.procurementsaas.tenantbilling.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.tenantbilling.domain.Invoice;
import com.procurementsaas.tenantbilling.domain.Plan;
import com.procurementsaas.tenantbilling.domain.Tenant;
import com.procurementsaas.tenantbilling.dto.Dtos.InvoiceDto;
import com.procurementsaas.tenantbilling.dto.Dtos.PlanDto;
import com.procurementsaas.tenantbilling.repo.InvoiceRepository;
import com.procurementsaas.tenantbilling.repo.PlanRepository;
import com.procurementsaas.tenantbilling.repo.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Plans and invoicing. */
@Service
@Transactional
public class BillingService {

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingService(TenantRepository tenantRepository, PlanRepository planRepository,
                          InvoiceRepository invoiceRepository) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listPlans() {
        return planRepository.findAll().stream().map(TenantMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> listInvoices(String tenantKey) {
        Tenant tenant = findTenant(tenantKey);
        return invoiceRepository.findByTenantIdOrderByPeriodStartDesc(tenant.getId()).stream()
            .map(TenantMapper::toDto).toList();
    }

    /**
     * Issues the invoice for one monthly period.
     *
     * <p>Refuses to bill the same period twice — billing jobs get retried, and a retry
     * must not charge a customer again. The database enforces this as well, so a race
     * between two runs cannot slip through.
     *
     * <p>The plan's price is copied onto the invoice, not referenced, so the invoice
     * remains an accurate record of what was charged even if the plan price later changes.
     */
    public InvoiceDto generateInvoice(String tenantKey, LocalDate periodStart) {
        Tenant tenant = findTenant(tenantKey);
        LocalDate start = periodStart != null
            ? periodStart.withDayOfMonth(1)
            : LocalDate.now().withDayOfMonth(1);

        if (invoiceRepository.existsByTenantIdAndPeriodStart(tenant.getId(), start)) {
            throw new IllegalArgumentException(
                "Tenant " + tenantKey + " is already invoiced for the period starting " + start);
        }
        Plan plan = planRepository.findByCode(tenant.getPlanCode())
            .orElseThrow(() -> new NotFoundException("Plan not found: " + tenant.getPlanCode()));

        LocalDate end = start.plusMonths(1).minusDays(1);
        Invoice invoice = new Invoice(tenant, plan.getCode(), start, end, plan.getPriceMonthly(),
            plan.getCurrencyCode());
        return TenantMapper.toDto(invoiceRepository.save(invoice));
    }

    public InvoiceDto markPaid(Long invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        invoice.markPaid();
        return TenantMapper.toDto(invoiceRepository.save(invoice));
    }

    public InvoiceDto voidInvoice(Long invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        invoice.voidInvoice();
        return TenantMapper.toDto(invoiceRepository.save(invoice));
    }

    private Tenant findTenant(String tenantKey) {
        return tenantRepository.findByTenantKey(tenantKey)
            .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantKey));
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
    }
}
