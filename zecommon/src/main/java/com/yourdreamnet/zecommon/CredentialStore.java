package com.yourdreamnet.zecommon;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import androidx.annotation.RequiresApi;

import static android.content.Context.MODE_PRIVATE;

public class CredentialStore {

    private static final String PREFERENCE_FILE = "authentication";
    private static final String EMAIL_KEY = "email";
    private static final String EMAIL_IV_KEY = "email_iv";
    private static final String PASSWORD_KEY = "password";
    private static final String PASSWORD_IV_KEY = "password_iv";
    private static final String LOGIN_KEY = "loginKey";

    private Context mContext;

    public static class Credentials {

        private String mEmail;
        private String mPassword;

        private Credentials(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        public String email() {
            return mEmail;
        }

        public String password() {
            return mPassword;
        }

    }

    public CredentialStore(Context context) {
        mContext = context;
    }

    public Credentials loadLoginInsecure() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE);
        String email = sharedPref.getString(EMAIL_KEY, null);
        String password = sharedPref.getString(PASSWORD_KEY, null);
        return new Credentials(email, password);
    }

    public Credentials loadLoginSecure() {
        try {
            SharedPreferences sharedPref = mContext.getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE);
            byte[] emailIv = Base64.decode(sharedPref.getString(EMAIL_IV_KEY, null), Base64.DEFAULT);
            byte[] emailEncrypted = Base64.decode(sharedPref.getString(EMAIL_KEY, null), Base64.DEFAULT);
            byte[] passwordIv = Base64.decode(sharedPref.getString(PASSWORD_IV_KEY, null), Base64.DEFAULT);
            byte[] passwordEncrypted = Base64.decode(sharedPref.getString(PASSWORD_KEY, null), Base64.DEFAULT);

            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                        .getEntry(LOGIN_KEY, null);
                if (secretKeyEntry != null) {
                    final SecretKey secretKey = secretKeyEntry.getSecretKey();
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec spec = new GCMParameterSpec(128, emailIv);
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                    final String email = new String(cipher.doFinal(emailEncrypted), "UTF-8");
                    cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    spec = new GCMParameterSpec(128, passwordIv);
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                    final String password = new String(cipher.doFinal(passwordEncrypted), "UTF-8");
                    return new Credentials(email, password);
                }
            } catch (BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException | KeyStoreException | UnrecoverableEntryException | CertificateException | IOException | NoSuchAlgorithmException e) {
                Log.e("LoginActivity", "Unable to load or save the login details", e);
            }
        } catch (NullPointerException e) {
            // At least one of the fields doesn't exist, skip
        }
        return new Credentials("", "");
    }

    public void saveLoginInsecure(String email, String password) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE);
        sharedPref.edit().
                putString(EMAIL_KEY, email).
                putString(PASSWORD_KEY, password).
                apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean saveLoginSecure(String email, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                    .getEntry(LOGIN_KEY, null);
            final SecretKey secretKey;
            if (secretKeyEntry == null) {
                final KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
                );
                final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                        LOGIN_KEY,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build();
                keyGenerator.init(keyGenParameterSpec);
                secretKey = keyGenerator.generateKey();
            } else {
                secretKey = secretKeyEntry.getSecretKey();
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] emailIv = cipher.getIV();
            byte[] emailEncrypted = cipher.doFinal(email.getBytes("UTF-8"));

            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] passwordIv = cipher.getIV();
            byte[] passwordEncrypted = cipher.doFinal(password.getBytes("UTF-8"));

            SharedPreferences sharedPref = mContext.getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE);
            sharedPref.edit().
                    putString(EMAIL_IV_KEY, Base64.encodeToString(emailIv, Base64.DEFAULT)).
                    putString(EMAIL_KEY, Base64.encodeToString(emailEncrypted, Base64.DEFAULT)).
                    putString(PASSWORD_IV_KEY, Base64.encodeToString(passwordIv, Base64.DEFAULT)).
                    putString(PASSWORD_KEY, Base64.encodeToString(passwordEncrypted, Base64.DEFAULT)).
                    apply();
            return true;
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeyException | NoSuchProviderException | InvalidAlgorithmParameterException | KeyStoreException | UnrecoverableEntryException | CertificateException | IOException | NoSuchAlgorithmException e) {
            Log.e("LoginActivity", "Unable to save the login details", e);
        }
        return false;
    }

    public void clear() {
        mContext.getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE).edit().clear().apply();
    }

}
