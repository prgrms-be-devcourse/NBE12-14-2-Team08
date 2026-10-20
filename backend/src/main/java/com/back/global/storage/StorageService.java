package com.back.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// 인증 사진(습관 / 벌칙) 저장 규칙을 담당한다.
// "누가 올릴 수 있는가"는 각 도메인 서비스가 확인한 뒤 이 서비스를 호출한다.
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final String SUPABASE_STORAGE_PATH = "/storage/v1";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

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

    // 제출된 인증 사진 URL 검증 (DB 저장 전에 호출)
    // ① 우리 저장소의 해당 종류 버킷 URL인가
    // ② 경로가 이 회원·이 대상으로 발급된 {memberId}/{targetId}/{UUID}{확장자} 형태인가
    //    - 지금 저장된 사진(currentImageUrl)을 그대로 다시 내는 경우는 생략 (#160 이전 경로 호환)
    // ③ Supabase에 파일이 실제로 있는가 (장애 시 StorageUnavailableException → 502)
    public void validateImageUrl(
            VerificationImageType type,
            Long memberId,
            Long targetId,
            String imageUrl,
            String currentImageUrl
    ) {
        String bucket = type.bucket(properties.storage());
        String bucketUrl = publicUrl(bucket, "");

        if (imageUrl == null || !imageUrl.startsWith(bucketUrl)) {
            throw new IllegalArgumentException("업로드한 사진만 사용할 수 있습니다.");
        }

        String path = imageUrl.substring(bucketUrl.length());

        if (!imageUrl.equals(currentImageUrl) && !isIssuedPath(path, memberId, targetId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 사용할 수 있습니다.");
        }

        if (!supabaseStorageClient.exists(bucket, path)) {
            throw new IllegalArgumentException("업로드된 사진을 확인할 수 없습니다.");
        }
    }

    // createUploadUrl이 발급하는 경로 형태와 정확히 같은지 확인 (../, 쿼리 문자열 등 차단)
    private boolean isIssuedPath(String path, Long memberId, Long targetId) {
        String prefix = memberId + "/" + targetId + "/";

        if (!path.startsWith(prefix)) {
            return false;
        }

        String filename = path.substring(prefix.length());
        int dotIndex = filename.lastIndexOf(".");

        return dotIndex > 0
                && UUID_PATTERN.matcher(filename.substring(0, dotIndex)).matches()
                && ALLOWED_EXTENSIONS.contains(filename.substring(dotIndex));
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
