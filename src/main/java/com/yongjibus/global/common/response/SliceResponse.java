package com.yongjibus.global.common.response;

import java.util.List;

import org.springframework.data.domain.Slice;

public record SliceResponse<T>(
  List<T> content,
  boolean hasNext,
  int number,
  int size
) {
  public static <T> SliceResponse<T> from(Slice<T> slice) {
    return new SliceResponse<>(slice.getContent(), slice.hasNext(), slice.getNumber(), slice.getSize());
  }
}