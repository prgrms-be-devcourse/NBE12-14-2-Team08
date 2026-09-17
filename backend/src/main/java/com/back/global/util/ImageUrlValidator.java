package com.back.global.util;

import java.util.Set;

public class ImageUrlValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    public static void validate(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 없습니다.");
        }

        int dotIndex = imageUrl.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new IllegalArgumentException("이미지 확장자를 확인할 수 없습니다.");
        }

        String extension = imageUrl.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않은 이미지 확장자입니다. (jpg, png만 가능)");
        }
    }
}
