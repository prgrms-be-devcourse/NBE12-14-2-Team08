package com.back.global.storage;

import com.back.global.exception.StorageUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SupabaseStorageClient {

    private static final String STORAGE_UNAVAILABLE_MESSAGE =
            "사진 저장소와 통신할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    // 인증 제출은 트랜잭션 안에서 Supabase를 부르므로, 응답이 없을 때 DB 커넥션을 오래 쥐지 않도록 끊는다.
    // 시간이 넘으면 IOException(HttpTimeoutException)이 나서 502로 응답한다.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final StorageProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SupabaseStorageClient(
            StorageProperties properties
    ) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // Supabase에서 업로드용 Signed URL을 받는다.
    // expiresInSeconds는 요청에 함께 보내지만, Supabase는 업로드용 Signed URL에는 이 값을 적용하지 않는다.
    // (직접 확인: 5분이 지나도 업로드가 거절되지 않음)
    public String createSignedUploadUrl(
            String bucket,
            String path,
            int expiresInSeconds
    ) {

        String endpoint =
                properties.url()
                        + "/storage/v1/object/upload/sign/"
                        + bucket
                        + "/"
                        + path
                        + "?expiresIn="
                        + expiresInSeconds;

        HttpResponse<String> response;

        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(REQUEST_TIMEOUT)
                            .header(
                                    "Authorization",
                                    "Bearer " + properties.serviceRoleKey()
                            )
                            .header(
                                    "apikey",
                                    properties.serviceRoleKey()
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            "{\"expiresIn\":" + expiresInSeconds + "}"
                                    )
                            )
                            .build();

            response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, e);
        } catch (IOException | IllegalArgumentException e) {
            throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE);
        }

        try {
            JsonNode url = objectMapper.readTree(response.body()).get("url");

            if (url == null || url.asText().isBlank()) {
                throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE);
            }

            return url.asText();

        } catch (IOException e) {
            throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, e);
        }
    }

    // Supabase Storage에 파일이 실제로 존재하는지 확인한다.
    public boolean exists(
            String bucket,
            String path
    ) {

        String endpoint =
                properties.url()
                        + "/storage/v1/object/info/"
                        + bucket
                        + "/"
                        + path;

        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(REQUEST_TIMEOUT)
                            .header(
                                    "Authorization",
                                    "Bearer " + properties.serviceRoleKey()
                            )
                            .header(
                                    "apikey",
                                    properties.serviceRoleKey()
                            )
                            .method(
                                    "HEAD",
                                    HttpRequest.BodyPublishers.noBody()
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            return isFound(response.statusCode());

        } catch (IllegalArgumentException e) {
            // URI로 만들 수 없는 경로 = 우리가 발급한 파일일 수 없음
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, e);
        } catch (IOException e) {
            throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE, e);
        }
    }

    // 200: 있음 / 400·404: 없음 (Supabase는 없는 파일에 400을 주기도 함) / 그 외: 저장소 장애
    static boolean isFound(int statusCode) {
        if (statusCode == 200) {
            return true;
        }
        if (statusCode == 400 || statusCode == 404) {
            return false;
        }
        throw new StorageUnavailableException(STORAGE_UNAVAILABLE_MESSAGE);
    }
}