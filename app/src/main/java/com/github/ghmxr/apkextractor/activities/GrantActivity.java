package com.github.ghmxr.apkextractor.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;

public class GrantActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_grant);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle(getResources().getString(R.string.activity_settings_grant));

//        refreshPermissionStatus();

        findViewById(R.id.grant_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 30) {
                    showAttentionDialog(R.string.dialog_grant_attention_title, R.string.dialog_grant_no_need, new Runnable() {
                        @Override
                        public void run() {
                            jumpToGrantPage(0, Global.URI_DATA);
                            ToastManager.showToast(GrantActivity.this, getResources().getString(R.string.dialog_grant_toast_data), Toast.LENGTH_SHORT);
                        }
                    });
                    return;
                }
                showAttentionDialog(R.string.dialog_grant_attention_title, R.string.dialog_grant_attention_data, new Runnable() {
                    @Override
                    public void run() {
                        jumpToGrantPage(0, Global.URI_DATA);
                        ToastManager.showToast(GrantActivity.this, getResources().getString(R.string.dialog_grant_toast_data), Toast.LENGTH_SHORT);
                    }
                });
            }
        });

        findViewById(R.id.grant_obb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 30) {
                    showAttentionDialog(R.string.dialog_grant_attention_title, R.string.dialog_grant_no_need, new Runnable() {
                        @Override
                        public void run() {
                            jumpToGrantPage(1, Global.URI_OBB);
                            ToastManager.showToast(GrantActivity.this, getResources().getString(R.string.dialog_grant_toast_obb), Toast.LENGTH_SHORT);
                        }
                    });
                    return;
                }
                showAttentionDialog(R.string.dialog_grant_attention_title, R.string.dialog_grant_attention_obb, new Runnable() {
                    @Override
                    public void run() {
                        jumpToGrantPage(1, Global.URI_OBB);
                        ToastManager.showToast(GrantActivity.this, getResources().getString(R.string.dialog_grant_toast_obb), Toast.LENGTH_SHORT);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    private void refreshPermissionStatus() {
        ((TextView) findViewById(R.id.grant_data_value)).setText(getResources().getString(DocumentFileUtil.canWriteDataPathByDocumentFile() ? R.string.permission_granted : R.string.permission_denied));
        ((TextView) findViewById(R.id.grant_obb_value)).setText(getResources().getString(DocumentFileUtil.canWriteObbPathByDocumentFile() ? R.string.permission_granted : R.string.permission_denied));
    }

    private void showAttentionDialog(int titleResId, int contentMessageId, final Runnable action_confirm) {
        new AlertDialog.Builder(GrantActivity.this).setTitle(getResources().getString(titleResId))
                .setMessage(getResources().getString(contentMessageId))
                .setPositiveButton(getResources().getString(R.string.action_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (action_confirm != null) {
                            action_confirm.run();
                        }
                    }
                })
                .setNegativeButton(getResources().getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void jumpToGrantPage(int requestCode, String uri) {
        if (Build.VERSION.SDK_INT < 26) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                DocumentFile.fromTreeUri(GrantActivity.this,
                        Uri.parse(uri)).getUri());
        startActivityForResult(intent, requestCode);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {

        if (requestCode == 0 && RESULT_OK == resultCode && data != null && data.getData() != null) {
            String uriString = data.getData().getPath();
            uriString = String.valueOf(uriString).toLowerCase();
            if (uriString.endsWith("primary:android/data")) {
                takePersistPermission(data.getData());
            } else {
                showAttentionDialog(R.string.dialog_grant_attention_title, R.string.dialog_grant_warn, new Runnable() {
                    @Override
                    public void run() {
                        takePersistPermission(data.getData());
                    }
                });
            }

        } else if (requestCode == 1 && RESULT_OK == resultCode && data != null && data.getData() != null) {
            String uriString = data.getData().getPath();
            uriString = String.valueOf(uriString).toLowerCase();
            if (uriString.endsWith("primary:android/obb")) {
                takePersistPermission(data.getData());
            } else {
                showAttentionDialog(R.string.dialog_grant_attention_title, R.string.dialog_grant_warn, new Runnable() {
                    @Override
                    public void run() {
                        takePersistPermission(data.getData());
                    }
                });
            }
        }

        refreshPermissionStatus();
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void takePersistPermission(Uri uri) {
        if (Build.VERSION.SDK_INT >= 19) {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
