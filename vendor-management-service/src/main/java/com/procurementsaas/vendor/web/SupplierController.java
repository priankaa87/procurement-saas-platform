package com.procurementsaas.vendor.web;

import com.procurementsaas.vendor.dto.Dtos.ContactDto;
import com.procurementsaas.vendor.dto.Dtos.CreateContactRequest;
import com.procurementsaas.vendor.dto.Dtos.CreateDocumentRequest;
import com.procurementsaas.vendor.dto.Dtos.CreateSupplierRequest;
import com.procurementsaas.vendor.dto.Dtos.DocumentDto;
import com.procurementsaas.vendor.dto.Dtos.SupplierDto;
import com.procurementsaas.vendor.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_VIEW')")
    public List<SupplierDto> list(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) String categoryCode) {
        return supplierService.list(status, categoryCode);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_VIEW')")
    public SupplierDto get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_MANAGE')")
    public SupplierDto create(@Valid @RequestBody CreateSupplierRequest request) {
        return supplierService.create(request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_MANAGE')")
    public SupplierDto activate(@PathVariable Long id) {
        return supplierService.activate(id);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_MANAGE')")
    public SupplierDto suspend(@PathVariable Long id) {
        return supplierService.suspend(id);
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_VIEW')")
    public List<ContactDto> listContacts(@PathVariable Long id) {
        return supplierService.listContacts(id);
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_MANAGE')")
    public ContactDto addContact(@PathVariable Long id, @Valid @RequestBody CreateContactRequest request) {
        return supplierService.addContact(id, request);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_VIEW')")
    public List<DocumentDto> listDocuments(@PathVariable Long id) {
        return supplierService.listDocuments(id);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_MANAGE')")
    public DocumentDto addDocument(@PathVariable Long id, @Valid @RequestBody CreateDocumentRequest request) {
        return supplierService.addDocument(id, request);
    }
}
