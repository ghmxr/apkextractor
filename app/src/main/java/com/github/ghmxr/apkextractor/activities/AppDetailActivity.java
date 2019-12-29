package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
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

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.FileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

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

    //private int item_permission=0,item_activity=0,item_receiver=0,item_loader=0;
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
        final SharedPreferences settings= SPUtil.getGlobalSharedPreferences(this);

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
        ((TextView)findViewById(R.id.app_detail_signature)).setText(EnvironmentUtil.getSignatureStringOfPackageInfo(packageInfo));

        findViewById(R.id.app_detail_run_area).setOnClickListener(this);
        findViewById(R.id.app_detail_export_area).setOnClickListener(this);
        findViewById(R.id.app_detail_share_area).setOnClickListener(this);
        findViewById(R.id.app_detail_detail_area).setOnClickListener(this);
        findViewById(R.id.app_detail_market_area).setOnClickListener(this);
        findViewById(R.id.app_detail_delete_area).setOnClickListener(this);

        findViewById(R.id.app_detail_permission_area).setOnClickListener(this);
        findViewById(R.id.app_detail_activity_area).setOnClickListener(this);
        findViewById(R.id.app_detail_receiver_area).setOnClickListener(this);
        findViewById(R.id.app_detail_static_loader_area).setOnClickListener(this);

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



        final String[] permissions=packageInfo.requestedPermissions;
        final ActivityInfo[] activities=packageInfo.activities;
        final ActivityInfo[] receivers=packageInfo.receivers;

        final boolean get_permissions=settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT);
        final boolean get_activities=settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT);
        final boolean get_receivers=settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT);
        final boolean get_static_loaders=settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS,Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<View>permission_child_views=new ArrayList<>();
                final ArrayList<View>activity_child_views=new ArrayList<>();
                final ArrayList<View>receiver_child_views=new ArrayList<>();
                final ArrayList<View>loaders_child_views=new ArrayList<>();

                if(permissions!=null&&get_permissions){
                    for(final String s:permissions){
                        if(s==null)continue;
                        permission_child_views.add(getSingleItemView(permission_views, s, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clip2ClipboardAndShowSnackbar(s);
                            }
                        }, null));
                    }
                }
                if(activities!=null&&get_activities){
                    for(final ActivityInfo info:activities){
                        activity_child_views.add(getSingleItemView(activity_views, info.name, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clip2ClipboardAndShowSnackbar(info.name);
                            }
                        }, new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                try {
                                    Intent intent = new Intent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setClassName(info.packageName, info.name);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    ToastManager.showToast(AppDetailActivity.this, e.toString(), Toast.LENGTH_SHORT);
                                }
                                return true;
                            }
                        }));
                    }
                }
                if(receivers!=null&&get_receivers){
                    for(final ActivityInfo activityInfo:receivers){
                        receiver_child_views.add(getSingleItemView(receiver_views, activityInfo.name, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clip2ClipboardAndShowSnackbar(activityInfo.name);
                            }
                        }, null));
                    }
                }

                Bundle bundle=appItem.getStaticReceiversBundle();
                final Set<String>keys=bundle.keySet();
                if(get_static_loaders){
                    for(final String s:keys){
                        View static_loader_item_view=LayoutInflater.from(AppDetailActivity.this).inflate(R.layout.item_static_loader,static_loader_views,false);
                        ((TextView)static_loader_item_view.findViewById(R.id.static_loader_name)).setText(s);
                        static_loader_item_view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clip2ClipboardAndShowSnackbar(s);
                            }
                        });
                        ViewGroup filter_views=static_loader_item_view.findViewById(R.id.static_loader_intents);
                        List<String>filters=bundle.getStringArrayList(s);
                        if(filters==null)continue;
                        for(final String filter:filters){
                            View itemView=LayoutInflater.from(AppDetailActivity.this).inflate(R.layout.item_single_textview,filter_views,false);
                            ((TextView)itemView.findViewById(R.id.item_textview)).setText(filter);
                            itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    clip2ClipboardAndShowSnackbar(filter);
                                }
                            });
                            filter_views.addView(itemView);
                        }
                        loaders_child_views.add(static_loader_item_view);
                    }
                }

                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(get_permissions) {
                            for(View view:permission_child_views) permission_views.addView(view);
                            TextView att_permission=((TextView)findViewById(R.id.app_detail_permission_area_att));
                            att_permission.setText(getResources().getString(R.string.activity_detail_permissions)
                                    +"("+permission_child_views.size()+getResources().getString(R.string.unit_item)+")");
                        }
                        if(get_activities) {
                            for(View view:activity_child_views)activity_views.addView(view);
                            TextView att_activity=((TextView)findViewById(R.id.app_detail_activity_area_att));
                            att_activity.setText(getResources().getString(R.string.activity_detail_activities)
                                    +"("+activity_child_views.size()+getResources().getString(R.string.unit_item)+")");
                        }
                        if(get_receivers) {
                            for(View view:receiver_child_views) receiver_views.addView(view);
                            TextView att_receiver=findViewById(R.id.app_detail_receiver_area_att);
                            att_receiver.setText(getResources().getString(R.string.activity_detail_receivers)+"("+receiver_child_views.size()+getResources().getString(R.string.unit_item)+")");
                        }
                        if(get_static_loaders) {
                            for(View view:loaders_child_views) static_loader_views.addView(view);
                            TextView att_static_loader=findViewById(R.id.app_detail_static_loader_area_att);
                            att_static_loader.setText(getResources().getString(R.string.activity_detail_static_loaders)+"("+keys.size()+getResources().getString(R.string.unit_item)+")");
                        }

                        //item_permission=permission_child_views.size();
                        //item_activity=activity_child_views.size();
                        //item_receiver=receiver_child_views.size();
                        //item_loader=loaders_child_views.size();

                        findViewById(R.id.app_detail_card_pg).setVisibility(View.GONE);
                        findViewById(R.id.app_detail_card_permissions).setVisibility(get_permissions?View.VISIBLE:View.GONE);
                        findViewById(R.id.app_detail_card_activities).setVisibility(get_activities?View.VISIBLE:View.GONE);
                        findViewById(R.id.app_detail_card_receivers).setVisibility(get_receivers?View.VISIBLE:View.GONE);
                        findViewById(R.id.app_detail_card_static_loaders).setVisibility(get_static_loaders?View.VISIBLE:View.GONE);
                    }
                });

            }
        }).start();
        try{
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            intentFilter.addDataScheme("package");
            registerReceiver(uninstall_receiver,intentFilter);
        }catch (Exception e){e.printStackTrace();}
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
                                +OutputUtil.getWriteFileNameForAppItem(AppDetailActivity.this,single_list.get(0),(item.exportData||item.exportObb)?"zip":"apk"),Toast.LENGTH_SHORT);
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
            case R.id.app_detail_signature_area:{
                clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_signature)).getText().toString());
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
            checkHeightAndFinish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkHeightAndFinish(){
        //SharedPreferences settings=Global.getGlobalSharedPreferences(this);
        //boolean show_permissions=settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS,Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT);
        //boolean show_activities=settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES,Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT);
        //boolean show_receivers=settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS,Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT);
        //boolean show_static_loaders=settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS,Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT);
        //int visible_items=(permission_views.getVisibility()==View.VISIBLE?item_permission:0)+(activity_views.getVisibility()==View.VISIBLE?item_activity:0)
                //+(receiver_views.getVisibility()==View.VISIBLE?item_receiver:0)+(static_loader_views.getVisibility()==View.VISIBLE?item_loader:0);
        boolean normal_finish=permission_views.getVisibility()==View.VISIBLE||activity_views.getVisibility()==View.VISIBLE||receiver_views.getVisibility()==View.VISIBLE
                ||static_loader_views.getVisibility()==View.VISIBLE;

        if(Build.VERSION.SDK_INT>=28){ //根布局项目太多时低版本Android会引发一个底层崩溃。版本号暂定28
            ActivityCompat.finishAfterTransition(this);
        }else {
            if(normal_finish)finish();
            else ActivityCompat.finishAfterTransition(this);
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
