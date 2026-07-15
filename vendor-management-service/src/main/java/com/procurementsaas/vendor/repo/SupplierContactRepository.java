package com.procurementsaas.vendor.repo;

import com.procurementsaas.vendor.domain.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {
    List<SupplierContact> findBySupplierId(Long supplierId);
}
