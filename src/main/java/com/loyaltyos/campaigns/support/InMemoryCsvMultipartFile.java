package com.loyaltyos.campaigns.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * In-memory CSV payload for API-driven target-customer imports (integration bulk).
 */
public final class InMemoryCsvMultipartFile implements MultipartFile {

    private final String filename;
    private final byte[] content;

    public InMemoryCsvMultipartFile(String filename, byte[] content) {
        this.filename = filename != null && !filename.isBlank() ? filename : "customers.csv";
        this.content = content != null ? content : new byte[0];
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return "text/csv";
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException {
        throw new UnsupportedOperationException("transferTo not supported");
    }
}
