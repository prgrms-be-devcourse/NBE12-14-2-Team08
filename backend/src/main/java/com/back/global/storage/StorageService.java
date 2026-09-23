package com.back.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageProperties properties;
    private final SupabaseStorageClient supabaseStorageClient;

    // 습관 인증 사진 업로드 URL 발급
    public UploadUrlResponse createHabitUploadUrl(
            Long memberId,
            Long habitId,
            String filename
    ) {

        // 파일 확장자 가져오기
        int dotIndex = filename.lastIndexOf(".");

        if (dotIndex == -1) {
            throw new IllegalArgumentException(
                    "파일 확장자가 없습니다."
            );
        }

        String extension = filename.substring(dotIndex);

        // Storage에 저장할 파일 경로
        String path =
                memberId + "/"
                        + habitId + "/"
                        + UUID.randomUUID()
                        + extension;

        // Supabase에서 업로드 URL 발급
        String signedPath =
                supabaseStorageClient.createSignedUploadUrl(
                        properties.storage().habitBucket(),
                        path
                );

        // 프론트가 사용할 업로드 URL
        String uploadUrl =
                properties.url() + signedPath;

        // 업로드 후 사용할 이미지 URL
        String publicUrl =
                properties.url()
                        + "/storage/v1/object/public/"
                        + properties.storage().habitBucket()
                        + "/"
                        + path;

        return new UploadUrlResponse(
                uploadUrl,
                publicUrl
        );
    }

    // 업로드된 이미지가 실제로 존재하는지 확인
    public boolean existsHabitImage(String imageUrl) {

        String bucketUrl =
                properties.url()
                        + "/storage/v1/object/public/"
                        + properties.storage().habitBucket()
                        + "/";

        // 우리가 발급한 이미지 URL인지 확인
        if (!imageUrl.startsWith(bucketUrl)) {
            return false;
        }

        // URL에서 파일 경로만 가져오기
        String path =
                imageUrl.substring(bucketUrl.length());

        // Supabase Storage에 실제 파일이 있는지 확인
        return supabaseStorageClient.exists(
                properties.storage().habitBucket(),
                path
        );
    }
}