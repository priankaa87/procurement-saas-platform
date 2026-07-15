package com.procurementsaas.vendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vendor Management Service.
 *
 * <p>Owns the supplier lifecycle: registration, contacts, compliance documents, and the
 * debarment process. Cross-service references (country, item categories) are held as
 * codes owned by the Master Data service — never as foreign keys across databases.
 *
 * <p>Cross-cutting concerns (tenancy, security, error handling) come from platform-common.
 */
@SpringBootApplication
public class VendorManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendorManagementServiceApplication.class, args);
    }
}
