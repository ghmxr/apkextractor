package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.AppItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;
import com.github.ghmxr.apkextractor.utils.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppDetailActivity extends BaseActivity implements View.OnClickListener{
    private AppItem appItem;
    private CheckBox cb_data,cb_obb;
    private ViewGroup permission_views;
    private ViewGroup activity_views;
    private ViewGroup receiver_views;
    private ViewGroup static_loader_views;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appItem=getIntent().getParcelableExtra(EXTRA_PARCELED_APP_ITEM);
        if(appItem==null){
            finish();
            return;
        }
        setContentView(R.layout.activity_app_detail);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_app_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(appItem.getAppName());

        cb_data=findViewById(R.id.app_detail_export_data);
        cb_obb=findViewById(R.id.app_detail_export_obb);
        permission_views=findViewById(R.id.app_detail_permission);
        activity_views=findViewById(R.id.app_detail_activity);
        receiver_views=findViewById(R.id.app_detail_receiver);
        static_loader_views =findViewById(R.id.app_detail_static_loader);

        PackageInfo packageInfo=appItem.getPackageInfo();

        ((TextView)findViewById(R.id.app_detail_name)).setText(appItem.getAppName());
        ((TextView)findViewById(R.id.app_detail_version_name_title)).setText(appItem.getVersionName());
        ((ImageView)findViewById(R.id.app_detail_icon)).setImageDrawable(appItem.getIcon(this));

        ((TextView)findViewById(R.id.app_detail_package_name)).setText(appItem.getPackageName());
        ((TextView)findViewById(R.id.app_detail_version_name)).setText(appItem.getVersionName());
        ((TextView)findViewById(R.id.app_detail_version_code)).setText(String.valueOf(appItem.getVersionCode()));
        ((TextView)findViewById(R.id.app_detail_size)).setText(Formatter.formatFileSize(this,appItem.getSize()));
        ((TextView)findViewById(R.id.app_detail_install_time)).setText(EnvironmentUtil.getFormatDateAndTime(packageInfo.firstInstallTime));
        ((TextView)findViewById(R.id.app_detail_update_time)).setText(EnvironmentUtil.getFormatDateAndTime(packageInfo.lastUpdateTime));
        ((TextView)findViewById(R.id.app_detail_minimum_api)).setText(Build.VERSION.SDK_INT>=24?String.valueOf(packageInfo.applicationInfo.minSdkVersion):getResources().getString(R.string.word_unknown));
        ((TextView)findViewById(R.id.app_detail_target_api)).setText(String.valueOf(packageInfo.applicationInfo.targetSdkVersion));
        ((TextView)findViewById(R.id.app_detail_signature)).setText(EnvironmentUtil.getSignatureStringOfPackageInfo(packageInfo));

        findViewById(R.id.app_detail_run_area).setOnClickListener(this);
        findViewById(R.id.app_detail_export_area).setOnClickListener(this);
        findViewById(R.id.app_detail_share_area).setOnClickListener(this);
        findViewById(R.id.app_detail_detail_area).setOnClickListener(this);
        findViewById(R.id.app_detail_market_area).setOnClickListener(this);
        findViewById(R.id.app_detail_delete_area).setOnClickListener(this);

        String[] permissions=packageInfo.requestedPermissions;
        ActivityInfo[] activities=packageInfo.activities;
        ActivityInfo[] receivers=packageInfo.receivers;

        TextView att_permission=((TextView)findViewById(R.id.app_detail_permission_area_att));
        if(permissions!=null){
            att_permission.setText(getResources().getString(R.string.activity_detail_permissions)
            +"("+permissions.length+getResources().getString(R.string.unit_item)+")");
            for(String permission:permissions){
                if(permission==null)continue;
                permission_views.addView(getSingleItemView(permission_views,permission,null,null));
            }
        }else{
            att_permission.setText(getResources().getString(R.string.activity_detail_permissions)+"(0"+getResources().getString(R.string.unit_item)+")");
        }

        TextView att_activity=((TextView)findViewById(R.id.app_detail_activity_area_att));
        if(activities!=null){
            att_activity.setText(getResources().getString(R.string.activity_detail_activities)
            +"("+activities.length+getResources().getString(R.string.unit_item)+")");
            for(final ActivityInfo info:activities){
                if(info==null)continue;
                activity_views.addView(getSingleItemView(activity_views, info.name, null,new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        try{
                            Intent intent=new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setClassName(info.packageName,info.name);
                            startActivity(intent);
                            return true;
                        }catch (Exception e){
                            e.printStackTrace();
                            ToastManager.showToast(AppDetailActivity.this,e.toString(), Toast.LENGTH_SHORT);
                        }
                        return false;
                    }
                }));
            }
        }else{
            //findViewById(R.id.app_detail_card_activities).setVisibility(View.GONE);
            att_activity.setText(getResources().getString(R.string.activity_detail_activities)+"(0"+getResources().getString(R.string.unit_item)+")");
        }

        TextView att_receiver=findViewById(R.id.app_detail_receiver_area_att);

        if(receivers!=null){
            att_receiver.setText(getResources().getString(R.string.activity_detail_receivers)+"("+receivers.length+getResources().getString(R.string.unit_item)+")");
            for(ActivityInfo activityInfo:receivers){
                receiver_views.addView(getSingleItemView(receiver_views,activityInfo.name,null,null));
            }

        }else {
            att_receiver.setText(getResources().getString(R.string.activity_detail_receivers)+"(0"+getResources().getString(R.string.unit_item)+")");
        }

        TextView att_static_loader=findViewById(R.id.app_detail_static_loader_area_att);
        //Map<String, List<String>>map=appItem.getStaticReceivers();
        Bundle bundle=appItem.getStaticReceiversBundle();
        Set<String>keys=bundle.keySet();
        att_static_loader.setText(getResources().getString(R.string.activity_detail_static_loaders)+"("+keys.size()+getResources().getString(R.string.unit_item)+")");

        for(String s:keys){
            View static_loader_item_view=LayoutInflater.from(this).inflate(R.layout.item_static_loader,static_loader_views,false);
            ((TextView)static_loader_item_view.findViewById(R.id.static_loader_name)).setText(s);
            ViewGroup filter_views=static_loader_item_view.findViewById(R.id.static_loader_intents);
            List<String>filters=bundle.getStringArrayList(s);
            if(filters==null)continue;
            for(String filter:filters){
               View itemView=LayoutInflater.from(this).inflate(R.layout.item_single_textview,filter_views,false);
                ((TextView)itemView.findViewById(R.id.item_textview)).setText(filter);
                filter_views.addView(itemView);
            }
            static_loader_views.addView(static_loader_item_view);
        }

        findViewById(R.id.app_detail_permission_area).setOnClickListener(this);
        findViewById(R.id.app_detail_activity_area).setOnClickListener(this);
        findViewById(R.id.app_detail_receiver_area).setOnClickListener(this);
        findViewById(R.id.app_detail_static_loader_area).setOnClickListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final long data= FileUtil.getFileOrFolderSize(new File(Storage.getMainExternalStoragePath()+"/android/data/"+appItem.getPackageName()));
                final long obb= FileUtil.getFileOrFolderSize(new File(Storage.getMainExternalStoragePath()+"/android/obb/"+appItem.getPackageName()));
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
                Global.checkAndExportCertainAppItemsToSetPathAndShare(this,single_list , new Global.ExportTaskFinishedListener() {
                    @Override
                    public void onFinished(@NonNull String error_message) {
                        ToastManager.showToast(AppDetailActivity.this,getResources().getString(R.string.toast_export_complete)
                                +Global.getAbsoluteWritePath(AppDetailActivity.this,single_list.get(0),(item.exportData||item.exportObb)?"zip":"apk"),Toast.LENGTH_SHORT);
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
            case R.id.app_detail_permission_area:{
                if(permission_views.getVisibility()==View.VISIBLE){
                    findViewById(R.id.app_detail_permission_area_arrow).setRotation(0);
                    permission_views.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content));
                }else {
                    findViewById(R.id.app_detail_permission_area_arrow).setRotation(90);
                    permission_views.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }
            }
            break;
            case R.id.app_detail_activity_area:{
                if(activity_views.getVisibility()==View.VISIBLE){
                    findViewById(R.id.app_detail_activity_area_arrow).setRotation(0);
                    activity_views.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }else{
                    findViewById(R.id.app_detail_activity_area_arrow).setRotation(90);
                    activity_views.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }
            }
            break;
            case R.id.app_detail_receiver_area:{
                if(receiver_views.getVisibility()==View.VISIBLE){
                    findViewById(R.id.app_detail_receiver_area_arrow).setRotation(0);
                    receiver_views.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }else{
                    findViewById(R.id.app_detail_receiver_area_arrow).setRotation(90);
                    receiver_views.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }
            }
            break;
            case R.id.app_detail_static_loader_area:{
                if(static_loader_views.getVisibility()==View.VISIBLE){
                    findViewById(R.id.app_detail_static_loader_area_arrow).setRotation(0);
                    static_loader_views.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }else{
                    findViewById(R.id.app_detail_static_loader_area_arrow).setRotation(90);
                    static_loader_views.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition((ViewGroup)findViewById(android.R.id.content));
                }
            }
            break;
        }
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

    private View getSingleItemView(ViewGroup group,String text,View.OnClickListener clickListener,View.OnLongClickListener longClickListener){
        View view=LayoutInflater.from(this).inflate(R.layout.item_single_textview,group,false);
        ((TextView)view.findViewById(R.id.item_textview)).setText(text);
        view.setOnClickListener(clickListener);
        view.setOnLongClickListener(longClickListener);
        return view;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            ActivityCompat.finishAfterTransition(this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case android.R.id.home:{
                ActivityCompat.finishAfterTransition(this);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
