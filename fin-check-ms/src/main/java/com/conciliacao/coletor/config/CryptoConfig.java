package com.conciliacao.coletor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

@Configuration
public class CryptoConfig {

    @Value("${crypto.secret-key}")
    private String rawKey;

    /**
     * Derives a 32-byte AES-256 key from the CRYPTO_SECRET_KEY environment variable.
     * Identical to the main backend — must use the same key to decrypt stored Conciflex credentials.
     */
    @Bean
    public SecretKey aesSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = Arrays.copyOf(
            digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)),
            32
        );
        return new SecretKeySpec(keyBytes, "AES");
    }
}
