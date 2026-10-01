package com.back.global.util;

import java.net.URI;
import java.util.Set;

public class ImageUrlValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    public static void validate(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 없습니다.");
        }

        try {
            URI uri = URI.create(imageUrl);
            String scheme = uri.getScheme();

            // 프로토콜 검증 (http, https 필수)
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("올바른 HTTP/HTTPS URL 형식이 아닙니다.");
            }

            // 쿼리 파라미터를 제외한 순수 경로(Path) 추출
            String path = uri.getPath();
            if (path == null) {
                throw new IllegalArgumentException("올바른 이미지 경로가 아닙니다.");
            }

            // 확장자 검증
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex == -1) {
                throw new IllegalArgumentException("이미지 확장자를 확인할 수 없습니다.");
            }

            String extension = path.substring(dotIndex + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("허용되지 않은 이미지 확장자입니다. (jpg, jpeg, png, webp만 가능)");
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 URL 형식입니다.", e);
        }
    }
}