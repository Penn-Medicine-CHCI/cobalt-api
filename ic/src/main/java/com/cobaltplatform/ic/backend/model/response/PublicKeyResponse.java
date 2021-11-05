package com.cobaltplatform.ic.backend.model.response;

public class PublicKeyResponse {
    private String format;
    private String jcaAlgorithm;
    private byte[] publicKey;
    private String algorithm;

    public String getFormat() {
        return format;
    }

    public PublicKeyResponse setFormat(final String format) {
        this.format = format;
        return this;
    }

    public String getJcaAlgorithm() {
        return jcaAlgorithm;
    }

    public PublicKeyResponse setJcaAlgorithm(final String jcaAlgorithm) {
        this.jcaAlgorithm = jcaAlgorithm;
        return this;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public PublicKeyResponse setPublicKey(final byte[] publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public PublicKeyResponse setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
        return this;
    }
}
