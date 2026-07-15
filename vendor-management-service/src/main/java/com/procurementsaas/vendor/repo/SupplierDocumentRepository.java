package com.procurementsaas.vendor.repo;

import com.procurementsaas.vendor.domain.SupplierDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, Long> {
    List<SupplierDocument> findBySupplierId(Long supplierId);
}
