package com.rnfingerprint;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import javax.crypto.Cipher;

public class FingerprintAuthModule extends ReactContextBaseJavaModule {
    private KeyguardManager keyguardManager;
    private Callback mErrorCb;
    private Callback mSuccessCb;
    private static final short BIOMETRIC_AUTHENTICATION_REQUEST = 31653;

    public FingerprintAuthModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode != BIOMETRIC_AUTHENTICATION_REQUEST ||
                    mErrorCb == null || mSuccessCb == null) {
                return;
            }

            if (resultCode == Activity.RESULT_CANCELED) {
                mErrorCb.invoke(FingerprintAuthConstants.AUTHENTICATION_CANCELED, "Cancelled");
            } else if (resultCode == Activity.RESULT_OK) {
                int code = intent.getIntExtra("code", FingerprintAuthConstants.AUTHENTICATION_FAILED);
                if (code != FingerprintAuthConstants.AUTHENTICATION_SUCCESS) {
                    mErrorCb.invoke("Failed", code);
                } else {
                    mSuccessCb.invoke("Success");
                }
            }
        }
    };

    private KeyguardManager getKeyguardManager() {
        if (keyguardManager != null) {
            return keyguardManager;
        }
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return null;
        }

        keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);

        return keyguardManager;
    }

    @Override
    public String getName() {
        return "FingerprintAuth";
    }

    @ReactMethod
    public void isSupported(final Callback errorCallback, final Callback successCallback) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }

        int result = isFingerprintAuthAvailable();
        if (result == FingerprintAuthConstants.IS_SUPPORTED) {
            successCallback.invoke("Is supported.");
        } else {
            errorCallback.invoke("Not supported.", result);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @ReactMethod
    public void authenticate(
            final String reason,
            final ReadableMap config,
            final Callback errorCallback,
            final Callback successCallback
    ) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }

        int availableResult = isFingerprintAuthAvailable();
        if (availableResult != FingerprintAuthConstants.IS_SUPPORTED) {
            errorCallback.invoke("Not supported", availableResult);
            return;
        }

        /* FINGERPRINT ACTIVITY RELATED STUFF */
        final Cipher cipher = new FingerprintCipher().getCipher();
        if (cipher == null) {
            errorCallback.invoke("Not supported", FingerprintAuthConstants.NOT_AVAILABLE);
            return;
        }

        mErrorCb = errorCallback;
        mSuccessCb = successCallback;
        ReactApplicationContext context = getReactApplicationContext();
        Intent intent = new Intent(activity, BiometricActivity.class);

        intent.putExtra("reason", reason);
        intent.putExtra("cancelText", config.getString("cancelText"));
        intent.putExtra("title", config.getString("title"));

        activity.startActivityForResult(intent, BIOMETRIC_AUTHENTICATION_REQUEST);
    }

    private int isFingerprintAuthAvailable() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return FingerprintAuthConstants.NOT_SUPPORTED;
        }

        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return FingerprintAuthConstants.NOT_AVAILABLE; // we can't do the check
        }

        final KeyguardManager keyguardManager = getKeyguardManager();

        // We should call it only when we absolutely sure that API >= 23.
        // Otherwise we will get the crash on older versions.
        // TODO: migrate to FingerprintManagerCompat
        final FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);

        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected()) {
            return FingerprintAuthConstants.NOT_PRESENT;
        }

        if (keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            return FingerprintAuthConstants.NOT_AVAILABLE;
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            return FingerprintAuthConstants.NOT_ENROLLED;
        }
        return FingerprintAuthConstants.IS_SUPPORTED;
    }
}
