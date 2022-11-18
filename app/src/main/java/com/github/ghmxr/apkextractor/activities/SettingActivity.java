package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.CompressExtensionDialog;
import com.github.ghmxr.apkextractor.ui.ExportRuleDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;

public class SettingActivity extends BaseActivity implements View.OnClickListener {

    private static final String ACTIVITY_RESULT = "result";
    private int result_code = RESULT_CANCELED;
    private SharedPreferences settings;

    private static final int REQUEST_CODE_SET_PATH = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        settings = SPUtil.getGlobalSharedPreferences(SettingActivity.this);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_settings));
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getSupportActionBar().setTitle(getResources().getString(R.string.action_settings));
        } catch (Exception e) {
            e.printStackTrace();
        }
        findViewById(R.id.settings_share_mode_area).setOnClickListener(this);
        findViewById(R.id.settings_night_mode_area).setOnClickListener(this);
        findViewById(R.id.settings_loading_options_area).setOnClickListener(this);
        findViewById(R.id.settings_rules_area).setOnClickListener(this);
        findViewById(R.id.settings_path_area).setOnClickListener(this);
        findViewById(R.id.settings_about_area).setOnClickListener(this);
        findViewById(R.id.settings_language_area).setOnClickListener(this);
        findViewById(R.id.settings_port_number_area).setOnClickListener(this);
        findViewById(R.id.settings_device_name_area).setOnClickListener(this);
        findViewById(R.id.settings_extension_area).setOnClickListener(this);
        findViewById(R.id.settings_package_scope_area).setOnClickListener(this);
        findViewById(R.id.settings_package_name_separator_area).setOnClickListener(this);
        findViewById(R.id.settings_package_exclude_area).setOnClickListener(this);
        View grantArea = findViewById(R.id.settings_grant_area);
        if (Build.VERSION.SDK_INT >= 26) {
            grantArea.setVisibility(View.VISIBLE);
        }
        grantArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingActivity.this, GrantActivity.class));
            }
        });
        refreshSettingValues();

        if (bundle != null) {
            setResult(bundle.getInt(ACTIVITY_RESULT));
        }
    }

    @Override
    public void onClick(View v) {
        if (settings == null) return;
        final SharedPreferences.Editor editor = settings.edit();
        switch (v.getId()) {
            default:
                break;
            case R.id.settings_share_mode_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_share_mode, null);
                int mode = settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT);
                ((RadioButton) dialogView.findViewById(R.id.share_mode_direct_ra)).setChecked(mode == Constants.SHARE_MODE_DIRECT);
                ((RadioButton) dialogView.findViewById(R.id.share_mode_after_extract_ra)).setChecked(mode == Constants.SHARE_MODE_AFTER_EXTRACT);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_share_mode))
                        .setView(dialogView)
                        .show();
                dialogView.findViewById(R.id.share_mode_direct).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_SHAREMODE, Constants.SHARE_MODE_DIRECT);
                        editor.apply();
                        refreshSettingValues();
                    }
                });
                dialogView.findViewById(R.id.share_mode_after_extract).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_SHAREMODE, Constants.SHARE_MODE_AFTER_EXTRACT);
                        editor.apply();
                        refreshSettingValues();
                    }
                });

            }
            break;
            case R.id.settings_night_mode_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_night_mode, null);
                int night_mode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT);
                ((RadioButton) dialogView.findViewById(R.id.night_mode_enabled_ra)).setChecked(night_mode == AppCompatDelegate.MODE_NIGHT_YES);
                ((RadioButton) dialogView.findViewById(R.id.night_mode_disabled_ra)).setChecked(night_mode == AppCompatDelegate.MODE_NIGHT_NO);
                ((RadioButton) dialogView.findViewById(R.id.night_mode_auto_ra)).setChecked(night_mode == AppCompatDelegate.MODE_NIGHT_AUTO);
                ((RadioButton) dialogView.findViewById(R.id.night_mode_follow_system_ra)).setChecked(night_mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_night_mode))
                        .setView(dialogView)
                        .show();
                dialogView.findViewById(R.id.night_mode_enabled).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });
                dialogView.findViewById(R.id.night_mode_disabled).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_NO);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });
                dialogView.findViewById(R.id.night_mode_auto).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_AUTO);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    }
                });
                dialogView.findViewById(R.id.night_mode_follow_system).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                });
            }
            break;
            case R.id.settings_loading_options_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading_selection, null);
                final CheckBox cb_permissions = dialogView.findViewById(R.id.loading_permissions);
                final CheckBox cb_activities = dialogView.findViewById(R.id.loading_activities);
                final CheckBox cb_receivers = dialogView.findViewById(R.id.loading_receivers);
                final CheckBox cb_static_loaders = dialogView.findViewById(R.id.loading_static_loaders);
                final CheckBox cb_signature = dialogView.findViewById(R.id.loading_apk_signature);
                final CheckBox cb_hash = dialogView.findViewById(R.id.loading_file_hash);
                final CheckBox cb_service = dialogView.findViewById(R.id.loading_services);
                final CheckBox cb_provider = dialogView.findViewById(R.id.loading_providers);
                final CheckBox cb_native = dialogView.findViewById(R.id.loading_native_libraries);
                cb_permissions.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT));
                cb_activities.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT));
                cb_receivers.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT));
                cb_static_loaders.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT));
                cb_signature.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT));
                cb_hash.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT));
                cb_service.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT));
                cb_provider.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT));
                cb_native.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_NATIVE_FILE, Constants.PREFERENCE_LOAD_NATIVE_FILE_DEFAULT));
                new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.activity_settings_loading_options))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editor.putBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, cb_permissions.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, cb_activities.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, cb_receivers.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, cb_static_loaders.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, cb_signature.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, cb_hash.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_SERVICES, cb_service.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, cb_provider.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_NATIVE_FILE, cb_native.isChecked());
                                editor.apply();
                                refreshSettingValues();
                                setResult(RESULT_OK);
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();

            }
            break;
            case R.id.settings_rules_area: {
                new ExportRuleDialog(this).show();
            }
            break;
            case R.id.settings_path_area: {
                /*if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    Global.showRequestingWritePermissionSnackBar(this);
                    return;
                }*/
                startActivityForResult(new Intent(this, FolderSelectorActivity.class), REQUEST_CODE_SET_PATH);
            }
            break;
            case R.id.settings_about_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);
                dialogView.findViewById(R.id.layout_about_donate).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://qr.alipay.com/FKX08041Y09ZGT6ZT91FA5")));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });
                new AlertDialog.Builder(this)
                        .setTitle(EnvironmentUtil.getAppName(this) + "(" + EnvironmentUtil.getAppVersionName(this) + ")")
                        .setIcon(R.drawable.icon_launcher)
                        .setCancelable(true)
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                            }
                        }).show();

            }
            break;
            case R.id.settings_language_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_language, null);
                int value = SPUtil.getGlobalSharedPreferences(this).getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT);
                ((RadioButton) dialogView.findViewById(R.id.language_follow_system_ra)).setChecked(value == Constants.LANGUAGE_FOLLOW_SYSTEM);
                ((RadioButton) dialogView.findViewById(R.id.language_chinese_ra)).setChecked(value == Constants.LANGUAGE_CHINESE);
                ((RadioButton) dialogView.findViewById(R.id.language_english_ra)).setChecked(value == Constants.LANGUAGE_ENGLISH);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_language))
                        .setView(dialogView)
                        .show();
                dialogView.findViewById(R.id.language_follow_system).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SPUtil.getGlobalSharedPreferences(SettingActivity.this).edit()
                                .putInt(Constants.PREFERENCE_LANGUAGE, Constants.LANGUAGE_FOLLOW_SYSTEM)
                                .apply();
                        dialog.cancel();
                        refreshLanguageValue();
                    }
                });
                dialogView.findViewById(R.id.language_chinese).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SPUtil.getGlobalSharedPreferences(SettingActivity.this).edit()
                                .putInt(Constants.PREFERENCE_LANGUAGE, Constants.LANGUAGE_CHINESE)
                                .apply();
                        dialog.cancel();
                        refreshLanguageValue();
                    }
                });
                dialogView.findViewById(R.id.language_english).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SPUtil.getGlobalSharedPreferences(SettingActivity.this).edit()
                                .putInt(Constants.PREFERENCE_LANGUAGE, Constants.LANGUAGE_ENGLISH)
                                .apply();
                        dialog.cancel();
                        refreshLanguageValue();
                    }
                });
            }
            break;
            case R.id.settings_port_number_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_port_number, null);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_port_number))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), null)
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .create();
                final EditText editText = dialogView.findViewById(R.id.dialog_port_edit);
                editText.setText(String.valueOf(SPUtil.getPortNumber(this)));
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String value = editText.getText().toString().trim();
                        try {
                            if (TextUtils.isEmpty(value)) {
                                ToastManager.showToast(SettingActivity.this, getResources().getString(R.string.activity_settings_port_unknown), Toast.LENGTH_SHORT);
                                return;
                            }
                            final int port = Integer.parseInt(value);
                            if (port < 1024 || port > 65535) {
                                ToastManager.showToast(SettingActivity.this, getResources().getString(R.string.activity_settings_port_invalid), Toast.LENGTH_SHORT);
                                return;
                            }
                            editor.putInt(Constants.PREFERENCE_NET_PORT, port);
                            editor.apply();
                            dialog.cancel();
                            refreshSettingValues();
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastManager.showToast(SettingActivity.this, e.toString(), Toast.LENGTH_SHORT);
                            return;
                        }
                    }
                });
            }
            break;
            case R.id.settings_device_name_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_device_name, null);
                final EditText editText = dialogView.findViewById(R.id.dialog_device_name_edit);
                editText.setText(SPUtil.getDeviceName(SettingActivity.this));
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_device_name))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), null)
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String value = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(value)) {
                            ToastManager.showToast(SettingActivity.this, getResources().getString(R.string.dialog_settings_device_name_invalid), Toast.LENGTH_SHORT);
                            return;
                        }
                        editor.putString(Constants.PREFERENCE_DEVICE_NAME, value);
                        editor.apply();
                        dialog.cancel();
                        refreshSettingValues();
                    }
                });
            }
            break;
            case R.id.settings_extension_area: {
                new CompressExtensionDialog(this, SPUtil.getCompressingExtensionName(this),
                        new CompressExtensionDialog.OnConfirmedCallback() {
                            @Override
                            public void onExtensionConfirmed(@NonNull String extension) {
                                editor.putString(Constants.PREFERENCE_COMPRESSING_EXTENSION, extension).apply();
                                refreshSettingValues();
                            }
                        }).show();
            }
            break;
            case R.id.settings_package_scope_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_package_scope, null);
                int value = settings.getInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT);
                ((RadioButton) dialogView.findViewById(R.id.package_scope_all_ra)).setChecked(value == Constants.PACKAGE_SCOPE_ALL);
                ((RadioButton) dialogView.findViewById(R.id.package_scope_exporting_path_ra)).setChecked(value == Constants.PACKAGE_SCOPE_EXPORTING_PATH);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_package_scope))
                        .setView(dialogView)
                        .show();
                dialogView.findViewById(R.id.package_scope_all).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editor.putInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PACKAGE_SCOPE_ALL);
                        editor.apply();
                        dialog.cancel();
                        refreshSettingValues();
                        sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                        ActivityCompat.requestPermissions(SettingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        Global.showRequestingWritePermissionSnackBar(SettingActivity.this);
                    }
                });
                dialogView.findViewById(R.id.package_scope_exporting_path).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editor.putInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PACKAGE_SCOPE_EXPORTING_PATH);
                        editor.apply();
                        dialog.cancel();
                        refreshSettingValues();
                        sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                    }
                });
            }
            break;
            case R.id.settings_package_name_separator_area: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_package_name_split, null);
                final EditText editText = dialogView.findViewById(R.id.dialog_package_name_split_edit);
                editText.setText(settings.getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT));
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_package_name_separator))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.action_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editor.putString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, editText.getText().toString()).apply();
                                refreshSettingValues();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
            break;
            case R.id.settings_package_exclude_area: {
                CheckBox cb = findViewById(R.id.filter_out_package);
                cb.toggle();
                settings.edit().putBoolean(Constants.PREFERENCE_EXCLUDE_INVALID_PACKAGE, cb.isChecked()).apply();
                sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
            }
            break;
        }
    }

    private void refreshNightMode(int value) {
        result_code = RESULT_OK;
        AppCompatDelegate.setDefaultNightMode(value);
        recreate();
    }

    private void refreshLanguageValue() {
        result_code = RESULT_OK;
        setAndRefreshLanguage();
        recreate();
    }

    private void refreshSettingValues() {
        if (settings == null) return;
        //SharedPreferences.Editor editor=settings.edit();
        ((TextView) findViewById(R.id.settings_path_value)).setText(SPUtil.getDisplayingExportPath(this));
        ((TextView) findViewById(R.id.settings_share_mode_value)).setText(
                getResources().getString(
                        settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT) == Constants.SHARE_MODE_DIRECT ?
                                R.string.share_mode_direct : R.string.share_mode_export));
        String night_mode_value = "";
        switch (settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT)) {
            default:
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                night_mode_value = getResources().getString(R.string.night_mode_enabled);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                night_mode_value = getResources().getString(R.string.night_mode_disabled);
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
                night_mode_value = getResources().getString(R.string.night_mode_auto);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                night_mode_value = getResources().getString(R.string.night_mode_follow_system);
                break;
        }
        ((TextView) findViewById(R.id.settings_night_mode_value)).setText(night_mode_value);
        String read_options = "";
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)) {
            read_options += getResources().getString(R.string.activity_detail_permissions);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.activity_detail_activities);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.activity_detail_services);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.activity_detail_receivers);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.activity_detail_providers);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_NATIVE_FILE, Constants.PREFERENCE_LOAD_NATIVE_FILE_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.activity_detail_native_library);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.activity_detail_static_loaders);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.dialog_loading_selection_signature);
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)) {
            if (!read_options.equals("")) read_options += ",";
            read_options += getResources().getString(R.string.dialog_loading_selection_file_hash);
        }
        if (read_options.trim().equals(""))
            read_options = getResources().getString(R.string.word_blank);
        ((TextView) findViewById(R.id.settings_loading_options_value)).setText(read_options);
        String language_value = "";
        switch (SPUtil.getGlobalSharedPreferences(this).getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT)) {
            default:
                break;
            case Constants.LANGUAGE_FOLLOW_SYSTEM:
                language_value = getResources().getString(R.string.language_follow_system);
                break;
            case Constants.LANGUAGE_CHINESE:
                language_value = getResources().getString(R.string.language_chinese);
                break;
            case Constants.LANGUAGE_ENGLISH:
                language_value = getResources().getString(R.string.language_english);
                break;
        }
        ((TextView) findViewById(R.id.settings_language_value)).setText(language_value);
        ((TextView) findViewById(R.id.settings_port_number_value)).setText(String.valueOf(SPUtil.getPortNumber(this)));
        ((TextView) findViewById(R.id.settings_device_name_value)).setText(SPUtil.getDeviceName(this));
        ((TextView) findViewById(R.id.settings_extension_value)).setText(SPUtil.getCompressingExtensionName(this));
        final int value_package_scope = settings.getInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT);
        TextView tv_package_scope = findViewById(R.id.settings_package_scope_value);
        switch (value_package_scope) {
            default:
                break;
            case Constants.PACKAGE_SCOPE_ALL: {
                tv_package_scope.setText(getResources().getString(R.string.package_scope_all));
            }
            break;
            case Constants.PACKAGE_SCOPE_EXPORTING_PATH: {
                tv_package_scope.setText(getResources().getString(R.string.package_scope_exporting_path));
            }
            break;
        }
        ((TextView) findViewById(R.id.settings_package_name_separator_value)).setText(settings.getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT));
        ((CheckBox) findViewById(R.id.filter_out_package)).setChecked(settings.getBoolean(Constants.PREFERENCE_EXCLUDE_INVALID_PACKAGE, Constants.PREFERENCE_EXCLUDE_INVALID_PACKAGE_DEFAULT));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ACTIVITY_RESULT, result_code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SET_PATH && resultCode == RESULT_OK) {
            refreshSettingValues();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
