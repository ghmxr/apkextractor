package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.widget.NestedScrollView;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.GetPackageInfoViewTask;
import com.github.ghmxr.apkextractor.tasks.GetSignatureInfoTask;
import com.github.ghmxr.apkextractor.tasks.HashTask;
import com.github.ghmxr.apkextractor.ui.AssemblyView;
import com.github.ghmxr.apkextractor.ui.LibraryView;
import com.github.ghmxr.apkextractor.ui.SignatureView;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PackageDetailActivity extends BaseActivity implements View.OnClickListener {

    private ImportItem importItem;
    public static final String EXTRA_IMPORT_ITEM_PATH = "import_item_path";
    private CheckBox cb_data, cb_obb, cb_apk;
    private ZipFileUtil.ZipFileInfo zipFileInfo;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = getIntent().getData();
            if (uri == null) {
                ToastManager.showToast(this, getResources().getString(R.string.activity_package_detail_blank), Toast.LENGTH_SHORT);
                finish();
                return;
            }
            importItem = new ImportItem(this, new FileItem(this, uri));
            if ("apk".equalsIgnoreCase(EnvironmentUtil.getFileExtensionName(importItem.getFileItem().getName()))) {
                ToastManager.showToast(this, getResources().getString(R.string.activity_package_detail_apk_attention), Toast.LENGTH_SHORT);
                finish();
                return;
            }
        } else {
            try {
                //importItem= Global.item_list.get(getIntent().getIntExtra(EXTRA_IMPORT_ITEM_POSITION,-1));
                synchronized (Global.item_list) {
                    importItem = Global.getImportItemByFileItemPath(Global.item_list, getIntent().getStringExtra(EXTRA_IMPORT_ITEM_PATH));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ToastManager.showToast(this, getResources().getString(R.string.activity_package_detail_blank), Toast.LENGTH_SHORT);
                finish();
            }
        }
        if (importItem == null) {
            ToastManager.showToast(this, getResources().getString(R.string.activity_package_detail_blank), Toast.LENGTH_SHORT);
            finish();
            return;
        }
        setContentView(R.layout.activity_package_detail);
        Toolbar toolbar = findViewById(R.id.toolbar_package_detail);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final NestedScrollView nestedScrollView = findViewById(R.id.nsv);
        final FloatingActionButton floatingActionButton = findViewById(R.id.toTop);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            int old_y;

            @Override
            public void onScrollChange(NestedScrollView nestedScrollView, int i, int i1, int i2, int i3) {
//                Log.e("111","i="+i+",i1="+i1+",i2="+i2+",i3="+i3);
                actionBar.setTitle(i1 > 0 ? String.valueOf(importItem.getItemName()) : "");
                if (i1 > old_y && old_y > 1500) {
                    floatingActionButton.show();
                } else {
                    floatingActionButton.hide();
                }
                old_y = i1;
            }
        });
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nestedScrollView.smoothScrollTo(0, 0);
            }
        });

        cb_data = findViewById(R.id.package_detail_data);
        cb_obb = findViewById(R.id.package_detail_obb);
        cb_apk = findViewById(R.id.package_detail_apk);
        ((ImageView) findViewById(R.id.package_icon)).setImageDrawable(importItem.getIconDrawable());
        ((TextView) findViewById(R.id.package_detail_name)).setText(importItem.getFileItem().getName());
        ((TextView) findViewById(R.id.package_detail_package_name)).setText(importItem.getPackageInfo() != null ? String.valueOf(importItem.getPackageInfo().packageName) : getResources().getString(R.string.word_unknown));
        ((TextView) findViewById(R.id.package_detail_version_name_title)).setText(importItem.getVersionName());
        ((TextView) findViewById(R.id.package_detail_file_name)).setText(importItem.getFileItem().getName());
        ((TextView) findViewById(R.id.package_detail_size)).setText(Formatter.formatFileSize(this, importItem.getSize()));
        ((TextView) findViewById(R.id.package_detail_version_name)).setText(importItem.getVersionName());
        ((TextView) findViewById(R.id.package_detail_version_code)).setText(importItem.getVersionCode());
        ((TextView) findViewById(R.id.package_detail_minimum_api)).setText(importItem.getMinSdkVersion());
        ((TextView) findViewById(R.id.package_detail_target_api)).setText(importItem.getTargetSdkVersion());
        ((TextView) findViewById(R.id.package_detail_last_modified)).setText(SimpleDateFormat.getDateTimeInstance().format(new Date(importItem.getLastModified())));
        ((TextView) findViewById(R.id.package_detail_path)).setText(importItem.getFileItem().getPath());
        if (importItem.getFileItem().isShareUriInstance()) {
            findViewById(R.id.package_detail_last_modified_area).setVisibility(View.GONE);
            //findViewById(R.id.package_detail_size_dividing).setVisibility(View.GONE);
            findViewById(R.id.package_detail_delete_area).setVisibility(View.GONE);
        }
        if (importItem.getImportType() == ImportItem.ImportType.ZIP) {
            findViewById(R.id.package_detail_version_name_title).setVisibility(View.GONE);
            findViewById(R.id.package_detail_package_name_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_version_name_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_version_code_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_minimum_api_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_target_api_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_target_api_dividing).setVisibility(View.GONE);
        }
        if (importItem.getImportType() == ImportItem.ImportType.APK && importItem.getPackageInfo() != null) {
            if (SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) {
                findViewById(R.id.package_detail_signature_head).setVisibility(View.VISIBLE);
                findViewById(R.id.package_detail_signature_pg).setVisibility(View.VISIBLE);
                findViewById(R.id.package_detail_card_pg).setVisibility(View.VISIBLE);

                new GetSignatureInfoTask(this, importItem.getPackageInfo(), (SignatureView) findViewById(R.id.package_detail_signature), new GetSignatureInfoTask.CompletedCallback() {
                    @Override
                    public void onCompleted() {
                        findViewById(R.id.package_detail_signature_pg).setVisibility(View.GONE);
                        findViewById(R.id.package_detail_signature).setVisibility(View.VISIBLE);
                    }
                }).start();
            }
            findViewById(R.id.package_detail_card_pg).setVisibility(View.VISIBLE);
            new GetPackageInfoViewTask(this, importItem.getPackageInfo(), (AssemblyView) findViewById(R.id.package_detail_assemble), new GetPackageInfoViewTask.CompletedCallback() {
                @Override
                public void onViewsCreated() {
                    findViewById(R.id.package_detail_card_pg).setVisibility(View.GONE);
                    findViewById(R.id.package_detail_assemble).setVisibility(View.VISIBLE);
                }
            }).start();

            if (SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_NATIVE_FILE, Constants.PREFERENCE_LOAD_NATIVE_FILE_DEFAULT)) {
                try {
                    LibraryView libraryView = (LibraryView) findViewById(R.id.libraryView);
                    libraryView.setLibrary(importItem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)) {
            findViewById(R.id.package_detail_hash_att).setVisibility(View.VISIBLE);
            findViewById(R.id.package_detail_hash).setVisibility(View.VISIBLE);
            new HashTask(importItem.getFileItem(), HashTask.HashType.MD5, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_md5_pg).setVisibility(View.GONE);
                    TextView tv_md5 = findViewById(R.id.detail_hash_md5_value);
                    tv_md5.setVisibility(View.VISIBLE);
                    tv_md5.setText(result);
                }
            }).start();
            new HashTask(importItem.getFileItem(), HashTask.HashType.SHA1, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha1_pg).setVisibility(View.GONE);
                    TextView tv_sha1 = findViewById(R.id.detail_hash_sha1_value);
                    tv_sha1.setVisibility(View.VISIBLE);
                    tv_sha1.setText(result);
                }
            }).start();
            new HashTask(importItem.getFileItem(), HashTask.HashType.SHA256, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha256_pg).setVisibility(View.GONE);
                    TextView tv_sha256 = findViewById(R.id.detail_hash_sha256_value);
                    tv_sha256.setVisibility(View.VISIBLE);
                    tv_sha256.setText(result);
                }
            }).start();
            new HashTask(importItem.getFileItem(), HashTask.HashType.CRC32, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_crc32_pg).setVisibility(View.GONE);
                    TextView tv_crc32 = findViewById(R.id.detail_hash_crc32_value);
                    tv_crc32.setVisibility(View.VISIBLE);
                    tv_crc32.setText(result);
                }
            }).start();
        }

        if (importItem.getImportType() == ImportItem.ImportType.ZIP) {
            findViewById(R.id.package_detail_data_obb_pg).setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long data = 0;
                        long obb = 0;
                        long apk = 0;
                        final ZipFileUtil.ZipFileInfo zipFileInfo = ZipFileUtil.getZipFileInfoOfImportItem(importItem);
                        if (zipFileInfo != null) {
                            data = zipFileInfo.getDataSize();
                            obb = zipFileInfo.getObbSize();
                            apk = zipFileInfo.getApkSize();
                        }
                        final long data_final = data;
                        final long obb_final = obb;
                        final long apk_final = apk;
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                PackageDetailActivity.this.zipFileInfo = zipFileInfo;
                                findViewById(R.id.package_detail_data_obb_pg).setVisibility(View.GONE);
                                findViewById(R.id.package_detail_checkboxes).setVisibility(View.VISIBLE);
                                cb_data.setEnabled(data_final > 0);
                                cb_obb.setEnabled(obb_final > 0);
                                cb_apk.setEnabled(apk_final > 0);
                                cb_data.setText("data:" + Formatter.formatFileSize(PackageDetailActivity.this, data_final));
                                cb_obb.setText("obb:" + Formatter.formatFileSize(PackageDetailActivity.this, obb_final));
                                cb_apk.setText("apk:" + Formatter.formatFileSize(PackageDetailActivity.this, apk_final));
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.package_detail_data_obb_pg).setVisibility(View.GONE);
                                ToastManager.showToast(PackageDetailActivity.this,
                                        "Occurs error while reading the file:" + e.toString(),
                                        Toast.LENGTH_SHORT);
                            }
                        });
                    }
                }
            }).start();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.package_detail_install_area: {
                if (importItem.getImportType() == ImportItem.ImportType.APK) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT <= 23) {
                            if (importItem.getFileItem().isFileInstance())
                                intent.setDataAndType(importItem.getUriFromFile(), "application/vnd.android.package-archive");
                            else
                                intent.setDataAndType(importItem.getUri(), "application/vnd.android.package-archive");
                        } else {
                            intent.setDataAndType(importItem.getUri(), "application/vnd.android.package-archive");
                        }
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ToastManager.showToast(this, e.toString(), Toast.LENGTH_SHORT);
                    }
                } else if (importItem.getImportType() == ImportItem.ImportType.ZIP) {
                    if (findViewById(R.id.package_detail_checkboxes).getVisibility() != View.VISIBLE) {
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.activity_detail_wait_loading), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (!cb_data.isChecked() && !cb_obb.isChecked() && !cb_apk.isChecked()) {
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.activity_detail_nothing_checked), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        ToastManager.showToast(this, getResources().getString(R.string.permission_write), Toast.LENGTH_SHORT);
                        return;
                    }
                    ImportItem importItem1 = new ImportItem(importItem, cb_data.isChecked(), cb_obb.isChecked(), cb_apk.isChecked());
                    final ArrayList<ImportItem> singleArray = new ArrayList<>();
                    singleArray.add(importItem1);
                    final ArrayList<ZipFileUtil.ZipFileInfo> zipFileInfos = new ArrayList<>();
                    zipFileInfos.add(zipFileInfo);
                    Global.showCheckingDuplicationDialogAndStartImporting(this, singleArray, zipFileInfos, new Global.ImportTaskFinishedCallback() {
                        @Override
                        public void onImportFinished(String error_message) {
                            if (!TextUtils.isEmpty(error_message)) {
                                new AlertDialog.Builder(PackageDetailActivity.this)
                                        .setTitle(getResources().getString(R.string.dialog_import_finished_error_title))
                                        .setMessage(getResources().getString(R.string.dialog_import_finished_error_message) + error_message)
                                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .show();
                            } else {
                                ToastManager.showToast(PackageDetailActivity.this, getResources().getString(R.string.toast_import_complete), Toast.LENGTH_SHORT);
                            }
                        }
                    });
                }
            }
            break;
            case R.id.package_detail_share_area: {
                final ArrayList<ImportItem> importItems = new ArrayList<>();
                importItems.add(importItem);
                Global.shareImportItems(this, importItems);
            }
            break;
            case R.id.package_detail_delete_area: {
                try {
                    new AlertDialog.Builder(this)
                            .setTitle(getResources().getString(R.string.dialog_import_delete_title))
                            .setMessage(getResources().getString(R.string.dialog_import_delete_message))
                            .setPositiveButton(getResources().getString(R.string.action_confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        if (importItem.getFileItem().delete()) {
                                            Intent intent = new Intent();
                                            intent.putExtra(EXTRA_IMPORT_ITEM_PATH, importItem.getFileItem().getPath());
                                            setResult(RESULT_OK, intent);
                                            finish();
                                        } else {
                                            ToastManager.showToast(PackageDetailActivity.this, "Delete failed", Toast.LENGTH_SHORT);
                                        }
//                                        sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case R.id.package_detail_file_name_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_file_name)).getText().toString());
            }
            break;
            case R.id.package_detail_version_name_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_version_name)).getText().toString());
            }
            break;
            case R.id.package_detail_version_code_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_version_code)).getText().toString());
            }
            break;
            case R.id.package_detail_size_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_size)).getText().toString());
            }
            break;
            case R.id.package_detail_minimum_api_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_minimum_api)).getText().toString());
            }
            break;
            case R.id.package_detail_target_api_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_target_api)).getText().toString());
            }
            break;
            case R.id.package_detail_last_modified_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_last_modified)).getText().toString());
            }
            break;
            case R.id.detail_hash_md5: {
                final String value = ((TextView) findViewById(R.id.detail_hash_md5_value)).getText().toString();
                if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
            }
            break;
            case R.id.detail_hash_sha1: {
                final String value = ((TextView) findViewById(R.id.detail_hash_sha1_value)).getText().toString();
                if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
            }
            break;
            case R.id.detail_hash_sha256: {
                final String value = ((TextView) findViewById(R.id.detail_hash_sha256_value)).getText().toString();
                if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
            }
            break;
            case R.id.detail_hash_crc32: {
                final String value = ((TextView) findViewById(R.id.detail_hash_crc32_value)).getText().toString();
                if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
            }
            break;
            case R.id.package_detail_path_area: {
                clip2ClipboardAndShowSnackbar(((TextView) findViewById(R.id.package_detail_path)).getText().toString());
            }
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
            case android.R.id.home: {
                checkHeightAndFinish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkHeightAndFinish() {
        if (Build.VERSION.SDK_INT >= 28) { //根布局项目太多时低版本Android会引发一个底层崩溃。版本号暂定28
            ActivityCompat.finishAfterTransition(this);
        } else {
            if (((AssemblyView) findViewById(R.id.package_detail_assemble)).getIsExpanded()) {
                finish();
            } else {
                ActivityCompat.finishAfterTransition(this);
            }
        }
    }

    private void clip2ClipboardAndShowSnackbar(String s) {
        try {
            ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("message", s));
            Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.snack_bar_clipboard), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
