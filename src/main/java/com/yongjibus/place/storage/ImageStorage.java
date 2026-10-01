package com.yongjibus.place.storage;

public interface ImageStorage {
    String store(byte[] content, String extension);

    void delete(String storageKey);

    String publicUrl(String storageKey);
}
