package com.procurementsaas.vendor.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.vendor.domain.Supplier;
import com.procurementsaas.vendor.domain.SupplierContact;
import com.procurementsaas.vendor.domain.SupplierDocument;
import com.procurementsaas.vendor.domain.SupplierStatus;
import com.procurementsaas.vendor.dto.Dtos.ContactDto;
import com.procurementsaas.vendor.dto.Dtos.CreateContactRequest;
import com.procurementsaas.vendor.dto.Dtos.CreateDocumentRequest;
import com.procurementsaas.vendor.dto.Dtos.CreateSupplierRequest;
import com.procurementsaas.vendor.dto.Dtos.DocumentDto;
import com.procurementsaas.vendor.dto.Dtos.SupplierDto;
import com.procurementsaas.vendor.repo.SupplierContactRepository;
import com.procurementsaas.vendor.repo.SupplierDocumentRepository;
import com.procurementsaas.vendor.repo.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierContactRepository contactRepository;
    private final SupplierDocumentRepository documentRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           SupplierContactRepository contactRepository,
                           SupplierDocumentRepository documentRepository) {
        this.supplierRepository = supplierRepository;
        this.contactRepository = contactRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> list(String status, String categoryCode) {
        List<Supplier> suppliers;
        if (status != null) {
            suppliers = supplierRepository.findByStatus(parseStatus(status));
        } else if (categoryCode != null) {
            suppliers = supplierRepository.findByCategoryCodesContaining(categoryCode);
        } else {
            suppliers = supplierRepository.findAll();
        }
        return suppliers.stream().map(VendorMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SupplierDto get(Long id) {
        return VendorMapper.toDto(findSupplier(id));
    }

    public SupplierDto create(CreateSupplierRequest request) {
        if (supplierRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Supplier code already exists: " + request.code());
        }
        Supplier supplier = new Supplier(request.code(), request.name(), request.legalName(),
            request.email(), request.phone(), request.taxId(), request.countryIso2());
        if (request.categoryCodes() != null) {
            supplier.setCategoryCodes(new HashSet<>(request.categoryCodes()));
        }
        return VendorMapper.toDto(supplierRepository.save(supplier));
    }

    /** Approves a supplier so it can be invited to tenders. */
    public SupplierDto activate(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.activate();
        return VendorMapper.toDto(supplierRepository.save(supplier));
    }

    public SupplierDto suspend(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.suspend();
        return VendorMapper.toDto(supplierRepository.save(supplier));
    }

    // --- Contacts ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ContactDto> listContacts(Long supplierId) {
        findSupplier(supplierId);
        return contactRepository.findBySupplierId(supplierId).stream()
            .map(VendorMapper::toDto).toList();
    }

    public ContactDto addContact(Long supplierId, CreateContactRequest request) {
        Supplier supplier = findSupplier(supplierId);
        SupplierContact contact = new SupplierContact(supplier, request.name(), request.email(),
            request.phone(), request.primaryContact());
        return VendorMapper.toDto(contactRepository.save(contact));
    }

    // --- Documents -----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DocumentDto> listDocuments(Long supplierId) {
        findSupplier(supplierId);
        return documentRepository.findBySupplierId(supplierId).stream()
            .map(VendorMapper::toDto).toList();
    }

    public DocumentDto addDocument(Long supplierId, CreateDocumentRequest request) {
        Supplier supplier = findSupplier(supplierId);
        SupplierDocument document = new SupplierDocument(supplier, request.docType(),
            request.fileName(), request.storageKey(), request.expiresAt());
        return VendorMapper.toDto(documentRepository.save(document));
    }

    Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }

    private static SupplierStatus parseStatus(String status) {
        try {
            return SupplierStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown supplier status: " + status);
        }
    }
}
