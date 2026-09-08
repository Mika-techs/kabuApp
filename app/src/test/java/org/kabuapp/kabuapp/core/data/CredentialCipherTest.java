package org.kabuapp.kabuapp.core.data;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Exercises the cipher's framing and round-trip with an ordinary AES key. The Android Keystore
 * provider only exists on a device, so {@link CredentialKeys.Keystore} itself is not covered here.
 */
public class CredentialCipherTest
{
    private static final int KEY_BITS = 256;

    private CredentialCipher cipher;

    @Before
    public void setUp() throws GeneralSecurityException
    {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(KEY_BITS);
        SecretKey key = generator.generateKey();
        cipher = new CredentialCipher(() -> key);
    }

    @Test
    public void decryptUndoesEncrypt() throws GeneralSecurityException
    {
        assertEquals("hunter2", cipher.decrypt(cipher.encrypt("hunter2")));
    }

    @Test
    public void unicodeSurvivesTheRoundTrip() throws GeneralSecurityException
    {
        assertEquals("Muster-Straße-öäü", cipher.decrypt(cipher.encrypt("Muster-Straße-öäü")));
    }

    @Test
    public void ciphertextDoesNotContainThePlaintext() throws GeneralSecurityException
    {
        byte[] encrypted = cipher.encrypt("hunter2");
        assertFalse(new String(encrypted, StandardCharsets.ISO_8859_1).contains("hunter2"));
    }

    @Test
    public void aFreshIvIsUsedEachTime() throws GeneralSecurityException
    {
        assertFalse(Arrays.equals(cipher.encrypt("hunter2"), cipher.encrypt("hunter2")));
    }

    @Test
    public void nullPassesThroughBothDirections() throws GeneralSecurityException
    {
        assertNull(cipher.encrypt(null));
        assertNull(cipher.decrypt(null));
    }

    @Test
    public void aPayloadTooShortForAnIvIsRejectedRatherThanThrowing() throws GeneralSecurityException
    {
        assertNull(cipher.decrypt(new byte[] { 1, 2, 3 }));
    }

    @Test
    public void tamperedCiphertextFailsTheAuthenticationTag() throws GeneralSecurityException
    {
        byte[] encrypted = cipher.encrypt("hunter2");
        encrypted[encrypted.length - 1] ^= 0x01;

        assertThrows(GeneralSecurityException.class, () -> cipher.decrypt(encrypted));
    }
}
