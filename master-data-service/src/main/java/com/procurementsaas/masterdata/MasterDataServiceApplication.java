package com.procurementsaas.masterdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Master Data Service.
 *
 * <p>Owns the platform's shared reference data — measurement units, currencies, item
 * categories and items, and geography. This data is read-heavy and slow-changing, so read
 * endpoints are cached; other services consume it via API (and cache locally) rather than
 * joining across databases.
 */
@EnableCaching
@SpringBootApplication
public class MasterDataServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MasterDataServiceApplication.class, args);
    }
}
