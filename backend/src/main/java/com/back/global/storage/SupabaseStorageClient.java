package com.back.global.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class SupabaseStorageClient {

    private final StorageProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SupabaseStorageClient(
            StorageProperties properties
    ) {
        this.properties = properties;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // Supabase에서 업로드용 Signed URL을 받는다.
    public String createSignedUploadUrl(
            String bucket,
            String path
    ) {

        String endpoint =
                properties.url()
                        + "/storage/v1/object/upload/sign/"
                        + bucket
                        + "/"
                        + path;

        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header(
                                    "Authorization",
                                    "Bearer " + properties.serviceRoleKey()
                            )
                            .header(
                                    "apikey",
                                    properties.serviceRoleKey()
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.noBody()
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(
                        "Supabase 업로드 URL 발급에 실패했습니다."
                );
            }

            JsonNode json =
                    objectMapper.readTree(response.body());

            return json.get("url").asText();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Supabase Storage와 통신할 수 없습니다.",
                    e
            );
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

            return response.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }
}