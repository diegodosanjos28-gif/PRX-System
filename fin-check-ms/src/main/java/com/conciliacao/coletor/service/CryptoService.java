package com.conciliacao.coletor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Base64;

/**
 * Descriptografa credenciais Conciflex armazenadas no banco.
 * Implementação idêntica ao backend principal — usa a mesma chave AES-256-GCM.
 * O método encrypt não é necessário neste microserviço.
 */
@Service
@RequiredArgsConstructor
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH  = 12;  // 96 bits — recomendado para GCM
    private static final int TAG_LENGTH = 128; // bits

    private final SecretKey aesSecretKey;

    /**
     * Descriptografa uma string criptografada com AES-256-GCM.
     * Formato esperado: Base64(IV[12 bytes] + ciphertext).
     * Retorna null em caso de falha — o chamador deve verificar e registrar o erro.
     */
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
            return null;
        }
    }


}
