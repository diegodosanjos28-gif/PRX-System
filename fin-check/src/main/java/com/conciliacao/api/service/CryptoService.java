package com.conciliacao.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;   // 96 bits — recomendado para GCM
    private static final int TAG_LENGTH = 128; // bits

    private final SecretKey aesSecretKey;

    // Retorna: Base64(IV + ciphertext)
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesSecretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plain.getBytes("UTF-8"));

            // Concatena IV + ciphertext para poder extrair o IV na descriptografia
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar dado", e);
        }
    }

    // Espera: Base64(IV + ciphertext)
    public String decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesSecretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao descriptografar dado", e);
        }
    }
}
