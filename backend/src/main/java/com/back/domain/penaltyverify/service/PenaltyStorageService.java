package com.back.domain.penaltyverify.service;

import com.back.global.storage.UploadUrlResponse;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class PenaltyStorageService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String penaltyBucket;
    private final int expirationSeconds;

    public PenaltyStorageService(
        @Value("${supabase.url}") String supabaseUrl,
        @Value("${supabase.service-role-key}") String serviceRoleKey,
        @Value("${supabase.storage.penalty-bucket}") String penaltyBucket,
        @Value("${supabase.storage.url-expiration-seconds:300}") int expirationSeconds
    ) {
        this.supabaseUrl = supabaseUrl;
        this.penaltyBucket = penaltyBucket;
        this.expirationSeconds = expirationSeconds;
        this.restClient = RestClient.builder()
            .baseUrl(supabaseUrl + "/storage/v1")
            .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
            .defaultHeader("apikey", serviceRoleKey)
            .build();
    }

    public UploadUrlResponse createUploadUrl(Long memberId, String filename) {
        String extension = extractExtension(filename);
        // 저장 경로: penalties/{memberId}/{UUID}.png
        String storagePath = "penalties/" + memberId + "/" + UUID.randomUUID() + extension;

        Map<?, ?> response = restClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/object/upload/sign/{bucket}/{path}")
                .queryParam("expiresIn", expirationSeconds)
                .build(penaltyBucket, storagePath))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("expiresIn", expirationSeconds))
            .retrieve()
            .body(Map.class);

        if (response == null || !response.containsKey("url")) {
            throw new IllegalStateException("Supabase 서명 URL 발급에 실패했습니다.");
        }

        String signedPath = (String) response.get("url");
        String fullUploadUrl = supabaseUrl + "/storage/v1" + signedPath;
        String publicUrl = String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, penaltyBucket, storagePath);

        return new UploadUrlResponse(fullUploadUrl, publicUrl);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}