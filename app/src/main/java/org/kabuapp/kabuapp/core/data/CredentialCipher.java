package org.kabuapp.kabuapp.core.data;

import lombok.AllArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Encrypts the stored credentials with AES/GCM. Ciphertext is framed as
 * {@code [12-byte IV][ciphertext+tag]} in a single BLOB column.
 *
 * @see CredentialKeys for where the key comes from
 */
@AllArgsConstructor
public class CredentialCipher
{
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final CredentialKeys keys;

    /**
     * @return the encrypted form of {@code plaintext}, or {@code null} when {@code plaintext} is null
     * @throws GeneralSecurityException when the key is unavailable
     */
    public byte[] encrypt(String plaintext) throws GeneralSecurityException
    {
        if (plaintext == null)
        {
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keys.key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
    }

    /**
     * @return the decrypted string, or {@code null} when {@code stored} is null or too short to
     *     contain both an IV and a payload
     * @throws GeneralSecurityException when the key is unavailable or the payload was tampered with
     */
    public String decrypt(byte[] stored) throws GeneralSecurityException
    {
        if (stored == null || stored.length <= IV_LENGTH)
        {
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keys.key(), new GCMParameterSpec(TAG_LENGTH_BITS, stored, 0, IV_LENGTH));
        return new String(cipher.doFinal(Arrays.copyOfRange(stored, IV_LENGTH, stored.length)), StandardCharsets.UTF_8);
    }
}
