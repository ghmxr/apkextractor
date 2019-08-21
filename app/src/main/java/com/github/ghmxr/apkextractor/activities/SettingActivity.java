package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.Constants;
import com.github.ghmxr.apkextractor.ui.ExportRuleDialog;

public class SettingActivity extends BaseActivity implements View.OnClickListener{

    private static final String ACTIVITY_RESULT="result";
    private int result_code=RESULT_CANCELED;
    private SharedPreferences settings;

    private static final int REQUEST_CODE_SET_PATH=0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        settings=Global.getGlobalSharedPreferences(SettingActivity.this);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_settings));
        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception e){e.printStackTrace();}
        findViewById(R.id.settings_share_mode_area).setOnClickListener(this);
        findViewById(R.id.settings_night_mode_area).setOnClickListener(this);
        findViewById(R.id.settings_loading_options_area).setOnClickListener(this);
        findViewById(R.id.settings_rules_area).setOnClickListener(this);
        findViewById(R.id.settings_path_area).setOnClickListener(this);
        findViewById(R.id.settings_about_area).setOnClickListener(this);

        refreshSettingValues();

        if(bundle!=null){
            setResult(bundle.getInt(ACTIVITY_RESULT));
        }
    }

    @Override
    public void onClick(View v){
        if(settings==null)return;
        final SharedPreferences.Editor editor=settings.edit();
        switch (v.getId()){
            default:break;
            case R.id.settings_share_mode_area:{
                View dialogView= LayoutInflater.from(this).inflate(R.layout.dialog_share_mode,null);
                int mode=settings.getInt(Constants.PREFERENCE_SHAREMODE,Constants.PREFERENCE_SHAREMODE_DEFAULT);
                ((RadioButton)dialogView.findViewById(R.id.share_mode_direct_ra)).setChecked(mode==Constants.SHARE_MODE_DIRECT);
                ((RadioButton)dialogView.findViewById(R.id.share_mode_after_extract_ra)).setChecked(mode==Constants.SHARE_MODE_AFTER_EXTRACT);
                final AlertDialog dialog=new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_share_mode))
                        .setView(dialogView)
                        .show();
                dialogView.findViewById(R.id.share_mode_direct).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_SHAREMODE,Constants.SHARE_MODE_DIRECT);
                        editor.apply();
                        refreshSettingValues();
                    }
                });
                dialogView.findViewById(R.id.share_mode_after_extract).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_SHAREMODE,Constants.SHARE_MODE_AFTER_EXTRACT);
                        editor.apply();
                        refreshSettingValues();
                    }
                });

            }
            break;
            case R.id.settings_night_mode_area:{
                View dialogView=LayoutInflater.from(this).inflate(R.layout.dialog_night_mode,null);
                int night_mode=settings.getInt(Constants.PREFERENCE_NIGHT_MODE,Constants.PREFERENCE_NIGHT_MODE_DEFAULT);
                ((RadioButton)dialogView.findViewById(R.id.night_mode_enabled_ra)).setChecked(night_mode== AppCompatDelegate.MODE_NIGHT_YES);
                ((RadioButton)dialogView.findViewById(R.id.night_mode_disabled_ra)).setChecked(night_mode==AppCompatDelegate.MODE_NIGHT_NO);
                ((RadioButton)dialogView.findViewById(R.id.night_mode_auto_ra)).setChecked(night_mode==AppCompatDelegate.MODE_NIGHT_AUTO);
                ((RadioButton)dialogView.findViewById(R.id.night_mode_follow_system_ra)).setChecked(night_mode==AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                final AlertDialog dialog=new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_settings_night_mode))
                        .setView(dialogView)
                        .show();
                dialogView.findViewById(R.id.night_mode_enabled).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_YES);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });
                dialogView.findViewById(R.id.night_mode_disabled).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_NO);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });
                dialogView.findViewById(R.id.night_mode_auto).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_AUTO);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    }
                });
                dialogView.findViewById(R.id.night_mode_follow_system).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        editor.putInt(Constants.PREFERENCE_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        editor.apply();
                        refreshNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                });
            }
            break;
            case R.id.settings_loading_options_area:{
                View dialogView=LayoutInflater.from(this).inflate(R.layout.dialog_loading_selection,null);
                final CheckBox cb_permissions=dialogView.findViewById(R.id.loading_permissions);
                final CheckBox cb_activities=dialogView.findViewById(R.id.loading_activities);
                final CheckBox cb_receivers=dialogView.findViewById(R.id.loading_receivers);
                final CheckBox cb_static_loaders=dialogView.findViewById(R.id.loading_static_loaders);
                cb_permissions.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT));
                cb_activities.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT));
                cb_receivers.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT));
                cb_static_loaders.setChecked(settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS,Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT));
                new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.activity_settings_loading_options))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editor.putBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,cb_permissions.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,cb_activities.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,cb_receivers.isChecked());
                                editor.putBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS,cb_static_loaders.isChecked());
                                editor.apply();
                                refreshSettingValues();
                                setResult(RESULT_OK);
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {}
                        })
                        .show();

            }
            break;
            case R.id.settings_rules_area:{
                new ExportRuleDialog(this).show();
            }
            break;
            case R.id.settings_path_area:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    Global.showRequestingWritePermissionSnackBar(this);
                    return;
                }
                startActivityForResult(new Intent(this,FolderSelectorActivity.class),REQUEST_CODE_SET_PATH);
            }
            break;
            case R.id.settings_about_area:{
                View dialogView=LayoutInflater.from(this).inflate(R.layout.dialog_about, null);
                dialogView.findViewById(R.id.layout_about_donate).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        try{
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://qr.alipay.com/FKX08041Y09ZGT6ZT91FA5")));
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                });
                new AlertDialog.Builder(this)
                        .setTitle(this.getResources().getString(R.string.dialog_about_title))
                        .setIcon(R.drawable.icon_launcher)
                        .setCancelable(true)
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface arg0, int arg1) {}
                        }).show();

            }
            break;
        }
    }

    private void refreshNightMode(int value){
        result_code=RESULT_OK;
        AppCompatDelegate.setDefaultNightMode(value);
        recreate();
    }

    private void refreshSettingValues(){
        if(settings==null)return;
        //SharedPreferences.Editor editor=settings.edit();
        ((TextView)findViewById(R.id.settings_path_value)).setText(settings.getString(Constants.PREFERENCE_SAVE_PATH,Constants.PREFERENCE_SAVE_PATH_DEFAULT));
        ((TextView)findViewById(R.id.settings_share_mode_value)).setText(
                getResources().getString(
                        settings.getInt(Constants.PREFERENCE_SHAREMODE,Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_DIRECT?
                        R.string.share_mode_direct:R.string.share_mode_export));
        String night_mode_value="";
        switch (settings.getInt(Constants.PREFERENCE_NIGHT_MODE,Constants.PREFERENCE_NIGHT_MODE_DEFAULT)){
            default:break;
            case AppCompatDelegate.MODE_NIGHT_YES:night_mode_value=getResources().getString(R.string.night_mode_enabled);break;
            case AppCompatDelegate.MODE_NIGHT_NO:night_mode_value=getResources().getString(R.string.night_mode_disabled);break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:night_mode_value=getResources().getString(R.string.night_mode_auto);break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:night_mode_value=getResources().getString(R.string.night_mode_follow_system);break;
        }
        ((TextView)findViewById(R.id.settings_night_mode_value)).setText(night_mode_value);
        String read_options="";
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)){
            read_options+=getResources().getString(R.string.activity_detail_permissions);
        }
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)){
            if(!read_options.equals(""))read_options+=",";
            read_options+=getResources().getString(R.string.activity_detail_activities);
        }
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)){
            if(!read_options.equals(""))read_options+=",";
            read_options+=getResources().getString(R.string.activity_detail_receivers);
        }
        if(settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS,Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)){
            if(!read_options.equals(""))read_options+=",";
            read_options+=getResources().getString(R.string.activity_detail_static_loaders);
        }
        if(read_options.trim().equals(""))read_options=getResources().getString(R.string.word_blank);
        ((TextView)findViewById(R.id.settings_loading_options_value)).setText(read_options);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ACTIVITY_RESULT,result_code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE_SET_PATH&&resultCode==RESULT_OK){
            refreshSettingValues();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
