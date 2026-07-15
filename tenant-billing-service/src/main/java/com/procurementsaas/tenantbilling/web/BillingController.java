package com.procurementsaas.tenantbilling.web;

import com.procurementsaas.tenantbilling.dto.Dtos.GenerateInvoiceRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.InvoiceDto;
import com.procurementsaas.tenantbilling.dto.Dtos.PlanDto;
import com.procurementsaas.tenantbilling.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /** The plan catalogue is public to any authenticated caller — it is a price list. */
    @GetMapping("/plans")
    public List<PlanDto> listPlans() {
        return billingService.listPlans();
    }

    @GetMapping("/tenants/{tenantKey}/invoices")
    @PreAuthorize("hasAuthority('FEATURE_BILLING_VIEW')")
    public List<InvoiceDto> listInvoices(@PathVariable String tenantKey) {
        return billingService.listInvoices(tenantKey);
    }

    @PostMapping("/tenants/{tenantKey}/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_BILLING_MANAGE')")
    public InvoiceDto generate(@PathVariable String tenantKey,
                               @RequestBody(required = false) GenerateInvoiceRequest request) {
        return billingService.generateInvoice(tenantKey,
            request == null ? null : request.periodStart());
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAuthority('FEATURE_BILLING_MANAGE')")
    public InvoiceDto markPaid(@PathVariable Long id) {
        return billingService.markPaid(id);
    }

    @PostMapping("/invoices/{id}/void")
    @PreAuthorize("hasAuthority('FEATURE_BILLING_MANAGE')")
    public InvoiceDto voidInvoice(@PathVariable Long id) {
        return billingService.voidInvoice(id);
    }
}
