package com.github.ghmxr.apkextractor.tasks;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.SignatureView;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.util.HashMap;

public class GetSignatureInfoTask extends Thread {

    private static final HashMap<String, String[]> sign_infos_cache = new HashMap<>();
    private static final HashMap<PackageInfo, String> md5_cache = new HashMap<>();
    private static final HashMap<PackageInfo, String> sha1_cache = new HashMap<>();
    private static final HashMap<PackageInfo, String> sha256_cache = new HashMap<>();

    private final Activity activity;
    private final PackageInfo packageInfo;
    private final SignatureView signatureView;
    private final CompletedCallback callback;

    public GetSignatureInfoTask(@NonNull Activity activity, @NonNull PackageInfo packageInfo, @NonNull SignatureView signatureView
            , @NonNull CompletedCallback callback) {
        super();
        this.activity = activity;
        this.packageInfo = packageInfo;
        this.signatureView = signatureView;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        String[] sign_infos1;
        String md5_1;
        String sha1_1;
        String sha256_1;
        synchronized (sign_infos_cache) {
            if (sign_infos_cache.get(packageInfo.applicationInfo.sourceDir) != null) {
                sign_infos1 = sign_infos_cache.get(packageInfo.applicationInfo.sourceDir);
            } else {
                sign_infos1 = EnvironmentUtil.getAPKSignInfo(packageInfo.applicationInfo.sourceDir);
                sign_infos_cache.put(packageInfo.applicationInfo.sourceDir, sign_infos1);
            }
        }
        synchronized (md5_cache) {
            if (md5_cache.get(packageInfo) != null) {
                md5_1 = md5_cache.get(packageInfo);
            } else {
                md5_1 = EnvironmentUtil.getSignatureMD5StringOfPackageInfo(packageInfo);
                md5_cache.put(packageInfo, md5_1);
            }
        }
        synchronized (sha1_cache) {
            if (sha1_cache.get(packageInfo) != null) {
                sha1_1 = sha1_cache.get(packageInfo);
            } else {
                sha1_1 = EnvironmentUtil.getSignatureSHA1OfPackageInfo(packageInfo);
                sha1_cache.put(packageInfo, sha1_1);
            }
        }
        synchronized (sha256_cache) {
            if (sha256_cache.get(packageInfo) != null) {
                sha256_1 = sha256_cache.get(packageInfo);
            } else {
                sha256_1 = EnvironmentUtil.getSignatureSHA256OfPackageInfo(packageInfo);
                sha256_cache.put(packageInfo, sha256_1);
            }
        }
        final String[] sign_infos = sign_infos1;
        final String md5 = md5_1;
        final String sha1 = sha1_1;
        final String sha256 = sha256_1;

        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                signatureView.getTv_sub_value().setText(sign_infos[0]);
                signatureView.getTv_iss_value().setText(sign_infos[1]);
                signatureView.getTv_serial_value().setText(sign_infos[2]);
                signatureView.getTv_start().setText(sign_infos[3]);
                signatureView.getTv_end().setText(sign_infos[4]);
                signatureView.getTv_md5().setText(md5);
                signatureView.getTv_sha1().setText(sha1);
                signatureView.getTv_sha256().setText(sha256);
                signatureView.getLinearLayout_sub().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_sub_value().getText().toString());
                    }
                });
                signatureView.getLinearLayout_iss().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_iss_value().getText().toString());
                    }
                });
                signatureView.getLinearLayout_serial().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_serial_value().getText().toString());
                    }
                });
                signatureView.getLinearLayout_start().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_start().getText().toString());
                    }
                });
                signatureView.getLinearLayout_end().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_end().getText().toString());
                    }
                });
                signatureView.getLinearLayout_md5().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_md5().getText().toString());
                    }
                });
                signatureView.getLinearLayout_sha1().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_sha1().getText().toString());
                    }
                });
                signatureView.getLinearLayout_sha256().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(signatureView.getTv_sha256().getText().toString());
                    }
                });
                signatureView.getRoot().setVisibility(View.VISIBLE);
                callback.onCompleted();
            }
        });
    }

    private void clip2ClipboardAndShowSnackbar(String s) {
        try {
            ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("message", s));
            Snackbar.make(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.snack_bar_clipboard), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static synchronized void clearCache() {
        sign_infos_cache.clear();
        md5_cache.clear();
        sha1_cache.clear();
        sha256_cache.clear();
    }

    public interface CompletedCallback {
        void onCompleted();
    }
}
