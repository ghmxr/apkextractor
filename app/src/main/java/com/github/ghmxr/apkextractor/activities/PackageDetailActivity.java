package com.github.ghmxr.apkextractor.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.GetPackageInfoViewTask;
import com.github.ghmxr.apkextractor.tasks.GetSignatureInfoTask;
import com.github.ghmxr.apkextractor.ui.AssemblyView;
import com.github.ghmxr.apkextractor.ui.SignatureView;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;

public class PackageDetailActivity extends BaseActivity implements View.OnClickListener{

    private ImportItem importItem;
    public static final String EXTRA_IMPORT_ITEM_POSITION="import_item_position";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        try{
            importItem= Global.item_list.get(getIntent().getIntExtra(EXTRA_IMPORT_ITEM_POSITION,-1));
        }catch (Exception e){
            e.printStackTrace();
            ToastManager.showToast(this,"Can not get the import item, try to refresh and re-open", Toast.LENGTH_SHORT);
            finish();
        }
        if(importItem==null){
            return;
        }
        setContentView(R.layout.activity_package_detail);
        Toolbar toolbar=findViewById(R.id.toolbar_package_detail);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(String.valueOf(importItem.getItemName()));

        ((ImageView)findViewById(R.id.package_icon)).setImageDrawable(importItem.getIconDrawable());
        ((TextView)findViewById(R.id.package_detail_name)).setText(importItem.getFileItem().getName());
        ((TextView)findViewById(R.id.package_detail_version_name_title)).setText(importItem.getVersionName());
        ((TextView)findViewById(R.id.package_detail_file_name)).setText(importItem.getFileItem().getName());
        ((TextView)findViewById(R.id.package_detail_size)).setText(Formatter.formatFileSize(this,importItem.getSize()));
        ((TextView)findViewById(R.id.package_detail_version_name)).setText(importItem.getVersionName());
        ((TextView)findViewById(R.id.package_detail_version_code)).setText(importItem.getVersionCode());
        ((TextView)findViewById(R.id.package_detail_minimum_api)).setText(importItem.getMinSdkVersion());
        ((TextView)findViewById(R.id.package_detail_target_api)).setText(importItem.getTargetSdkVersion());
        if(importItem.getImportType()== ImportItem.ImportType.APK&&importItem.getPackageInfo()!=null){
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

            new GetPackageInfoViewTask(this,importItem.getPackageInfo(),new Bundle(),(AssemblyView)findViewById(R.id.package_detail_assemble),new GetPackageInfoViewTask.CompletedCallback(){
                @Override
                public void onViewsCreated() {
                    findViewById(R.id.package_detail_card_pg).setVisibility(View.GONE);
                    findViewById(R.id.package_detail_assemble).setVisibility(View.VISIBLE);
                }
            }).start();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    final String md5= EnvironmentUtil.hashMD5Value(importItem.getFileItem().getInputStream());
                    final String sha1=EnvironmentUtil.hashSHA1Value(importItem.getFileItem().getInputStream());
                    final String sha256=EnvironmentUtil.hashSHA256Value(importItem.getFileItem().getInputStream());
                    final String crc32= Integer.toHexString((int)FileUtil.getCRC32FromInputStream(importItem.getFileItem().getInputStream()).getValue());
                    Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.package_detail_hash).setVisibility(View.VISIBLE);
                            (findViewById(R.id.package_detail_hash_pg)).setVisibility(View.GONE);
                            ((TextView)findViewById(R.id.detail_hash_md5_value)).setText(md5);
                            ((TextView)findViewById(R.id.detail_hash_sha1_value)).setText(sha1);
                            ((TextView)findViewById(R.id.detail_hash_sha256_value)).setText(sha256);
                            ((TextView)findViewById(R.id.detail_hash_crc32_value)).setText(crc32);
                        }
                    });
                }catch (Exception e){e.printStackTrace();}
            }
        }).start();

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            default:break;
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
