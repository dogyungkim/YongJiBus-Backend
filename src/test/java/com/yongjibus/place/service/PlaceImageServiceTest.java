package com.yongjibus.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.place.domain.PlaceImage;
import com.yongjibus.place.repository.PlaceImageRepository;
import com.yongjibus.place.repository.PlaceRepository;
import com.yongjibus.place.storage.ImageStorage;

class PlaceImageServiceTest {
    @Test
    void storesAValidImageAndReturnsItsPublicUrl() throws Exception {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, new PlaceImageProcessor());
        Place place = place();
        MockMultipartFile file = new MockMultipartFile(
                "images", "place.png", "image/png",
                png());
        when(places.findById(1L)).thenReturn(Optional.of(place));
        when(images.findByPlace_IdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());
        when(storage.store(any(byte[].class), eq("jpg"))).thenReturn("display-key.jpg", "thumbnail-key.jpg");
        when(storage.publicUrl("display-key.jpg")).thenReturn("/place-images/display-key.jpg");
        when(storage.publicUrl("thumbnail-key.jpg")).thenReturn("/place-images/thumbnail-key.jpg");
        when(images.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.add(1L, List.of(file));

        assertThat(result).singleElement().satisfies(image -> {
            assertThat(image.imageUrl()).isEqualTo("/place-images/display-key.jpg");
            assertThat(image.thumbnailUrl()).isEqualTo("/place-images/thumbnail-key.jpg");
            assertThat(image.sortOrder()).isZero();
        });
    }

    @Test
    void userRequestRejectsImagesForNonPendingPlace() throws Exception {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, new PlaceImageProcessor());
        when(places.findById(1L)).thenReturn(Optional.of(Place.approved("장소", "주소",
                BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766),
                "1234567890123456789012345", PlaceCategory.CAFE, null, "1",
                "https://place.map.kakao.com/1", Member.builder().id(1L).username("operator").build())));

        assertThatThrownBy(() -> service.addForRequest(1L, List.of(new MockMultipartFile(
                "images", "place.png", "image/png", png()))))
                .isInstanceOfSatisfying(PlaceException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PLACE_REQUEST_NOT_ALLOWED));
        verifyNoInteractions(storage);
        verifyNoInteractions(images);
    }

    @Test
    void userRequestRechecksPendingStatusBeforePersistAndCleansStoredFiles() throws Exception {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, new PlaceImageProcessor());
        Place pending = place();
        Place approved = Place.approved("장소", "주소", BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766),
                "1234567890123456789012345", PlaceCategory.CAFE, null, "1",
                "https://place.map.kakao.com/1", Member.builder().id(1L).username("operator").build());
        when(places.findById(1L)).thenReturn(Optional.of(pending));
        when(places.findByIdForUpdate(1L)).thenReturn(Optional.of(approved));
        when(images.findByPlace_IdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());
        when(storage.store(any(byte[].class), eq("jpg"))).thenReturn("display.jpg", "thumbnail.jpg");

        assertThatThrownBy(() -> service.addForRequest(1L, List.of(new MockMultipartFile(
                "images", "place.png", "image/png", png()))))
                .isInstanceOfSatisfying(PlaceException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PLACE_REQUEST_NOT_ALLOWED));
        verify(storage).delete("display.jpg");
        verify(storage).delete("thumbnail.jpg");
    }

    @Test
    void rejectsAFileWhoseBytesDoNotMatchItsImageContentType() {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, new PlaceImageProcessor());
        when(places.findById(1L)).thenReturn(Optional.of(place()));
        when(images.findByPlace_IdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile(
                "images", "fake.jpg", "image/jpeg", "not-an-image".getBytes());

        assertThatThrownBy(() -> service.add(1L, List.of(file)))
                .isInstanceOfSatisfying(PlaceException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PLACE_IMAGE));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsMoreThanFiveImagesPerPlace() {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, new PlaceImageProcessor());
        Place place = place();
        when(places.findById(1L)).thenReturn(Optional.of(place));
        when(images.findByPlace_IdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(
                PlaceImage.of(place, "0.jpg", 0), PlaceImage.of(place, "1.jpg", 1),
                PlaceImage.of(place, "2.jpg", 2), PlaceImage.of(place, "3.jpg", 3),
                PlaceImage.of(place, "4.jpg", 4)));
        MockMultipartFile file = new MockMultipartFile(
                "images", "place.jpg", "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});

        assertThatThrownBy(() -> service.add(1L, List.of(file)))
                .isInstanceOfSatisfying(PlaceException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_PLACE_IMAGES));
        verifyNoInteractions(storage);
    }

    @Test
    void removesAlreadyStoredDisplayWhenThumbnailStorageFails() {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageProcessor processor = mock(PlaceImageProcessor.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, processor);
        MockMultipartFile file = new MockMultipartFile("images", "place.jpg", "image/jpeg", new byte[] {1});
        when(places.findById(1L)).thenReturn(Optional.of(place()));
        when(images.findByPlace_IdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());
        when(processor.process(file)).thenReturn(new PlaceImageProcessor.ProcessedImages(new byte[] {1}, new byte[] {2}));
        when(storage.store(any(byte[].class), eq("jpg"))).thenReturn("display.jpg")
                .thenThrow(new PlaceException(ErrorCode.PLACE_IMAGE_STORAGE_FAILED));

        assertThatThrownBy(() -> service.add(1L, List.of(file)))
                .isInstanceOf(PlaceException.class);

        verify(storage).delete("display.jpg");
    }

    @Test
    void removesBothStoredFilesWhenDatabaseWriteFails() {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageProcessor processor = mock(PlaceImageProcessor.class);
        PlaceImageService service = new PlaceImageService(places, images, storage, processor);
        MockMultipartFile file = new MockMultipartFile("images", "place.jpg", "image/jpeg", new byte[] {1});
        when(places.findById(1L)).thenReturn(Optional.of(place()));
        when(images.findByPlace_IdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());
        when(processor.process(file)).thenReturn(new PlaceImageProcessor.ProcessedImages(new byte[] {1}, new byte[] {2}));
        when(storage.store(any(byte[].class), eq("jpg"))).thenReturn("display.jpg", "thumbnail.jpg");
        when(images.saveAllAndFlush(any())).thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> service.add(1L, List.of(file)))
                .isInstanceOf(RuntimeException.class);

        verify(storage).delete("display.jpg");
        verify(storage).delete("thumbnail.jpg");
    }

    @Test
    void deletesLegacyImageKeyOnlyOnce() {
        PlaceRepository places = mock(PlaceRepository.class);
        PlaceImageRepository images = mock(PlaceImageRepository.class);
        ImageStorage storage = mock(ImageStorage.class);
        PlaceImageService service = new PlaceImageService(places, images, storage);
        Place place = place();
        PlaceImage image = PlaceImage.of(place, "legacy.jpg", 0);
        when(images.findByIdAndPlace_Id(2L, 1L)).thenReturn(Optional.of(image));

        service.delete(1L, 2L);

        verify(storage).delete("legacy.jpg");
    }

    private static Place place() {
        return Place.pending("장소", "주소", BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766),
                "1234567890123456789012345", PlaceCategory.CAFE, null, "1",
                "https://place.map.kakao.com/1", Member.builder().id(1L).username("maker").build());
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
