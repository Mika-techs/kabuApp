package org.kabuapp.kabuapp.core.data;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

/**
 * Encrypts the stored credentials with an AES/GCM key held in the Android Keystore. The key is
 * hardware-backed where available and cannot be exported, so a copy of the database taken off
 * the device - via backup, ADB or a rooted filesystem - is not decryptable elsewhere.
 *
 * <p>Ciphertext is stored as {@code [12-byte IV][ciphertext+tag]} in a single BLOB column.
 */
public class CredentialCipher
{
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "kabuapp-credentials";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    /**
     * @return the encrypted form of {@code plaintext}, or {@code null} when {@code plaintext} is null
     * @throws GeneralSecurityException when the Keystore is unavailable
     */
    public byte[] encrypt(String plaintext) throws GeneralSecurityException
    {
        if (plaintext == null)
        {
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
    }

    /**
     * @return the decrypted string, or {@code null} when {@code stored} is null or malformed
     * @throws GeneralSecurityException when the Keystore is unavailable or the key changed
     */
    public String decrypt(byte[] stored) throws GeneralSecurityException
    {
        if (stored == null || stored.length <= IV_LENGTH)
        {
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, stored, 0, IV_LENGTH));
        byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(stored, IV_LENGTH, stored.length));
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private static synchronized SecretKey key() throws GeneralSecurityException
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry)
            {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
            generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
            return generator.generateKey();
        }
        catch (GeneralSecurityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new GeneralSecurityException("Keystore unavailable", e);
        }
    }
}
