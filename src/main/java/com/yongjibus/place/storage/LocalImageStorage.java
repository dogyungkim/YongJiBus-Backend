package com.yongjibus.place.storage;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;

@Component
@ConditionalOnProperty(name = "storage.image.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {
    private final Path root;
    private final String publicPath;

    public LocalImageStorage(
            @Value("${storage.image.local.root:./data/place-images}") String root,
            @Value("${storage.image.local.public-path:/place-images}") String publicPath) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create image storage directory", e);
        }
    }

    @Override
    public String store(byte[] content, String extension) {
        if (content == null || extension == null || !extension.matches("[A-Za-z0-9]{1,8}")) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED);
        }
        String storageKey = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        Path target = resolve(storageKey);
        try {
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return storageKey;
        } catch (FileAlreadyExistsException e) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED);
        } catch (IOException e) {
            tryDelete(target);
            throw new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public String publicUrl(String storageKey) {
        return publicPath + "/" + storageKey;
    }

    public Resource load(String storageKey) {
        try {
            Resource resource = new UrlResource(resolve(storageKey).toUri());
            if (!resource.isReadable()) {
                throw new PlaceException(ErrorCode.PLACE_IMAGE_NOT_FOUND);
            }
            return resource;
        } catch (IOException e) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_NOT_FOUND);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_NOT_FOUND);
        }
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_NOT_FOUND);
        }
        return target;
    }

    private static String normalizePublicPath(String path) {
        String value = path == null ? "" : path.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? "/place-images" : value;
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup after a failed write.
        }
    }
}
