package com.procurementsaas.reporting.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores reports on disk, under a directory per tenant.
 *
 * <p>Swap for an S3-backed implementation and nothing else changes. The per-tenant prefix
 * is not decoration: one tenant's export must never sit where another tenant's key could
 * name it.
 */
@Component
public class FileSystemReportStorage implements ReportStorage {

    private static final Logger log = LoggerFactory.getLogger(FileSystemReportStorage.class);

    private final Path root;

    public FileSystemReportStorage(@Value("${reporting.storage-dir}") String storageDir) {
        this.root = Path.of(storageDir);
    }

    @Override
    public String store(String tenantId, Long jobId, String filename, byte[] content) {
        String key = sanitize(tenantId) + "/" + jobId + "/" + sanitize(filename);
        Path target = root.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            log.debug("Stored report at {} ({} bytes)", key, content.length);
            return key;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store report " + key, ex);
        }
    }

    @Override
    public byte[] retrieve(String key) {
        Path target = root.resolve(key).normalize();
        // A key is built by this service, but reading a caller-supplied path without
        // checking it stays under the root is how a download endpoint becomes a way to
        // read /etc/passwd.
        if (!target.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Refusing to read outside the report store: " + key);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read report " + key, ex);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
