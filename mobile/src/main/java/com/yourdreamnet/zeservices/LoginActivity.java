package com.yourdreamnet.zeservices;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.yourdreamnet.zecommon.CredentialStore;
import com.yourdreamnet.zecommon.api.AuthenticatedApi;
import com.yourdreamnet.zecommon.api.QueueSingleton;
import com.yourdreamnet.zecommon.api.ZEServicesApi;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity {

    // UI references
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Switch mSaveSwitch;

    private CredentialStore mStore;
    private AuthenticatedApi mCachedApi;
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

        Button emailSignInButton = findViewById(R.id.email_sign_in_button);
        emailSignInButton.setOnClickListener(view -> attemptLogin());

        Button registerButton = findViewById(R.id.register);
        registerButton.setOnClickListener(view -> goToRegistration());

        mSaveSwitch = findViewById(R.id.save_credentials);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mSaveSwitch.setText(R.string.save_credentials_insecure);
            mSaveSwitch.setChecked(false);
        } else {
            mSaveSwitch.setChecked(true);
        }

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mIsPaused = false;
        mCachedApi = null;

        mStore = new CredentialStore(this);
    }

    private void goToRegistration() {
        Intent browserIntent = new Intent(
            Intent.ACTION_VIEW, Uri.parse("https://www.services.renault-ze.com/user/registration")
        );
        startActivity(browserIntent);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getIntent().getBooleanExtra("logout", false)) {
            // Delete the shared preferences and cancel any cached login
            mCachedApi = null;
            clearWearable();
            mStore.clear();
        } else {
            CredentialStore.Credentials credentials;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                credentials = mStore.loadLoginSecure();
            } else {
                credentials = mStore.loadLoginInsecure();
            }
            if (!credentials.email().isEmpty() && !credentials.password().isEmpty()) {
                login(credentials.email(), credentials.password(), false);
            }
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
            login(email, password, mSaveSwitch.isChecked());
        }
    }

    private void loginComplete(AuthenticatedApi api) {
        runOnUiThread(() -> {
            showProgress(false);
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.putExtra("api", api);
            startIntent.setFlags(startIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(startIntent);
        });
    }

    private void saveToWearable(String email, String password) {
        DataClient client = Wearable.getDataClient(this);
        PutDataMapRequest request = PutDataMapRequest.create("/zeservices/credentials");
        DataMap dataMap = request.getDataMap();
        dataMap.putString("email", email);
        dataMap.putString("password", password);
        client.putDataItem(request.asPutDataRequest());
    }

    private void clearWearable() {
        Uri uri = new Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .path("/zeservices/credentials")
                .authority("*")
                .build();
        Wearable.getDataClient(this).deleteDataItems(uri);
    }

    private void login(String email, String password, boolean save) {
        showProgress(true);
        new ZEServicesApi(email, password).
            getAuthenticated(QueueSingleton.getQueue()).
            subscribe(
                api -> {
                    if (save) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mStore.saveLoginSecure(email, password);
                        } else {
                            mStore.saveLoginInsecure(email, password);
                        }
                    }
                    saveToWearable(email, password);
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
