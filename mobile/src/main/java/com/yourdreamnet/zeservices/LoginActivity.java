package com.yourdreamnet.zeservices;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

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

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREFERENCE_FILE = "authentication";
    private static final String EMAIL_KEY = "email";
    private static final String EMAIL_IV_KEY = "email_iv";
    private static final String PASSWORD_KEY = "password";
    private static final String PASSWORD_IV_KEY = "password_iv";
    private static final String LOGIN_KEY = "loginKey";

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private ZEServicesAPI.AuthenticatedAPI mCachedApi;
    private boolean mIsPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = findViewById(R.id.email);

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(view -> attemptLogin());

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mIsPaused = false;
        mCachedApi = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            loadLogin();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsPaused = false;

        if (mCachedApi != null) {
            loginComplete(mCachedApi);
            mCachedApi = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void loadLogin() {

        try {
            SharedPreferences sharedPref = getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE);
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
                    mEmailView.setText(email);
                    mPasswordView.setText(password);
                    if (!email.isEmpty() && !password.isEmpty()) {
                        login(email, password);
                    }
                }
            } catch (BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException | KeyStoreException | UnrecoverableEntryException | CertificateException | IOException | NoSuchAlgorithmException e) {
                Log.e("LoginActivity", "Unable to load or save the login details", e);
            }
        } catch (NullPointerException e) {
            // At least one of the fields doesn't exist, skip
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void saveLogin(String email, String password) {
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

            SharedPreferences sharedPref = getSharedPreferences(PREFERENCE_FILE, MODE_PRIVATE);
            sharedPref.edit().
                    putString(EMAIL_IV_KEY, Base64.encodeToString(emailIv, Base64.DEFAULT)).
                    putString(EMAIL_KEY, Base64.encodeToString(emailEncrypted, Base64.DEFAULT)).
                    putString(PASSWORD_IV_KEY, Base64.encodeToString(passwordIv, Base64.DEFAULT)).
                    putString(PASSWORD_KEY, Base64.encodeToString(passwordEncrypted, Base64.DEFAULT)).
                    apply();
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeyException | NoSuchProviderException | InvalidAlgorithmParameterException | KeyStoreException | UnrecoverableEntryException | CertificateException | IOException | NoSuchAlgorithmException e) {
            Log.e("LoginActivity", "Unable to save the login details", e);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            login(email, password);
        }
    }

    private void loginComplete(ZEServicesAPI.AuthenticatedAPI api) {
        runOnUiThread(() -> {
            showProgress(false);
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.putExtra("api", api);
            startIntent.setFlags(startIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(startIntent);
        });
    }

    private void login(String email, String password) {
        showProgress(true);
        new ZEServicesAPI(email, password).
            getAuthenticated(QueueSingleton.getQueue()).
            subscribe(
                api -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        saveLogin(email, password);
                    }
                    if (mIsPaused) {
                        mCachedApi = api;
                    } else {
                        loginComplete(api);
                    }
                },
                error -> runOnUiThread(() -> {
                    Log.e("LoginActivity", "Unable to authenticate", error);
                    showProgress(false);
                    mEmailView.setError(getString(R.string.error_invalid_email));
                    mEmailView.requestFocus();
                })
            );
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

}

