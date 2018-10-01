package com.rnfingerprint;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.concurrent.Executor;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import com.rnfingerprint.biometrics.BiometricPrompt;

public class BiometricActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setDescription(intent.getStringExtra("reason"))
                .setNegativeButtonText(intent.getStringExtra("cancelText"))
                .setTitle(intent.getStringExtra("title"))
                .build();

        BiometricExecutor executor = new BiometricExecutor(this);
        BiometricPrompt.AuthenticationCallback authenticationCallback = getAuthenticationCallback();

        new BiometricPrompt(this, executor, authenticationCallback).authenticate(info);
    }

    private BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        final Intent resultIntent = new Intent();

        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                resultIntent.putExtra("code", errorCode);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
            
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                resultIntent.putExtra("code", FingerprintAuthConstants.AUTHENTICATION_SUCCESS);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        };
    }

    private class BiometricExecutor implements Executor {
        private Activity mActivity;

        BiometricExecutor(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void execute(Runnable command) {
            mActivity.runOnUiThread(command);
        }
    }
}
