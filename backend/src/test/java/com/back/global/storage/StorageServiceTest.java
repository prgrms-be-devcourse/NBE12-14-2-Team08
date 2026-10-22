package com.back.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.back.global.exception.StorageUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    private static final String SUPABASE_URL = "https://supabase.test";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Mock
    private SupabaseStorageClient supabaseStorageClient;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(
            SUPABASE_URL,
            "service-role-key",
            new StorageProperties.Storage("habit-bucket", "penalty-bucket", 300)
        );
        storageService = new StorageService(properties, supabaseStorageClient);
    }

    @Test
    @DisplayName("습관 사진은 습관 버킷의 {회원id}/{습관id}/{UUID}.{확장자} 경로로 발급된다")
    void 습관_사진_업로드_URL() {
        given(supabaseStorageClient.createSignedUploadUrl(eq("habit-bucket"), anyString(), eq(300)))
            .willReturn("/object/upload/sign/habit-bucket/signed?token=abc");

        UploadUrlResponse response = storageService.createUploadUrl(VerificationImageType.HABIT, 7L, 50L, "today.png");

        ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(supabaseStorageClient).createSignedUploadUrl(eq("habit-bucket"), path.capture(), eq(300));

        assertThat(path.getValue()).matches("7/50/" + UUID_PATTERN + "\\.png");
        assertThat(response.uploadUrl())
            .isEqualTo(SUPABASE_URL + "/storage/v1/object/upload/sign/habit-bucket/signed?token=abc");
        assertThat(response.publicUrl())
            .isEqualTo(SUPABASE_URL + "/storage/v1/object/public/habit-bucket/" + path.getValue());
    }

    @Test
    @DisplayName("벌칙 사진은 벌칙 버킷의 {회원id}/{벌칙id}/{UUID}.{확장자} 경로로 발급된다")
    void 벌칙_사진_업로드_URL() {
        given(supabaseStorageClient.createSignedUploadUrl(eq("penalty-bucket"), anyString(), eq(300)))
            .willReturn("/object/upload/sign/penalty-bucket/signed?token=abc");

        UploadUrlResponse response = storageService.createUploadUrl(VerificationImageType.PENALTY, 7L, 3L, "proof.jpg");

        ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(supabaseStorageClient).createSignedUploadUrl(eq("penalty-bucket"), path.capture(), eq(300));

        assertThat(path.getValue()).matches("7/3/" + UUID_PATTERN + "\\.jpg");
        assertThat(response.publicUrl()).startsWith(SUPABASE_URL + "/storage/v1/object/public/penalty-bucket/7/3/");
    }

    @ParameterizedTest(name = "{0} 허용")
    @ValueSource(strings = {"a.jpg", "a.jpeg", "a.png", "a.webp", "a.PNG", "a.Jpeg"})
    @DisplayName("jpg, jpeg, png, webp는 대소문자 상관없이 허용하고 소문자 확장자로 저장한다")
    void 허용_확장자(String filename) {
        given(supabaseStorageClient.createSignedUploadUrl(anyString(), anyString(), anyInt()))
            .willReturn("/object/upload/sign/habit-bucket/signed");

        UploadUrlResponse response = storageService.createUploadUrl(VerificationImageType.HABIT, 7L, 50L, filename);

        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        assertThat(response.publicUrl()).endsWith(extension);
    }

    @ParameterizedTest(name = "{0} 거절")
    @ValueSource(strings = {"a.gif", "a.exe", "a.svg"})
    @DisplayName("허용되지 않은 확장자는 거절하고 Supabase를 호출하지 않는다")
    void 허용되지_않은_확장자(String filename) {
        assertThatThrownBy(() -> storageService.createUploadUrl(VerificationImageType.PENALTY, 7L, 3L, filename))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("허용되지 않는 이미지 파일 형식입니다.");

        verify(supabaseStorageClient, never()).createSignedUploadUrl(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("확장자가 없으면 거절한다 (기본 확장자를 붙이지 않는다)")
    void 확장자_없음() {
        assertThatThrownBy(() -> storageService.createUploadUrl(VerificationImageType.PENALTY, 7L, 3L, "proof"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("파일 확장자가 없습니다.");

        verify(supabaseStorageClient, never()).createSignedUploadUrl(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Supabase 통신 실패는 StorageUnavailableException(502)으로 전달된다")
    void 저장소_통신_실패() {
        given(supabaseStorageClient.createSignedUploadUrl(anyString(), anyString(), anyInt()))
            .willThrow(new StorageUnavailableException("사진 저장소와 통신할 수 없습니다. 잠시 후 다시 시도해 주세요."));

        assertThatThrownBy(() -> storageService.createUploadUrl(VerificationImageType.HABIT, 7L, 50L, "today.png"))
            .isInstanceOf(StorageUnavailableException.class);
    }
}
