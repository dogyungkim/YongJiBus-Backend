package com.yongjibus.place.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.place.storage.LocalImageStorage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/place-images")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.image.type", havingValue = "local", matchIfMissing = true)
public class LocalImageController {
    private final LocalImageStorage storage;

    @GetMapping("/{storageKey:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String storageKey) {
        Resource image = storage.load(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(storageKey).orElseThrow())
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(image);
    }
}
