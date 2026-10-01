package com.yongjibus.place.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class LocalImageStorageTest {
    @TempDir
    Path directory;

    @Test
    void storesServesAndDeletesAnImageByStorageKey() throws Exception {
        LocalImageStorage storage = new LocalImageStorage(directory.toString(), "/place-images/");
        String key = storage.store(new byte[] {1, 2, 3}, "jpg");

        assertThat(storage.publicUrl(key)).isEqualTo("/place-images/" + key);
        assertThat(storage.load(key).getInputStream().readAllBytes()).containsExactly(1, 2, 3);
        storage.delete(key);
        assertThat(Files.exists(directory.resolve(key))).isFalse();
    }
}
