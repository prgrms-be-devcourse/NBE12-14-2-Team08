package com.back.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// 인증 사진(습관 / 벌칙) 저장 규칙을 담당한다.
// "누가 올릴 수 있는가"는 각 도메인 서비스가 확인한 뒤 이 서비스를 호출한다.
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final String SUPABASE_STORAGE_PATH = "/storage/v1";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final StorageProperties properties;
    private final SupabaseStorageClient supabaseStorageClient;

    // 인증 사진 업로드 URL 발급
    // 저장 경로: {memberId}/{targetId}/{UUID}{확장자}  (targetId = 습관 id 또는 벌칙 인증 id)
    public UploadUrlResponse createUploadUrl(
            VerificationImageType type,
            Long memberId,
            Long targetId,
            String filename
    ) {
        String extension = extractExtension(filename);
        String bucket = type.bucket(properties.storage());
        String path = memberId + "/" + targetId + "/" + UUID.randomUUID() + extension;

        String signedPath = supabaseStorageClient.createSignedUploadUrl(
                bucket,
                path,
                properties.storage().urlExpirationSeconds()
        );

        // Supabase가 반환하는 signedPath는 /storage/v1 기준 상대 경로라 직접 붙여야 함
        String uploadUrl = properties.url() + SUPABASE_STORAGE_PATH + signedPath;

        return new UploadUrlResponse(uploadUrl, publicUrl(bucket, path));
    }

    // 업로드된 이미지가 실제로 존재하는지 확인
    public boolean existsHabitImage(String imageUrl) {

        String bucket = properties.storage().habitBucket();
        String bucketUrl = publicUrl(bucket, "");

        // 우리가 발급한 이미지 URL인지 확인
        if (!imageUrl.startsWith(bucketUrl)) {
            return false;
        }

        // URL에서 파일 경로만 가져오기
        String path = imageUrl.substring(bucketUrl.length());

        // Supabase Storage에 실제 파일이 있는지 확인
        return supabaseStorageClient.exists(bucket, path);
    }

    // .jpg, .jpeg, .png, .webp 형식만 허용 (대소문자 무시)
    private String extractExtension(String filename) {
        int dotIndex = filename == null ? -1 : filename.lastIndexOf(".");

        if (dotIndex == -1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        String extension = filename.substring(dotIndex).toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 이미지 파일 형식입니다.");
        }

        return extension;
    }

    private String publicUrl(String bucket, String path) {
        return properties.url() + SUPABASE_STORAGE_PATH + "/object/public/" + bucket + "/" + path;
    }
}
