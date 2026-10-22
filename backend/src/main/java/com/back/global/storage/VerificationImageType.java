package com.back.global.storage;

// 인증 사진 종류: 습관 인증 사진 / 벌칙 인증 사진. 종류마다 저장 버킷이 다르다.
public enum VerificationImageType {
    HABIT,
    PENALTY;

    public String bucket(StorageProperties.Storage storage) {
        return switch (this) {
            case HABIT -> storage.habitBucket();
            case PENALTY -> storage.penaltyBucket();
        };
    }
}
