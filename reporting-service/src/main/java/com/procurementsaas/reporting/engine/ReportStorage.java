package com.procurementsaas.reporting.engine;

/**
 * Where rendered reports live.
 *
 * <p>An interface because this is object storage (S3/MinIO) in production and a directory
 * in development, and the engine should not know which. Reports are written once and read
 * occasionally — they do not belong in the database.
 */
public interface ReportStorage {

    /**
     * @return the key needed to read the file back
     */
    String store(String tenantId, Long jobId, String filename, byte[] content);

    byte[] retrieve(String key);
}
