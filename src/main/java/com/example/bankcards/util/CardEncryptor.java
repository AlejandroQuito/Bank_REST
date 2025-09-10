package com.example.bankcards.util;

import com.example.bankcards.exception.EncryptionException;
import com.example.bankcards.util.properties.EncryptionProperties;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class CardEncryptor {

    private final SecretKeySpec secretKey;
    private final EncryptionProperties properties;

    public CardEncryptor(EncryptionProperties properties) {
        this.properties = properties;
        validateKey(properties.getKey());

        this.secretKey = new SecretKeySpec(
                properties.getKey().getBytes(StandardCharsets.UTF_8),
                "AES"
        );

        log.info("CardEncryptor initialized successfully with algorithm: {}", properties.getAlgorithm());
    }

    private void validateKey(String key) {
        if (key == null || key.length() != 16) {
            throw new EncryptionException("Encryption key must be 16 characters long");
        }
    }

    @Named("encrypt")
    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            byte[] iv = new byte[properties.getIvLength()];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                    new GCMParameterSpec(properties.getTagLength() * 8, iv));

            byte[] encrypted = cipher.doFinal(data.getBytes(properties.getCharset()));
            byte[] encryptedWithIv = new byte[properties.getIvLength() + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, properties.getIvLength());
            System.arraycopy(encrypted, 0, encryptedWithIv, properties.getIvLength(), encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            throw new EncryptionException("Encryption failed");
        }
    }

    @Named("decrypt")
    public String decrypt(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[properties.getIvLength()];
            byte[] encrypted = new byte[decoded.length - properties.getIvLength()];
            System.arraycopy(decoded, 0, iv, 0, properties.getIvLength());
            System.arraycopy(decoded, properties.getIvLength(), encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(properties.getTagLength() * 8, iv));

            return new String(cipher.doFinal(encrypted), properties.getCharset());
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed");
        }
    }

    @Named("mask")
    public String mask(String decryptedNumber) {
        if (decryptedNumber == null || decryptedNumber.length() < 4) {
            return String.format(properties.getMaskingPattern(), "****");
        }
        String lastFour = decryptedNumber.substring(decryptedNumber.length() - 4);
        return String.format(properties.getMaskingPattern(), lastFour);
    }

    @Named("maskCardNumber")
    public String maskCardNumber(String encryptedCardNumber) {
        try {
            String decrypted = decrypt(encryptedCardNumber);
            return mask(decrypted);
        } catch (Exception e) {
            return String.format(properties.getMaskingPattern(), "****");
        }
    }
}
