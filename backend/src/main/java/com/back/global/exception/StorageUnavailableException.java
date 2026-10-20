package com.back.global.exception;

// 외부 사진 저장소(Supabase Storage)와 통신하지 못했을 때 사용 (502 BAD_GATEWAY)
public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
