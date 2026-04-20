package com.conciliacao.coletor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Decrypts Conciflex credentials stored in the database.
 * Implementation is identical to the main backend — uses the same AES-256-GCM key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH  = 12;  // 96-bit IV — recommended for GCM
    private static final int    TAG_LENGTH = 128; // authentication tag in bits

    private final SecretKey aesSecretKey;

    /**
     * Decrypts a string encrypted with AES-256-GCM.
     * Expected format: Base64(IV[12 bytes] + ciphertext).
     * Returns null on any failure — callers must check and log the error.
     */
    public String decrypt(String encrypted) {
        try {
            byte[] combined   = Base64.getDecoder().decode(encrypted);
            byte[] iv         = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesSecretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Decryption failed: {}", e.getMessage());
            return null;
        }
    }
}
