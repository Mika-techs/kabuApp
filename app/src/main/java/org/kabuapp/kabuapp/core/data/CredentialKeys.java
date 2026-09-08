package org.kabuapp.kabuapp.core.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Where {@link CredentialCipher} gets its key. Separated so the cipher's framing can be tested
 * with an ordinary AES key - the Android Keystore provider does not exist off-device.
 */
public interface CredentialKeys
{
    SecretKey key() throws GeneralSecurityException;

    /**
     * A non-exportable AES key held by the Android Keystore, hardware-backed where available.
     * Because the key cannot leave the device, a copy of the database taken elsewhere is not
     * decryptable.
     */
    class Keystore implements CredentialKeys
    {
        private static final String KEYSTORE = "AndroidKeyStore";
        private static final String KEY_ALIAS = "kabuapp-credentials";

        @Override
        public synchronized SecretKey key() throws GeneralSecurityException
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
}
