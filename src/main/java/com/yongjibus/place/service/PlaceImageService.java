package com.yongjibus.place.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceImageItem;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceImage;
import com.yongjibus.place.domain.PlaceStatus;
import com.yongjibus.place.repository.PlaceImageRepository;
import com.yongjibus.place.repository.PlaceRepository;
import com.yongjibus.place.storage.ImageStorage;

/** Coordinates image processing, object storage, and the short DB write. */
@Service
public class PlaceImageService {
    private static final int MAX_IMAGES = 5;

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository imageRepository;
    private final ImageStorage storage;
    private final PlaceImageProcessor processor;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PlaceImageService(PlaceRepository placeRepository, PlaceImageRepository imageRepository,
            ImageStorage storage, PlaceImageProcessor processor, TransactionTemplate transactionTemplate) {
        this.placeRepository = placeRepository;
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.processor = processor;
        this.transactionTemplate = transactionTemplate;
    }

    public PlaceImageService(PlaceRepository placeRepository, PlaceImageRepository imageRepository,
            ImageStorage storage, PlaceImageProcessor processor) {
        this(placeRepository, imageRepository, storage, processor, null);
    }

    /** Compatibility constructor for focused unit tests and old callers. */
    public PlaceImageService(PlaceRepository placeRepository, PlaceImageRepository imageRepository,
            ImageStorage storage) {
        this(placeRepository, imageRepository, storage, new PlaceImageProcessor(), null);
    }

    public List<PlaceImageItem> add(Long placeId, List<MultipartFile> files) {
        return add(placeId, files, null);
    }

    public List<PlaceImageItem> addForRequest(Long placeId, List<MultipartFile> files) {
        return add(placeId, files, PlaceStatus.PENDING);
    }

    private List<PlaceImageItem> add(Long placeId, List<MultipartFile> files, PlaceStatus requiredStatus) {
        if (files == null || files.isEmpty()) {
            throw new PlaceException(ErrorCode.INVALID_PLACE_IMAGE);
        }
        if (files.size() > MAX_IMAGES) {
            throw new PlaceException(ErrorCode.TOO_MANY_PLACE_IMAGES);
        }

        checkCapacity(placeId, files.size(), requiredStatus);
        List<String> storedKeys = new ArrayList<>(files.size() * 2);
        try {
            List<PlaceImageProcessor.ProcessedImages> processed = files.stream()
                    .map(processor::process)
                    .toList();
            List<StoredImage> storedImages = new ArrayList<>(processed.size());
            for (PlaceImageProcessor.ProcessedImages image : processed) {
                String displayKey = store(image.displayBytes(), storedKeys);
                String thumbnailKey = store(image.thumbnailBytes(), storedKeys);
                if (displayKey.equals(thumbnailKey)) {
                    throw new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED);
                }
                storedImages.add(new StoredImage(displayKey, thumbnailKey));
            }
            return inTransaction(() -> persist(placeId, storedImages, requiredStatus));
        } catch (RuntimeException e) {
            storedKeys.stream().distinct().forEach(this::deleteQuietly);
            throw e;
        }
    }

    public void delete(Long placeId, Long imageId) {
        ImageKeys keys = inTransaction(() -> {
            PlaceImage image = imageRepository.findByIdAndPlace_Id(imageId, placeId)
                    .orElseThrow(() -> new PlaceException(ErrorCode.PLACE_IMAGE_NOT_FOUND));
            imageRepository.delete(image);
            imageRepository.flush();
            return new ImageKeys(image.getStorageKey(), image.getThumbnailStorageKey());
        });
        deleteStoredFiles(keys);
    }

    @Transactional(readOnly = true)
    public List<PlaceImageItem> findByPlaceId(Long placeId) {
        return toItems(imageRepository.findByPlace_IdOrderBySortOrderAscIdAsc(placeId));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<PlaceImageItem>> findByPlaceIds(Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PlaceImageItem>> result = new HashMap<>();
        for (PlaceImage image : imageRepository
                .findByPlace_IdInOrderByPlace_IdAscSortOrderAscIdAsc(placeIds)) {
            result.computeIfAbsent(image.getPlace().getId(), ignored -> new ArrayList<>()).add(toItem(image));
        }
        return result;
    }

    private void checkCapacity(Long placeId, int requestedImages, PlaceStatus requiredStatus) {
        inTransaction(() -> {
            Place place = placeRepository.findById(placeId)
                    .orElseThrow(() -> new PlaceException(ErrorCode.PLACE_NOT_FOUND));
            if (requiredStatus != null && place.getStatus() != requiredStatus) {
                throw new PlaceException(ErrorCode.PLACE_REQUEST_NOT_ALLOWED);
            }
            int existingImages = imageRepository.findByPlace_IdOrderBySortOrderAscIdAsc(placeId).size();
            if (existingImages + requestedImages > MAX_IMAGES) {
                throw new PlaceException(ErrorCode.TOO_MANY_PLACE_IMAGES);
            }
            return null;
        });
    }

    private List<PlaceImageItem> persist(Long placeId, List<StoredImage> storedImages, PlaceStatus requiredStatus) {
        Place place = (requiredStatus == PlaceStatus.PENDING
                ? placeRepository.findByIdForUpdate(placeId) : placeRepository.findById(placeId))
                .orElseThrow(() -> new PlaceException(ErrorCode.PLACE_NOT_FOUND));
        if (requiredStatus != null && place.getStatus() != requiredStatus) {
            throw new PlaceException(ErrorCode.PLACE_REQUEST_NOT_ALLOWED);
        }
        List<PlaceImage> existing = imageRepository.findByPlace_IdOrderBySortOrderAscIdAsc(placeId);
        if (existing.size() + storedImages.size() > MAX_IMAGES) {
            throw new PlaceException(ErrorCode.TOO_MANY_PLACE_IMAGES);
        }

        boolean[] usedOrders = new boolean[MAX_IMAGES];
        for (PlaceImage image : existing) {
            if (image.getSortOrder() >= 0 && image.getSortOrder() < MAX_IMAGES) {
                usedOrders[image.getSortOrder()] = true;
            }
        }
        List<PlaceImage> added = new ArrayList<>(storedImages.size());
        for (StoredImage stored : storedImages) {
            int order = firstAvailable(usedOrders);
            usedOrders[order] = true;
            added.add(PlaceImage.of(place, stored.displayKey(), stored.thumbnailKey(), order));
        }
        imageRepository.saveAllAndFlush(added);

        List<PlaceImage> result = new ArrayList<>(existing.size() + added.size());
        result.addAll(existing);
        result.addAll(added);
        result.sort(Comparator.comparingInt(PlaceImage::getSortOrder));
        return toItems(result);
    }

    private String store(byte[] content, List<String> storedKeys) {
        String key = storage.store(content, "jpg");
        if (key == null || key.isBlank()) {
            throw new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED);
        }
        storedKeys.add(key);
        return key;
    }

    private void deleteStoredFiles(ImageKeys keys) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        uniqueKeys.add(keys.displayKey());
        uniqueKeys.add(keys.thumbnailKey());
        RuntimeException firstFailure = null;
        for (String key : uniqueKeys) {
            try {
                storage.delete(key);
            } catch (RuntimeException e) {
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    private List<PlaceImageItem> toItems(List<PlaceImage> images) {
        return images.stream().map(this::toItem).toList();
    }

    private PlaceImageItem toItem(PlaceImage image) {
        String thumbnailKey = image.getThumbnailStorageKey() == null
                ? image.getStorageKey() : image.getThumbnailStorageKey();
        return new PlaceImageItem(image.getId(), storage.publicUrl(image.getStorageKey()),
                storage.publicUrl(thumbnailKey), image.getSortOrder());
    }

    private static int firstAvailable(boolean[] usedOrders) {
        for (int i = 0; i < usedOrders.length; i++) {
            if (!usedOrders[i]) {
                return i;
            }
        }
        throw new PlaceException(ErrorCode.TOO_MANY_PLACE_IMAGES);
    }

    private void deleteQuietly(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException ignored) {
            // Preserve the original upload or database error.
        }
    }

    private <T> T inTransaction(Supplier<T> action) {
        if (transactionTemplate == null) {
            return action.get();
        }
        return transactionTemplate.execute(status -> action.get());
    }

    private record StoredImage(String displayKey, String thumbnailKey) {
    }

    private record ImageKeys(String displayKey, String thumbnailKey) {
    }
}
