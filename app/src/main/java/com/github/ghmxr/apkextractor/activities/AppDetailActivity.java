package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.tasks.GetPackageInfoViewTask;
import com.github.ghmxr.apkextractor.tasks.GetSignatureInfoTask;
import com.github.ghmxr.apkextractor.tasks.HashTask;
import com.github.ghmxr.apkextractor.ui.AssemblyView;
import com.github.ghmxr.apkextractor.ui.SignatureView;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppDetailActivity extends BaseActivity implements View.OnClickListener{
    private AppItem appItem;
    private CheckBox cb_data,cb_obb;
    private final BroadcastReceiver uninstall_receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)||intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)){
                    String data=intent.getDataString();
                    String package_name=data.substring(data.indexOf(":")+1);
                    if(package_name.equalsIgnoreCase(appItem.getPackageName()))finish();
                }
            }catch (Exception e){e.printStackTrace();}
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //appItem=getIntent().getParcelableExtra(EXTRA_PARCELED_APP_ITEM);
        try{
            appItem=Global.getAppItemByPackageNameFromList(Global.app_list,getIntent().getStringExtra(EXTRA_PACKAGE_NAME));
        }catch (Exception e){e.printStackTrace();}
        if(appItem==null){
            ToastManager.showToast(this,"(-_-)The AppItem info is null, try to restart this application.",Toast.LENGTH_SHORT);
            finish();
            return;
        }
        setContentView(R.layout.activity_app_detail);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_app_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(appItem.getAppName());

        cb_data=findViewById(R.id.app_detail_export_data);
        cb_obb=findViewById(R.id.app_detail_export_obb);

        PackageInfo packageInfo=appItem.getPackageInfo();

        ((TextView)findViewById(R.id.app_detail_name)).setText(appItem.getAppName());
        ((TextView)findViewById(R.id.app_detail_version_name_title)).setText(appItem.getVersionName());
        ((ImageView)findViewById(R.id.app_detail_icon)).setImageDrawable(appItem.getIcon());

        ((TextView)findViewById(R.id.app_detail_package_name)).setText(appItem.getPackageName());
        ((TextView)findViewById(R.id.app_detail_version_name)).setText(appItem.getVersionName());
        ((TextView)findViewById(R.id.app_detail_version_code)).setText(String.valueOf(appItem.getVersionCode()));
        ((TextView)findViewById(R.id.app_detail_size)).setText(Formatter.formatFileSize(this,appItem.getSize()));
        ((TextView)findViewById(R.id.app_detail_install_time)).setText(EnvironmentUtil.getFormatDateAndTime(packageInfo.firstInstallTime));
        ((TextView)findViewById(R.id.app_detail_update_time)).setText(EnvironmentUtil.getFormatDateAndTime(packageInfo.lastUpdateTime));
        ((TextView)findViewById(R.id.app_detail_minimum_api)).setText(Build.VERSION.SDK_INT>=24?String.valueOf(packageInfo.applicationInfo.minSdkVersion):getResources().getString(R.string.word_unknown));
        ((TextView)findViewById(R.id.app_detail_target_api)).setText(String.valueOf(packageInfo.applicationInfo.targetSdkVersion));
        ((TextView)findViewById(R.id.app_detail_is_system_app)).setText(getResources().getString((appItem.getPackageInfo().applicationInfo.flags& ApplicationInfo.FLAG_SYSTEM)>0?R.string.word_yes:R.string.word_no));

        getDataObbSizeAndFillView();

        new GetPackageInfoViewTask(this, appItem.getPackageInfo(), appItem.getStaticReceiversBundle(), (AssemblyView) findViewById(R.id.app_detail_assembly), new GetPackageInfoViewTask.CompletedCallback() {
            @Override
            public void onViewsCreated() {
                findViewById(R.id.app_detail_card_pg).setVisibility(View.GONE);
            }
        }).start();

        if(SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE,Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)){
            findViewById(R.id.app_detail_signature_att).setVisibility(View.VISIBLE);
            findViewById(R.id.app_detail_sign_pg).setVisibility(View.VISIBLE);
            new GetSignatureInfoTask(this, appItem.getPackageInfo(), (SignatureView) findViewById(R.id.app_detail_signature), new GetSignatureInfoTask.CompletedCallback() {
                @Override
                public void onCompleted() {
                    findViewById(R.id.app_detail_sign_pg).setVisibility(View.GONE);
                }
            }).start();
        }

        if(SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH,Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)){
            findViewById(R.id.app_detail_hash_att).setVisibility(View.VISIBLE);
            findViewById(R.id.app_detail_hash).setVisibility(View.VISIBLE);
            new HashTask(appItem.getFileItem(), HashTask.HashType.MD5, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_md5_pg).setVisibility(View.GONE);
                    TextView tv_md5=findViewById(R.id.detail_hash_md5_value);
                    tv_md5.setVisibility(View.VISIBLE);
                    tv_md5.setText(result);
                }
            }).start();
            new HashTask(appItem.getFileItem(), HashTask.HashType.SHA1, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha1_pg).setVisibility(View.GONE);
                    TextView tv_sha1=findViewById(R.id.detail_hash_sha1_value);
                    tv_sha1.setVisibility(View.VISIBLE);
                    tv_sha1.setText(result);
                }
            }).start();
            new HashTask(appItem.getFileItem(), HashTask.HashType.SHA256, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha256_pg).setVisibility(View.GONE);
                    TextView tv_sha256=findViewById(R.id.detail_hash_sha256_value);
                    tv_sha256.setVisibility(View.VISIBLE);
                    tv_sha256.setText(result);
                }
            }).start();
            new HashTask(appItem.getFileItem(), HashTask.HashType.CRC32, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_crc32_pg).setVisibility(View.GONE);
                    TextView tv_crc32=findViewById(R.id.detail_hash_crc32_value);
                    tv_crc32.setVisibility(View.VISIBLE);
                    tv_crc32.setText(result);
                }
            }).start();
        }

        try{
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            intentFilter.addDataScheme("package");
            registerReceiver(uninstall_receiver,intentFilter);
        }catch (Exception e){e.printStackTrace();}
    }

    private void getDataObbSizeAndFillView(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long data= FileUtil.getFileOrFolderSize(new File(StorageUtil.getMainExternalStoragePath()+"/android/data/"+appItem.getPackageName()));
                final long obb= FileUtil.getFileOrFolderSize(new File(StorageUtil.getMainExternalStoragePath()+"/android/obb/"+appItem.getPackageName()));
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.app_detail_export_progress_bar).setVisibility(View.GONE);
                        cb_data.setText("Data:"+Formatter.formatFileSize(AppDetailActivity.this,data));
                        cb_obb.setText("Obb:"+Formatter.formatFileSize(AppDetailActivity.this,obb));
                        cb_data.setEnabled(data>0);
                        cb_obb.setEnabled(obb>0);
                        findViewById(R.id.app_detail_export_checkboxes).setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            default:break;
            case R.id.app_detail_run_area:{
                try{
                    startActivity(getPackageManager().getLaunchIntentForPackage(appItem.getPackageName()));
                }catch (Exception e){
                    ToastManager.showToast(AppDetailActivity.this,e.toString(),Toast.LENGTH_SHORT);
                }
            }
            break;
            case R.id.app_detail_export_area:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    Global.showRequestingWritePermissionSnackBar(this);
                    return;
                }
                final List<AppItem>single_list=getSingleItemArrayList(true);
                final AppItem item=single_list.get(0);
                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(this,single_list , false,new Global.ExportTaskFinishedListener() {
                    @Override
                    public void onFinished(@NonNull String error_message) {
                        if(!error_message.trim().equals("")){
                            new AlertDialog.Builder(AppDetailActivity.this)
                                    .setTitle(getResources().getString(R.string.exception_title))
                                    .setMessage(getResources().getString(R.string.exception_message)+error_message)
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                            return;
                        }
                        ToastManager.showToast(AppDetailActivity.this,getResources().getString(R.string.toast_export_complete)+" "
                                +SPUtil.getDisplayingExportPath(AppDetailActivity.this)
                                +OutputUtil.getWriteFileNameForAppItem(AppDetailActivity.this,single_list.get(0),(item.exportData||item.exportObb)?
                                SPUtil.getCompressingExtensionName(AppDetailActivity.this):"apk"),Toast.LENGTH_SHORT);
                    }
                });
            }
            break;
            case R.id.app_detail_share_area:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    Global.showRequestingWritePermissionSnackBar(this);
                    return;
                }
                Global.shareCertainAppsByItems(this,getSingleItemArrayList(false));
            }
            break;
            case R.id.app_detail_detail_area:{
                Intent intent=new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", appItem.getPackageName(), null));
                startActivity(intent);
            }
            break;
            case R.id.app_detail_market_area:{
                try{
                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appItem.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }catch (Exception e){
                    ToastManager.showToast(AppDetailActivity.this,e.toString(),Toast.LENGTH_SHORT);
                }
            }
            break;
            case R.id.app_detail_delete_area:{
                try{
                    Intent uninstall_intent = new Intent();
                    uninstall_intent.setAction(Intent.ACTION_DELETE);
                    uninstall_intent.setData(Uri.parse("package:"+appItem.getPackageName()));
                    startActivity(uninstall_intent);
                }catch (Exception e){
                    ToastManager.showToast(AppDetailActivity.this,e.toString(),Toast.LENGTH_SHORT);
                }
            }
            break;

            case R.id.app_detail_package_name_area:{
                clip2ClipboardAndShowSnackbar(appItem.getPackageName());
            }
            break;
            case R.id.app_detail_version_name_area:{
                clip2ClipboardAndShowSnackbar(appItem.getVersionName());
            }
            break;
            case R.id.app_detail_version_code_area:{
                clip2ClipboardAndShowSnackbar(String.valueOf(appItem.getVersionCode()));
            }
            break;
            case R.id.app_detail_size_area:{
                clip2ClipboardAndShowSnackbar(Formatter.formatFileSize(this,appItem.getSize()));
            }
            break;
            case R.id.app_detail_install_time_area:{
                clip2ClipboardAndShowSnackbar(EnvironmentUtil.getFormatDateAndTime(appItem.getPackageInfo().firstInstallTime));
            }
            break;
            case R.id.app_detail_update_time_area:{
                clip2ClipboardAndShowSnackbar(EnvironmentUtil.getFormatDateAndTime(appItem.getPackageInfo().lastUpdateTime));
            }
            break;
            case R.id.app_detail_minimum_api_area:{
                if(Build.VERSION.SDK_INT>=24)clip2ClipboardAndShowSnackbar(String.valueOf(appItem.getPackageInfo().applicationInfo.minSdkVersion));
            }
            break;
            case R.id.app_detail_target_api_area:{
                clip2ClipboardAndShowSnackbar(String.valueOf(appItem.getPackageInfo().applicationInfo.targetSdkVersion));
            }
            break;
            case R.id.app_detail_is_system_app_area:{
                clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_is_system_app)).getText().toString());
            }
            break;
            case R.id.detail_hash_md5:{
                clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.detail_hash_md5_value)).getText().toString());
            }
            break;
            case R.id.detail_hash_sha1:{
                clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.detail_hash_sha1_value)).getText().toString());
            }
            break;
            case R.id.detail_hash_sha256:{
                clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.detail_hash_sha256_value)).getText().toString());
            }
            break;
            case R.id.detail_hash_crc32:{
                clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.detail_hash_crc32_value)).getText().toString());
            }
            break;
        }
    }

    private void clip2ClipboardAndShowSnackbar(String s){
        try{
            ClipboardManager manager=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("message",s));
            Snackbar.make(findViewById(android.R.id.content),getResources().getString(R.string.snack_bar_clipboard),Snackbar.LENGTH_SHORT).show();
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * 构造包含单个副本AppItem的ArrayList
     */
    private @NonNull ArrayList<AppItem>getSingleItemArrayList(boolean put_checkbox_value){
        ArrayList<AppItem>list=new ArrayList<>();
        AppItem item=new AppItem(appItem,false,false);
        if(put_checkbox_value){
            item.exportData=cb_data.isChecked();
            item.exportObb=cb_obb.isChecked();
        }
        list.add(item);
        return list;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkHeightAndFinish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkHeightAndFinish(){
        if(Build.VERSION.SDK_INT>=28){ //根布局项目太多时低版本Android会引发一个底层崩溃。版本号暂定28
            ActivityCompat.finishAfterTransition(this);
        }else {
            if(((AssemblyView)findViewById(R.id.app_detail_assembly)).getIsExpanded()){
                finish();
            }else{
                ActivityCompat.finishAfterTransition(this);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        try{
            unregisterReceiver(uninstall_receiver);
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case android.R.id.home:{
                checkHeightAndFinish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
