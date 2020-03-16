package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.GetImportLengthAndDuplicateInfoTask;
import com.github.ghmxr.apkextractor.tasks.GetPackageInfoViewTask;
import com.github.ghmxr.apkextractor.tasks.GetSignatureInfoTask;
import com.github.ghmxr.apkextractor.tasks.HashTask;
import com.github.ghmxr.apkextractor.tasks.ImportTask;
import com.github.ghmxr.apkextractor.ui.AssemblyView;
import com.github.ghmxr.apkextractor.ui.ImportingDialog;
import com.github.ghmxr.apkextractor.ui.SignatureView;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PackageDetailActivity extends BaseActivity implements View.OnClickListener{

    private ImportItem importItem;
    public static final String EXTRA_IMPORT_ITEM_POSITION="import_item_position";
    private CheckBox cb_data,cb_obb,cb_apk;
    private ZipFileUtil.ZipFileInfo zipFileInfo;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
        if(getIntent().getAction()!=null&&getIntent().getAction().equals(Intent.ACTION_VIEW)){
            Uri uri=getIntent().getData();
            if(uri==null||uri.getLastPathSegment()==null){
                ToastManager.showToast(this,getResources().getString(R.string.activity_package_detail_blank), Toast.LENGTH_SHORT);
                finish();
                return;
            }
            if(!uri.getLastPathSegment().toLowerCase().endsWith("zip")&&!uri.getLastPathSegment().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(this).toLowerCase())){
                ToastManager.showToast(this,getResources().getString(R.string.activity_package_detail_invalid_format),Toast.LENGTH_SHORT);
                finish();
                return;
            }
            importItem=new ImportItem(this,new FileItem(this, uri));
        }else{
            try{
                importItem= Global.item_list.get(getIntent().getIntExtra(EXTRA_IMPORT_ITEM_POSITION,-1));
            }catch (Exception e){
                e.printStackTrace();
                ToastManager.showToast(this,getResources().getString(R.string.activity_package_detail_blank), Toast.LENGTH_SHORT);
                finish();
            }
        }
        if(importItem==null){
            ToastManager.showToast(this,getResources().getString(R.string.activity_package_detail_blank), Toast.LENGTH_SHORT);
            finish();
            return;
        }
        setContentView(R.layout.activity_package_detail);
        Toolbar toolbar=findViewById(R.id.toolbar_package_detail);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(String.valueOf(importItem.getItemName()));

        cb_data=findViewById(R.id.package_detail_data);
        cb_obb=findViewById(R.id.package_detail_obb);
        cb_apk=findViewById(R.id.package_detail_apk);
        ((ImageView)findViewById(R.id.package_icon)).setImageDrawable(importItem.getIconDrawable());
        ((TextView)findViewById(R.id.package_detail_name)).setText(importItem.getFileItem().getName());
        ((TextView)findViewById(R.id.package_detail_version_name_title)).setText(importItem.getVersionName());
        ((TextView)findViewById(R.id.package_detail_file_name)).setText(importItem.getFileItem().getName());
        ((TextView)findViewById(R.id.package_detail_size)).setText(Formatter.formatFileSize(this,importItem.getSize()));
        ((TextView)findViewById(R.id.package_detail_version_name)).setText(importItem.getVersionName());
        ((TextView)findViewById(R.id.package_detail_version_code)).setText(importItem.getVersionCode());
        ((TextView)findViewById(R.id.package_detail_minimum_api)).setText(importItem.getMinSdkVersion());
        ((TextView)findViewById(R.id.package_detail_target_api)).setText(importItem.getTargetSdkVersion());
        ((TextView)findViewById(R.id.package_detail_last_modified)).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(importItem.getLastModified())));
        if(importItem.getFileItem().isShareUriInstance()){
            findViewById(R.id.package_detail_last_modified_area).setVisibility(View.GONE);
        }
        if(importItem.getImportType()==ImportItem.ImportType.ZIP){
            findViewById(R.id.package_detail_version_name_title).setVisibility(View.GONE);
            findViewById(R.id.package_detail_version_name_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_version_code_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_minimum_api_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_target_api_area).setVisibility(View.GONE);
            findViewById(R.id.package_detail_target_api_dividing).setVisibility(View.GONE);
        }
        if(importItem.getImportType()== ImportItem.ImportType.APK&&importItem.getPackageInfo()!=null){
            if(SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE,Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)){
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
            new GetPackageInfoViewTask(this,importItem.getPackageInfo(),new Bundle(),(AssemblyView)findViewById(R.id.package_detail_assemble),new GetPackageInfoViewTask.CompletedCallback(){
                @Override
                public void onViewsCreated() {
                    findViewById(R.id.package_detail_card_pg).setVisibility(View.GONE);
                    findViewById(R.id.package_detail_assemble).setVisibility(View.VISIBLE);
                }
            }).start();
        }

        if(SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH,Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)){
            findViewById(R.id.package_detail_hash_att).setVisibility(View.VISIBLE);
            findViewById(R.id.package_detail_hash).setVisibility(View.VISIBLE);
            new HashTask(importItem.getFileItem(), HashTask.HashType.MD5, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_md5_pg).setVisibility(View.GONE);
                    TextView tv_md5=findViewById(R.id.detail_hash_md5_value);
                    tv_md5.setVisibility(View.VISIBLE);
                    tv_md5.setText(result);
                }
            }).start();
            new HashTask(importItem.getFileItem(), HashTask.HashType.SHA1, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha1_pg).setVisibility(View.GONE);
                    TextView tv_sha1=findViewById(R.id.detail_hash_sha1_value);
                    tv_sha1.setVisibility(View.VISIBLE);
                    tv_sha1.setText(result);
                }
            }).start();
            new HashTask(importItem.getFileItem(), HashTask.HashType.SHA256, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha256_pg).setVisibility(View.GONE);
                    TextView tv_sha256=findViewById(R.id.detail_hash_sha256_value);
                    tv_sha256.setVisibility(View.VISIBLE);
                    tv_sha256.setText(result);
                }
            }).start();
            new HashTask(importItem.getFileItem(), HashTask.HashType.CRC32, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_crc32_pg).setVisibility(View.GONE);
                    TextView tv_crc32=findViewById(R.id.detail_hash_crc32_value);
                    tv_crc32.setVisibility(View.VISIBLE);
                    tv_crc32.setText(result);
                }
            }).start();
        }

        if(importItem.getImportType()== ImportItem.ImportType.ZIP){
            findViewById(R.id.package_detail_data_obb_pg).setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long data=0;
                        long obb=0;
                        long apk=0;
                        final ZipFileUtil.ZipFileInfo zipFileInfo=ZipFileUtil.getZipFileInfoOfImportItem(importItem);
                        if(zipFileInfo!=null){
                            data=zipFileInfo.getDataSize();
                            obb=zipFileInfo.getObbSize();
                            apk=zipFileInfo.getApkSize();
                        }
                        final long data_final=data;
                        final long obb_final=obb;
                        final long apk_final=apk;
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                PackageDetailActivity.this.zipFileInfo=zipFileInfo;
                                findViewById(R.id.package_detail_data_obb_pg).setVisibility(View.GONE);
                                findViewById(R.id.package_detail_checkboxes).setVisibility(View.VISIBLE);
                                cb_data.setEnabled(data_final>0);
                                cb_obb.setEnabled(obb_final>0);
                                cb_apk.setEnabled(apk_final>0);
                                cb_data.setText("data:"+Formatter.formatFileSize(PackageDetailActivity.this,data_final));
                                cb_obb.setText("obb:"+Formatter.formatFileSize(PackageDetailActivity.this,obb_final));
                                cb_apk.setText("apk:"+Formatter.formatFileSize(PackageDetailActivity.this,apk_final));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            default:break;
            case R.id.package_detail_install_area:{
                if(importItem.getImportType()== ImportItem.ImportType.APK){
                    try{
                        Intent intent =new Intent(Intent.ACTION_VIEW);
                        if(Build.VERSION.SDK_INT<=23){
                            if(importItem.getFileItem().isFileInstance())intent.setDataAndType(importItem.getUriFromFile(),"application/vnd.android.package-archive");
                            else intent.setDataAndType(importItem.getUri(),"application/vnd.android.package-archive");
                        }else
                            intent.setDataAndType(importItem.getUri(), "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }catch (Exception e){
                        e.printStackTrace();
                        ToastManager.showToast(this,e.toString(),Toast.LENGTH_SHORT);
                    }
                }else if(importItem.getImportType()== ImportItem.ImportType.ZIP){
                    if(!cb_data.isChecked()&&!cb_obb.isChecked()&&!cb_apk.isChecked()){
                        Snackbar.make(findViewById(android.R.id.content),getResources().getString(R.string.activity_detail_nothing_checked),Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    ImportItem importItem1=new ImportItem(importItem,cb_data.isChecked(),cb_obb.isChecked(),cb_apk.isChecked());
                    final ArrayList<ImportItem>singleArray=new ArrayList<>();
                    singleArray.add(importItem1);
                    final ArrayList<ZipFileUtil.ZipFileInfo>zipFileInfos=new ArrayList<>();
                    zipFileInfos.add(zipFileInfo);
                    final AlertDialog dialog_duplication_wait=new AlertDialog.Builder(this)
                            .setTitle(getResources().getString(R.string.dialog_wait))
                            .setView(LayoutInflater.from(this).inflate(R.layout.dialog_duplication_file,null))
                            .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), null)
                            .setCancelable(false)
                            .show();

                    final GetImportLengthAndDuplicateInfoTask infoTask=new GetImportLengthAndDuplicateInfoTask(singleArray,zipFileInfos,new GetImportLengthAndDuplicateInfoTask.GetImportLengthAndDuplicateInfoCallback(){
                        @Override
                        public void onCheckingFinished(@NonNull List<String> duplication_infos, long total) {
                            dialog_duplication_wait.cancel();
                            final ImportingDialog importingDialog=new ImportingDialog(PackageDetailActivity.this,total);
                            final ImportTask.ImportTaskCallback importTaskCallback=new ImportTask.ImportTaskCallback() {
                                @Override
                                public void onImportTaskStarted() {}

                                @Override
                                public void onRefreshSpeed(long speed) {
                                    importingDialog.setSpeed(speed);
                                }

                                @Override
                                public void onImportTaskProgress(@NonNull String writePath, long progress) {
                                    importingDialog.setProgress(progress);
                                    importingDialog.setCurrentWritingName(writePath);
                                }

                                @Override
                                public void onImportTaskFinished(@NonNull String errorMessage) {
                                    importingDialog.cancel();
                                    //if(callback!=null)callback.onImportFinished(errorMessage);
                                    if(!TextUtils.isEmpty(errorMessage)){
                                        new AlertDialog.Builder(PackageDetailActivity.this)
                                                .setTitle(getResources().getString(R.string.dialog_import_finished_error_title))
                                                .setMessage(getResources().getString(R.string.dialog_import_finished_error_message)+errorMessage)
                                                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {}
                                                })
                                                .show();
                                    }else{
                                        ToastManager.showToast(PackageDetailActivity.this,getResources().getString(R.string.toast_import_complete),Toast.LENGTH_SHORT);
                                    }
                                }
                            };
                            final ImportTask importTask=new ImportTask(PackageDetailActivity.this, singleArray,importTaskCallback);
                            importingDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    importTask.setInterrupted();
                                    importingDialog.cancel();
                                }
                            });
                            if(duplication_infos.size()==0){
                                importingDialog.show();
                                importTask.start();
                            }else{
                                StringBuilder stringBuilder=new StringBuilder();
                                int checkingIndex=duplication_infos.size();
                                int unListed=0;
                                if(checkingIndex>100){
                                    unListed=checkingIndex-100;
                                    checkingIndex=100;
                                }
                                for(int i=0;i<checkingIndex;i++){
                                    stringBuilder.append(duplication_infos.get(i));
                                    stringBuilder.append("\n\n");
                                }
                                if(unListed>0){
                                    stringBuilder.append("+");
                                    stringBuilder.append(unListed);
                                    stringBuilder.append(getResources().getString(R.string.dialog_import_duplicate_more));
                                }
                                new AlertDialog.Builder(PackageDetailActivity.this)
                                        .setTitle(getResources().getString(R.string.dialog_import_duplicate_title))
                                        .setMessage(getResources().getString(R.string.dialog_import_duplicate_message)+stringBuilder.toString())
                                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                importingDialog.show();
                                                importTask.start();
                                            }
                                        })
                                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {}
                                        })
                                        .show();
                            }
                        }
                    });
                    infoTask.start();
                    dialog_duplication_wait.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_duplication_wait.cancel();
                            infoTask.setInterrupted();
                        }
                    });
                }
            }
            break;
            case R.id.package_detail_share_area:{
                final ArrayList<ImportItem>importItems=new ArrayList<>();
                importItems.add(importItem);
                Global.shareImportItems(this,importItems);
            }
            break;
            case R.id.package_detail_delete_area:{
                try{
                    new AlertDialog.Builder(this)
                            .setTitle(getResources().getString(R.string.dialog_import_delete_title))
                            .setMessage(getResources().getString(R.string.dialog_import_delete_message))
                            .setPositiveButton(getResources().getString(R.string.action_confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try{
                                        importItem.getFileItem().delete();
                                        finish();
                                        sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            })
                            .show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            break;
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0){
            if(grantResults[0]==PermissionChecker.PERMISSION_GRANTED)recreate();
            else {
                ToastManager.showToast(this,getResources().getString(R.string.permission_write),Toast.LENGTH_SHORT);
                finish();
            }
        }
    }

    private void checkHeightAndFinish(){
        if(Build.VERSION.SDK_INT>=28){ //根布局项目太多时低版本Android会引发一个底层崩溃。版本号暂定28
            ActivityCompat.finishAfterTransition(this);
        }else {
            if(((AssemblyView)findViewById(R.id.package_detail_assemble)).getIsExpanded()){
                finish();
            }else{
                ActivityCompat.finishAfterTransition(this);
            }
        }
    }
}
