package com.back.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record StorageProperties(
        String url,
        String serviceRoleKey,
        Storage storage
) {

    public record Storage(
            String habitBucket,
            String penaltyBucket,
            int urlExpirationSeconds
    ) {
    }
}
