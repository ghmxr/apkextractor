package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.RefreshImportListTask;
import com.github.ghmxr.apkextractor.tasks.RefreshInstalledListTask;
import com.github.ghmxr.apkextractor.tasks.SearchTask;
import com.github.ghmxr.apkextractor.ui.AppItemSortConfigDialog;
import com.github.ghmxr.apkextractor.ui.ImportItemSortConfigDialog;
import com.github.ghmxr.apkextractor.ui.SortConfigDialogCallback;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener,ViewPager.OnPageChangeListener, CompoundButton.OnCheckedChangeListener
, NavigationView.OnNavigationItemSelectedListener {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private int currentSelection=0;
    private boolean isSearchMode=false;
    private SearchView searchView;
    private Menu menu;
    private InputMethodManager inputMethodManager;

    private SearchTask searchTask;

    private RecyclerView recyclerView_export;
    private RecyclerView recyclerView_import;

    private boolean isCurrentPageMultiSelectMode=false;

    private CardView card_export_normal,card_export_multi_select;
    private TextView tv_card_export_multi_select_title,tv_card_import_multi_select_title;
    private TextView tv_storage;
    private CardView card_import_multi_select;

    private ViewGroup loading_area_export;
    private ProgressBar pg_export;
    private TextView tv_export_progress;

    private ViewGroup no_content_att_export,no_content_att_import;

    private Button btn_export_select_all,btn_export_deselect_all,btn_export_action,btn_export_share,btn_import_select_all,btn_import_delete,btn_import_action,btn_import_share;

    private CheckBox cb_show_sys_app;

    private SwipeRefreshLayout swipeRefreshLayout_export,swipeRefreshLayout_import;

    private SharedPreferences settings;

    private boolean isScrollable_export=false;

    final RecyclerView.OnScrollListener onScrollListener_export=new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if(card_export_normal==null||card_export_multi_select==null)return;
            boolean isMultiSelectMode=false;
            try{
                isMultiSelectMode=((ListAdapter)recyclerView.getAdapter()).isMultiSelectMode();
            }catch (Exception e){e.printStackTrace();}
            if (!recyclerView.canScrollVertically(-1)) {
                // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
                // onScrolledToBottom();
                if(isMultiSelectMode){
                    if(isScrollable_export&&card_export_multi_select.getVisibility()!= View.GONE)
                        setViewVisibilityWithAnimation(card_export_multi_select,View.GONE);
                }else if(isScrollable_export&&card_export_normal.getVisibility()!=View.GONE&&!isSearchMode)
                    setViewVisibilityWithAnimation(card_export_normal,View.GONE);
            } else if (dy < 0) {
                // onScrolledUp();
                if(isMultiSelectMode){
                    if(isScrollable_export&&card_export_multi_select.getVisibility()!=View.VISIBLE)
                        setViewVisibilityWithAnimation(card_export_multi_select,View.VISIBLE);
                }else if(isScrollable_export&&card_export_normal.getVisibility()!=View.VISIBLE&&!isSearchMode)
                    setViewVisibilityWithAnimation(card_export_normal,View.VISIBLE);
            } else if (dy > 0) {
                // onScrolledDown();
                isScrollable_export=true;
                if(isMultiSelectMode){
                    if(card_export_multi_select.getVisibility()!= View.GONE)
                        setViewVisibilityWithAnimation(card_export_multi_select,View.GONE);
                }else if(card_export_normal.getVisibility()!=View.GONE&&!isSearchMode)
                    setViewVisibilityWithAnimation(card_export_normal,View.GONE);
            }
        }
    };

    final RefreshInstalledListTask.RefreshInstalledListTaskCallback refreshInstalledListTaskCallback=new RefreshInstalledListTask.RefreshInstalledListTaskCallback() {
        @Override
        public void onRefreshProgressStarted(int total) {
            isScrollable_export=false;
            loading_area_export.setVisibility(View.VISIBLE);
            recyclerView_export.setAdapter(null);
            no_content_att_export.setVisibility(View.GONE);
            pg_export.setMax(total);
            pg_export.setProgress(0);
            swipeRefreshLayout_export.setRefreshing(true);
        }

        @Override
        public void onRefreshProgressUpdated(int current,int total) {
            pg_export.setProgress(current);
            tv_export_progress.setText(getResources().getString(R.string.dialog_loading_title)+" "+current+"/"+total);
        }

        @Override
        public void onRefreshCompleted(List<AppItem> appList) {
            loading_area_export.setVisibility(View.GONE);
            no_content_att_export.setVisibility(appList.size()==0?View.VISIBLE:View.GONE);
            swipeRefreshLayout_export.setRefreshing(false);
            int mode= SPUtil.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                    ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            ListAdapter adapter=new ListAdapter<>(appList,mode,list_export_operation_callback);
            setRecyclerViewLayoutManagers(recyclerView_export,mode);
            recyclerView_export.setAdapter(adapter);
            cb_show_sys_app.setEnabled(true);
            swipeRefreshLayout_export.setRefreshing(false);
            card_export_normal.setVisibility(View.VISIBLE);
            refreshAvailableStorage();
        }
    };

    private final ListAdapterOperationListener list_export_operation_callback=new ListAdapterOperationListener() {
        @Override
        public void onItemClicked(DisplayItem displayItem,ViewHolder viewHolder) {
            if(!(displayItem instanceof AppItem))return;
            AppItem appItem=(AppItem)displayItem;
            Intent intent=new Intent(MainActivity.this,AppDetailActivity.class);
            intent.putExtra(EXTRA_PACKAGE_NAME,appItem.getPackageName());
            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this,new Pair<View, String>(viewHolder.icon,"icon"));
            try{
                ActivityCompat.startActivity(MainActivity.this, intent, compat.toBundle());
            }catch (Exception e){e.printStackTrace();}
        }

        @Override
        public void onItemLongClicked() {
            card_export_normal.setVisibility(View.GONE);
            setViewVisibilityWithAnimation(card_export_multi_select,View.VISIBLE);
            isCurrentPageMultiSelectMode=true;
            swipeRefreshLayout_export.setEnabled(false);
            setBackButtonVisible(true);
            hideInputMethod();
        }

        @Override
        public void onMultiSelectItemChanged(List<DisplayItem> selected_items,long length) {
            tv_card_export_multi_select_title.setText(selected_items.size()+getResources().getString(R.string.unit_item)+"/"+Formatter.formatFileSize(MainActivity.this,length));
            btn_export_action.setEnabled(selected_items.size()>0);
            btn_export_share.setEnabled(selected_items.size()>0);
        }

        @Override
        public void onMultiSelectModeClosed() {
            swipeRefreshLayout_export.setEnabled(true);
            card_export_multi_select.setVisibility(View.GONE);
            if(!isSearchMode){
                setBackButtonVisible(false);
                setViewVisibilityWithAnimation(card_export_normal,View.VISIBLE);
            }
        }
    };

    private boolean isScrollable_import=false;
    final RecyclerView.OnScrollListener onScrollListener_import=new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            boolean isMultiSelectMode=false;
            try{
                isMultiSelectMode=((ListAdapter)recyclerView.getAdapter()).isMultiSelectMode();
            }catch (Exception e){e.printStackTrace();}
            if (!recyclerView.canScrollVertically(-1)) {
                // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
                // onScrolledToBottom();
                if(isMultiSelectMode){
                    if(isScrollable_import&&card_import_multi_select.getVisibility()!= View.GONE)
                        setViewVisibilityWithAnimation(card_import_multi_select,View.GONE);
                }
            } else if (dy < 0) {
                // onScrolledUp();
                if(isMultiSelectMode){
                    if(isScrollable_import&&card_import_multi_select.getVisibility()!=View.VISIBLE){
                        setViewVisibilityWithAnimation(card_import_multi_select,View.VISIBLE);
                    }
                }
            } else if (dy > 0) {
                // onScrolledDown();
                isScrollable_import=true;
                if(isMultiSelectMode){
                    if(card_import_multi_select.getVisibility()!= View.GONE)
                        setViewVisibilityWithAnimation(card_import_multi_select,View.GONE);
                }
            }
        }
    };

    /**
     * 导入列表刷新完成回调
     */
    final RefreshImportListTask.RefreshImportListTaskCallback refreshImportListTaskCallback=new RefreshImportListTask.RefreshImportListTaskCallback() {
        @Override
        public void onRefreshStarted() {
            recyclerView_import.setAdapter(null);
            swipeRefreshLayout_import.setRefreshing(true);
            no_content_att_import.setVisibility(View.GONE);
        }

        @Override
        public void onRefreshCompleted(List<ImportItem> list) {
            swipeRefreshLayout_import.setRefreshing(false);
            isScrollable_import=false;
            int mode= SPUtil.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                    ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            ListAdapter adapter=new ListAdapter<>(list,mode,listAdapterOperationListener_import);
            setRecyclerViewLayoutManagers(recyclerView_import,mode);
            recyclerView_import.setAdapter(adapter);
            no_content_att_import.setVisibility(list.size()==0?View.VISIBLE:View.GONE);
        }
    };

    /**
     * 导入RecyclerView的操作回调
     */
    ListAdapterOperationListener listAdapterOperationListener_import=new ListAdapterOperationListener() {
        @Override
        public void onItemClicked(DisplayItem displayItem,ViewHolder viewHolder) {
            if(!(displayItem instanceof ImportItem))return;
            ImportItem item=(ImportItem)displayItem;
            if(item.getImportType()== ImportItem.ImportType.APK){
                try{
                    Intent intent =new Intent(Intent.ACTION_VIEW);
                    if(Build.VERSION.SDK_INT<=23){
                        if(item.getFileItem().isFileInstance())intent.setDataAndType(item.getUriFromFile(),"application/vnd.android.package-archive");
                        else intent.setDataAndType(item.getUri(),"application/vnd.android.package-archive");
                    }else
                        intent.setDataAndType(item.getUri(), "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    ToastManager.showToast(MainActivity.this,e.toString(),Toast.LENGTH_SHORT);
                }
            }
            else if(item.getImportType()== ImportItem.ImportType.ZIP){
                ArrayList<ImportItem>arrayList=new ArrayList<>();
                arrayList.add(item);
                Global.showImportingDataObbDialog(MainActivity.this, arrayList, new Global.ImportTaskFinishedCallback() {
                    @Override
                    public void onImportFinished(String error_message) {
                        if(!error_message.trim().equals("")){
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(getResources().getString(R.string.dialog_import_finished_error_title))
                                    .setMessage(getResources().getString(R.string.dialog_import_finished_error_message)+error_message)
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                        }else ToastManager.showToast(MainActivity.this,getResources().getString(R.string.toast_import_complete),Toast.LENGTH_SHORT);
                    }
                });
            }
        }

        @Override
        public void onItemLongClicked() {
            isCurrentPageMultiSelectMode=true;
            setViewVisibilityWithAnimation(card_import_multi_select,View.VISIBLE);
            swipeRefreshLayout_import.setEnabled(false);
            setBackButtonVisible(true);
            hideInputMethod();
        }

        @Override
        public void onMultiSelectItemChanged(List<DisplayItem> selected_items,long length) {
            tv_card_import_multi_select_title.setText(selected_items.size()+getResources().getString(R.string.unit_item)+"/"+Formatter.formatFileSize(MainActivity.this,length));
            btn_import_action.setEnabled(selected_items.size()>0);
            btn_import_delete.setEnabled(selected_items.size()>0);
            btn_import_share.setEnabled(selected_items.size()>0);
        }

        @Override
        public void onMultiSelectModeClosed() {
            swipeRefreshLayout_import.setEnabled(true);
            card_import_multi_select.setVisibility(View.GONE);
            if(!isSearchMode)setBackButtonVisible(false);
        }
    };

    private final BroadcastReceiver refresh_status_receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent==null)return;
            if(intent.getAction()==null)return;
            if(intent.getAction().equals(Constants.ACTION_REFRESH_APP_LIST)){
                new RefreshInstalledListTask(MainActivity.this,cb_show_sys_app.isChecked(),refreshInstalledListTaskCallback).start();
            }else if(intent.getAction().equals(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST)){
                new RefreshImportListTask(MainActivity.this,refreshImportListTaskCallback).start();
            }else if(intent.getAction().equals(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE)){
                refreshAvailableStorage();
            }
        }
    };

    private final BroadcastReceiver package_status_changed_receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent==null||intent.getAction()==null)return;
            if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)||intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
                new RefreshInstalledListTask(MainActivity.this,cb_show_sys_app.isChecked(),refreshInstalledListTaskCallback).start();
            }
        }
    };

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try{
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }catch (Exception e){e.printStackTrace();}

        drawerLayout=findViewById(R.id.main_drawer);
        ActionBarDrawerToggle actionBarDrawerToggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar,0,0){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                hideInputMethod();
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        NavigationView navigationView=findViewById(R.id.main_navigation);
        navigationView.setNavigationItemSelectedListener(this);

        tabLayout=findViewById(R.id.main_tablayout);
        viewPager=findViewById(R.id.main_viewpager);
        inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        settings=SPUtil.getGlobalSharedPreferences(this);

        try{
            View view=LayoutInflater.from(this).inflate(R.layout.actionbar_search,null);
            searchView=view.findViewById(R.id.actionbar_main_search);
            getSupportActionBar().setCustomView(view);
            int imgId = searchView.getContext().getResources().getIdentifier("android:id/search_mag_icon",null,null);
            int plateId=searchView.getContext().getResources().getIdentifier("android:id/search_plate",null,null);
            int submitId=searchView.getContext().getResources().getIdentifier("android:id/submit_area",null,null);
            int textId=searchView.getContext().getResources().getIdentifier("android:id/search_src_text",null,null);
            int closeId=searchView.getContext().getResources().getIdentifier("android:id/search_close_btn",null,null);
            ((ImageView)searchView.findViewById(imgId)).setImageResource(R.drawable.icon_search);
            ((TextView)searchView.findViewById(textId)).setTextColor(getResources().getColor(R.color.colorHighLightTextDarkBackground));
            ((TextView)searchView.findViewById(textId)).setHintTextColor(getResources().getColor(R.color.colorDividingLine));
            ((ImageView)searchView.findViewById(closeId)).setImageResource(R.drawable.icon_close);
            if(Build.VERSION.SDK_INT>=16){
                searchView.findViewById(plateId).setBackground(null);
                searchView.findViewById(submitId).setBackground(null);
            }
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if(searchTask!=null)searchTask.setInterrupted();
                    recyclerView_export.setAdapter(null);
                    recyclerView_import.setAdapter(null);
                    card_export_multi_select.setVisibility(View.GONE);
                    card_import_multi_select.setVisibility(View.GONE);
                    swipeRefreshLayout_export.setRefreshing(true);
                    swipeRefreshLayout_import.setRefreshing(true);
                    searchTask=new SearchTask(Global.app_list,Global.item_list ,newText, new SearchTask.SearchTaskCompletedCallback() {
                        @Override
                        public void onSearchTaskCompleted(@NonNull List<AppItem> appItems,@NonNull List<ImportItem>importItems) {
                            swipeRefreshLayout_export.setRefreshing(false);
                            swipeRefreshLayout_import.setRefreshing(false);
                            card_export_normal.setVisibility(View.GONE);
                            int mode= SPUtil.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                                    ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
                            setRecyclerViewLayoutManagers(recyclerView_export,mode);
                            setRecyclerViewLayoutManagers(recyclerView_import,mode);
                            recyclerView_export.setAdapter(new ListAdapter<>(appItems,mode,list_export_operation_callback));
                            recyclerView_import.setAdapter(new ListAdapter<>(importItems,mode,listAdapterOperationListener_import));
                        }
                    });
                    searchTask.start();
                    return true;
                }
            });
        }catch (Exception e){e.printStackTrace();}

        viewPager.setAdapter(new MyPagerAdapter());
        tabLayout.setupWithViewPager(viewPager,true);
        viewPager.addOnPageChangeListener(this);

        try{
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Constants.ACTION_REFRESH_APP_LIST);
            intentFilter.addAction(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST);
            intentFilter.addAction(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE);
            registerReceiver(refresh_status_receiver,intentFilter);
        }catch (Exception e){e.printStackTrace();}

        try{
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addDataScheme("package");
            registerReceiver(package_status_changed_receiver,intentFilter);
        }catch (Exception e){e.printStackTrace();}

        if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private MyPagerAdapter getMyPagerAdapter(){
        return (MyPagerAdapter)viewPager.getAdapter();
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {}

    @Override
    public void onPageSelected(int i) {
        this.currentSelection=i;
        try{
            ListAdapter adapter=getMyPagerAdapter().getRecyclerViewListAdapter(i);
            if(adapter!=null)isCurrentPageMultiSelectMode=adapter.isMultiSelectMode();
            else isCurrentPageMultiSelectMode=false;
        }catch (Exception e){e.printStackTrace();}
        if(isSearchMode)return;
        setBackButtonVisible(isCurrentPageMultiSelectMode);
    }

    @Override
    public void onPageScrollStateChanged(int i) {}

    @Override
    public void onClick(View v){
        switch (v.getId()){
            default:break;
            case R.id.main_select_all:{
                try{
                    getMyPagerAdapter().getRecyclerViewListAdapter(0).setSelectAll(true);
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case R.id.main_deselect_all:{
                try{
                    getMyPagerAdapter().getRecyclerViewListAdapter(0).setSelectAll(false);
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case R.id.main_export:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(this);
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }

                final ArrayList<AppItem>arrayList=new ArrayList<>();
                try{
                    ListAdapter adapter=(ListAdapter)recyclerView_export.getAdapter();
                    arrayList.addAll(adapter.getSelectedItems());
                    adapter.closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(this, arrayList, true,new Global.ExportTaskFinishedListener() {
                    @Override
                    public void onFinished(@NonNull String error_message) {
                        if(!error_message.trim().equals("")){
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(getResources().getString(R.string.exception_title))
                                    .setMessage(getResources().getString(R.string.exception_message)+error_message)
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                            return;
                        }
                        ToastManager.showToast(MainActivity.this,getResources().getString(R.string.toast_export_complete)+ " "
                                +SPUtil.getDisplayingExportPath(MainActivity.this),Toast.LENGTH_SHORT);
                        refreshAvailableStorage();
                    }
                });
            }
            break;
            case R.id.main_share:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(this);
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }

                final ArrayList<AppItem>arrayList=new ArrayList<>();
                try{
                    ListAdapter adapter=(ListAdapter)recyclerView_export.getAdapter();
                    arrayList.addAll(adapter.getSelectedItems());
                    adapter.closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
                Global.shareCertainAppsByItems(this,arrayList);
            }
            break;
            case R.id.import_select_all:{
                try{
                    getMyPagerAdapter().getRecyclerViewListAdapter(1).setSelectOrDeselectAll();
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case R.id.import_delete:{
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.dialog_import_delete_title))
                        .setMessage(getResources().getString(R.string.dialog_import_delete_message))
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                                    Global.showRequestingWritePermissionSnackBar(MainActivity.this);
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                                    return;
                                }
                                try{
                                    ListAdapter adapter=getMyPagerAdapter().getRecyclerViewListAdapter(1);
                                    List<ImportItem>importItems=adapter.getSelectedItems();
                                    for(ImportItem importItem:importItems){
                                        FileItem fileItem=importItem.getFileItem();
                                        try{
                                            fileItem.delete();
                                        }catch (Exception e){e.printStackTrace();}
                                    }
                                    adapter.closeMultiSelectMode();
                                    sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                                    sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
                                }catch (Exception e){
                                    e.printStackTrace();
                                    ToastManager.showToast(MainActivity.this,e.toString(),Toast.LENGTH_SHORT);
                                }
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
            case R.id.import_action:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(this);
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }
                ArrayList<ImportItem>arrayList=new ArrayList<>();
                try{
                    ListAdapter adapter=getMyPagerAdapter().getRecyclerViewListAdapter(1);
                    arrayList.addAll(adapter.getSelectedItems());
                    for(ImportItem importItem:arrayList){
                        if(importItem.getImportType()== ImportItem.ImportType.APK){
                            new AlertDialog.Builder(this)
                                    .setTitle(getResources().getString(R.string.dialog_import_contains_apk_title))
                                    .setMessage(getResources().getString(R.string.dialog_import_contains_apk_message))
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                            return;
                        }
                    }

                    Global.showImportingDataObbDialog(this, arrayList, new Global.ImportTaskFinishedCallback() {
                        @Override
                        public void onImportFinished(String error_message) {
                            if(!error_message.trim().equals("")){
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(getResources().getString(R.string.dialog_import_finished_error_title))
                                        .setMessage(getResources().getString(R.string.dialog_import_finished_error_message)+error_message)
                                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {}
                                        })
                                        .show();
                            }else{
                                ToastManager.showToast(MainActivity.this,getResources().getString(R.string.toast_import_complete),Toast.LENGTH_SHORT);
                            }
                        }
                    });
                    adapter.closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case R.id.import_share:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(this);
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }
                ArrayList<ImportItem>arrayList=new ArrayList<>();
                try{
                    ListAdapter adapter=getMyPagerAdapter().getRecyclerViewListAdapter(1);
                    arrayList.addAll(adapter.getSelectedItems());
                    Global.shareImportItems(this,arrayList);
                    adapter.closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
            }
            break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView.getId()==R.id.main_show_system_app){
            //Log.e("checked",""+isChecked);
            buttonView.setEnabled(false);
            SharedPreferences.Editor editor=settings.edit();
            editor.putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,isChecked);
            editor.apply();
            new RefreshInstalledListTask(this,isChecked,refreshInstalledListTaskCallback).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0){
            if(grantResults.length==0)return;
            if(grantResults[0]==PermissionChecker.PERMISSION_GRANTED){
                //ToastManager.showToast(this,getResources().getString(R.string.permission_denied_att),Toast.LENGTH_SHORT);
                sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu=menu;
        //setIconEnable(menu,true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if(id==android.R.id.home){
            checkAndExit();
        }

        if(id==R.id.action_search){
            try{
                if(recyclerView_export.getAdapter()==null||recyclerView_import.getAdapter()==null){
                    ToastManager.showToast(this,getResources().getString(R.string.toast_wait_list_loading),Toast.LENGTH_SHORT);
                    return false;
                }
            }catch (Exception e){e.printStackTrace();}
            openSearchMode();
        }

        if(id==R.id.action_view){
            try{
                if(recyclerView_export.getAdapter()==null||recyclerView_import.getAdapter()==null){
                    ToastManager.showToast(this,getResources().getString(R.string.toast_wait_list_loading),Toast.LENGTH_SHORT);
                    return false;
                }
            }catch (Exception e){e.printStackTrace();}
            SharedPreferences settings= SPUtil.getGlobalSharedPreferences(this);
            int mode=settings.getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,mode==0?1:0);
            editor.apply();
            if(isSearchMode)return false;
            resetRecyclerViewsWithDataAndAdapter();
        }

        if(id==R.id.action_sort){
            if(currentSelection==0){
                try{
                    if(recyclerView_export.getAdapter()==null){
                        ToastManager.showToast(this,getResources().getString(R.string.toast_wait_list_loading),Toast.LENGTH_SHORT);
                        return false;
                    }
                }catch (Exception e){e.printStackTrace();}
                AppItemSortConfigDialog appItemSortConfigDialog=new AppItemSortConfigDialog(this, new SortConfigDialogCallback() {
                    @Override
                    public void onOptionSelected(int value) {
                        recyclerView_export.setAdapter(null);
                        new RefreshInstalledListTask(MainActivity.this,cb_show_sys_app.isChecked(),refreshInstalledListTaskCallback).start();
                    }
                });
                appItemSortConfigDialog.show();
            }else if(currentSelection==1){
                try{
                    if(recyclerView_import.getAdapter()==null){
                        ToastManager.showToast(this,getResources().getString(R.string.toast_wait_list_loading),Toast.LENGTH_SHORT);
                        return false;
                    }
                }catch (Exception e){e.printStackTrace();}
                ImportItemSortConfigDialog importItemSortConfigDialog=new ImportItemSortConfigDialog(this, new SortConfigDialogCallback() {
                    @Override
                    public void onOptionSelected(int value) {
                        new RefreshImportListTask(MainActivity.this,refreshImportListTaskCallback).start();
                    }
                });
                importItemSortConfigDialog.show();
            }


        }

        if(id==R.id.nav_receive){
            startActivity(new Intent(this,FileReceiveActivity.class));
        }
        if(id==R.id.nav_about){
            startActivity(new Intent(this,FileSendActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        drawerLayout.closeDrawers();
        switch (menuItem.getItemId()){
            default:break;
            case R.id.nav_receive:{
                startActivity(new Intent(this,FileReceiveActivity.class));
            }
            break;
            case R.id.nav_settings:{
                startActivityForResult(new Intent(this,SettingActivity.class),REQUEST_CODE_SETTINGS);
            }
            break;
            case R.id.nav_about:{
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
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkAndExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static final int REQUEST_CODE_SETTINGS=0;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            default:break;
            case REQUEST_CODE_SETTINGS:{
                if(resultCode==RESULT_OK){
                    recreate();
                }
            }
            break;
        }
    }

    @Override
    public void finish() {
        super.finish();
        try{
            unregisterReceiver(refresh_status_receiver);
        }catch (Exception e){e.printStackTrace();}
        try{
            unregisterReceiver(package_status_changed_receiver);
        }catch (Exception e){e.printStackTrace();}
    }

    private void openSearchMode(){
        isCurrentPageMultiSelectMode=false;
        isSearchMode=true;
        setBackButtonVisible(true);
        setMenuVisible(false);
        searchView.setQuery("",false);
        searchView.requestFocus();
        setActionbarDisplayCustomView(true);
        setBackButtonVisible(true);
        recyclerView_export.setAdapter(null);
        recyclerView_import.setAdapter(null);
        swipeRefreshLayout_import.setEnabled(false);
        swipeRefreshLayout_export.setEnabled(false);
        card_export_normal.setVisibility(View.GONE);
        card_export_multi_select.setVisibility(View.GONE);
        card_import_multi_select.setVisibility(View.GONE);
        inputMethodManager.showSoftInput(searchView.findFocus(),0);
    }

    private void closeSearchMode(){
        isSearchMode=false;
        setBackButtonVisible(false);
        setMenuVisible(true);
        resetRecyclerViewsWithDataAndAdapter();
        setActionbarDisplayCustomView(false);
        hideInputMethod();
    }

    private void setMenuVisible(boolean b){
        if(menu==null)return;
        for(int i=0;i<menu.size();i++){
            menu.getItem(i).setVisible(b);
        }
    }

    private void setRecyclerViewLayoutManagers(RecyclerView recyclerView,int mode){
        if(mode==1){
            GridLayoutManager gridLayoutManager=new GridLayoutManager(MainActivity.this,4);
            recyclerView.setLayoutManager(gridLayoutManager);
        }else{
            LinearLayoutManager linearLayoutManager=new LinearLayoutManager(MainActivity.this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
        }
    }


    private void refreshAvailableStorage(){
        try{
            String head=getResources().getString(R.string.main_card_remaining_storage)+":";
            boolean isExternal=SPUtil.getIsSaved2ExternalStorage(this);
            if(isExternal){
                long availible=0;
                if(Build.VERSION.SDK_INT>=19){
                    File[]files=getExternalFilesDirs(null);
                    for(File file:files){
                        if (file.getAbsolutePath().toLowerCase().startsWith(StorageUtil.getMainExternalStoragePath()))continue;
                        availible=StorageUtil.getAvaliableSizeOfPath(file.getAbsolutePath());
                    }
                }
                head+=Formatter.formatFileSize(this,availible);
            }else {
                head+=Formatter.formatFileSize(this, StorageUtil.getAvaliableSizeOfPath(StorageUtil.getMainExternalStoragePath()));
            }

            tv_storage.setText(head);
        }catch (Exception e){e.printStackTrace();}
    }

    private void setBackButtonVisible(boolean b){
        try{
            //getSupportActionBar().setDisplayHomeAsUpEnabled(b);
        }catch (Exception e){e.printStackTrace();}
    }

    private void setActionbarDisplayCustomView(boolean b){
        try{
            getSupportActionBar().setDisplayShowCustomEnabled(b);
        }catch (Exception e){e.printStackTrace();}
    }

    private void hideInputMethod(){
        try{
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
        }catch (Exception e){e.printStackTrace();}
    }

    private void setViewVisibilityWithAnimation(View view,int visibility){
        if(visibility==View.GONE){
            view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.exit_300));
            view.setVisibility(View.GONE);
        }else if(visibility==View.VISIBLE){
            view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.entry_300));
            view.setVisibility(View.VISIBLE);
        }
    }

    private void resetRecyclerViewsWithDataAndAdapter(){
        int mode= SPUtil.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
        swipeRefreshLayout_export.setRefreshing(false);
        swipeRefreshLayout_import.setRefreshing(false);
        loading_area_export.setVisibility(View.GONE);
        setRecyclerViewLayoutManagers(recyclerView_export,mode);
        setRecyclerViewLayoutManagers(recyclerView_import,mode);
        recyclerView_export.setAdapter(new ListAdapter<>(Global.app_list,mode,list_export_operation_callback));
        recyclerView_import.setAdapter(new ListAdapter<>(Global.item_list,mode,listAdapterOperationListener_import));
        card_export_normal.setVisibility(View.VISIBLE);
        card_import_multi_select.setVisibility(View.GONE);
        card_export_multi_select.setVisibility(View.GONE);
        swipeRefreshLayout_export.setEnabled(true);
        swipeRefreshLayout_import.setEnabled(true);
        setBackButtonVisible(false);
    }

    private void checkAndExit(){
        if(drawerLayout.isDrawerOpen(Gravity.START)){
            drawerLayout.closeDrawers();
            return;
        }
        if(isSearchMode){
            if(isCurrentPageMultiSelectMode){
                getMyPagerAdapter().getRecyclerViewListAdapter(currentSelection).closeMultiSelectMode();
                isCurrentPageMultiSelectMode=false;
                return;
            }
            closeSearchMode();
            return;
        }
        if(isCurrentPageMultiSelectMode) {
            try{
                getMyPagerAdapter().getRecyclerViewListAdapter(currentSelection).closeMultiSelectMode();
            }catch (Exception e){e.printStackTrace();}
            isCurrentPageMultiSelectMode=false;
            return;
        }
        finish();
    }

    private class MyPagerAdapter extends PagerAdapter{

        private final View[] pageViews=new View[2];

        MyPagerAdapter(){}
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            if(pageViews[position]==null){
                switch (position){
                    default:break;
                    case 0:{
                        pageViews[position]=LayoutInflater.from(MainActivity.this).inflate(R.layout.page_export,container,false);
                        View view=pageViews[position];
                        swipeRefreshLayout_export=view.findViewById(R.id.content_swipe);
                        //final RecyclerView recyclerView=view.findViewById(R.id.content_recyclerview);
                        recyclerView_export=view.findViewById(R.id.content_recyclerview);
                        loading_area_export=view.findViewById(R.id.loading);
                        pg_export=view.findViewById(R.id.loading_pg);
                        tv_export_progress=view.findViewById(R.id.loading_text);

                        boolean is_show_sys= SPUtil.getGlobalSharedPreferences(MainActivity.this).getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT);
                        cb_show_sys_app =view.findViewById(R.id.main_show_system_app);
                        cb_show_sys_app.setChecked(is_show_sys);
                        cb_show_sys_app.setOnCheckedChangeListener(MainActivity.this);
                        tv_storage=view.findViewById(R.id.main_storage_remain);
                        btn_export_select_all=view.findViewById(R.id.main_select_all);
                        btn_export_deselect_all=view.findViewById(R.id.main_deselect_all);
                        btn_export_action=view.findViewById(R.id.main_export);
                        btn_export_share=view.findViewById(R.id.main_share);

                        card_export_normal=view.findViewById(R.id.export_card);
                        card_export_multi_select=view.findViewById(R.id.export_card_multi_select);
                        tv_card_export_multi_select_title=view.findViewById(R.id.main_select_num_size);
                        no_content_att_export=view.findViewById(R.id.no_content_att);

                        btn_export_select_all.setOnClickListener(MainActivity.this);
                        btn_export_deselect_all.setOnClickListener(MainActivity.this);
                        btn_export_action.setOnClickListener(MainActivity.this);
                        btn_export_share.setOnClickListener(MainActivity.this);

                        swipeRefreshLayout_export.setColorSchemeColors(getResources().getColor(R.color.colorTitle));
                        swipeRefreshLayout_export.setRefreshing(true);

                        recyclerView_export.addOnScrollListener(onScrollListener_export);
                        loading_area_export.setVisibility(View.VISIBLE);
                        recyclerView_export.setAdapter(null);

                        new RefreshInstalledListTask(MainActivity.this, is_show_sys,refreshInstalledListTaskCallback).start();

                        swipeRefreshLayout_export.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                            @Override
                            public void onRefresh() {
                                if(isSearchMode)return;
                                try{
                                    if(((ListAdapter)recyclerView_export.getAdapter()).isMultiSelectMode())return;
                                }catch (Exception e){e.printStackTrace();}
                                new RefreshInstalledListTask(MainActivity.this,
                                        SPUtil.getGlobalSharedPreferences(MainActivity.this).getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT)
                                        ,refreshInstalledListTaskCallback).start();
                            }
                        });
                    }
                    break;
                    case 1:{
                        pageViews[position]=LayoutInflater.from(MainActivity.this).inflate(R.layout.page_import,container,false);
                        View view=pageViews[position];
                        swipeRefreshLayout_import=view.findViewById(R.id.content_swipe);
                        recyclerView_import=view.findViewById(R.id.content_recyclerview);
                        view.findViewById(R.id.loading).setVisibility(View.GONE);

                        card_import_multi_select=view.findViewById(R.id.import_card_multi_select);
                        tv_card_import_multi_select_title=view.findViewById(R.id.import_card_att);
                        btn_import_select_all=view.findViewById(R.id.import_select_all);

                        btn_import_delete=view.findViewById(R.id.import_delete);
                        btn_import_action=view.findViewById(R.id.import_action);
                        btn_import_share=view.findViewById(R.id.import_share);
                        no_content_att_import=view.findViewById(R.id.no_content_att);

                        btn_import_select_all.setOnClickListener(MainActivity.this);
                        btn_import_action.setOnClickListener(MainActivity.this);
                        btn_import_delete.setOnClickListener(MainActivity.this);
                        btn_import_share.setOnClickListener(MainActivity.this);

                        swipeRefreshLayout_import.setColorSchemeColors(getResources().getColor(R.color.colorTitle));

                        recyclerView_import.addOnScrollListener(onScrollListener_import);

                        new RefreshImportListTask(MainActivity.this, refreshImportListTaskCallback).start();

                        swipeRefreshLayout_import.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                            @Override
                            public void onRefresh() {
                                try{
                                    ListAdapter adapter=(ListAdapter)recyclerView_import.getAdapter();
                                    if(adapter.isMultiSelectMode()||isSearchMode)return;
                                }catch (Exception e){e.printStackTrace();}
                                new RefreshImportListTask(MainActivity.this,refreshImportListTaskCallback).start();
                            }
                        });
                    }
                    break;
                }
            }
            container.addView(pageViews[position]);
            return pageViews[position];
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View)object);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                default:break;
                case 0:return getResources().getString(R.string.main_page_export);
                case 1:return getResources().getString(R.string.main_page_import);
            }
            return "";
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view==o;
        }

        ListAdapter getRecyclerViewListAdapter(int position){
            return (ListAdapter)((RecyclerView)pageViews[position].findViewById(R.id.content_recyclerview)).getAdapter();
        }
    }

    interface ListAdapterOperationListener{
        void onItemClicked(DisplayItem displayItem,ViewHolder viewHolder);
        void onMultiSelectItemChanged(List<DisplayItem>selected_items,long length);
        void onMultiSelectModeClosed();
        void onItemLongClicked();
    }

    private class ListAdapter<T extends DisplayItem> extends RecyclerView.Adapter<ViewHolder>{

        private final ArrayList<DisplayItem>list=new ArrayList<>();
        private boolean[] isSelected;
        private boolean isMultiSelectMode=false;
        private int mode=0;

        private ListAdapterOperationListener listener;

        private ListAdapter(@NonNull List<T> list, int mode,ListAdapterOperationListener listener){
            this.list.addAll(list);
            this.listener=listener;
            isSelected=new boolean[list.size()];
            if(mode==1)this.mode=1;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(MainActivity.this).inflate(mode==0?R.layout.item_app_info_linear
                    :R.layout.item_app_info_grid,viewGroup,false),mode);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
            try{
                final DisplayItem item=list.get(viewHolder.getAdapterPosition());
                viewHolder.title.setText(String.valueOf(item.getTitle()));
                viewHolder.title.setTextColor(getResources().getColor((item.isRedMarked()?
                        R.color.colorSystemAppTitleColor:R.color.colorHighLightText)));
                viewHolder.icon.setImageDrawable(item.getIconDrawable());
                if(mode==0){
                    viewHolder.description.setText(String.valueOf(item.getDescription()));
                    viewHolder.right.setText(Formatter.formatFileSize(MainActivity.this,item.getSize()));
                    viewHolder.cb.setChecked(isSelected[viewHolder.getAdapterPosition()]);
                    viewHolder.right.setVisibility(isMultiSelectMode?View.GONE:View.VISIBLE);
                    viewHolder.cb.setVisibility(isMultiSelectMode?View.VISIBLE:View.GONE);
                }else if(mode==1){
                    if(isMultiSelectMode)viewHolder.root.setBackgroundColor(getResources().getColor(isSelected[viewHolder.getAdapterPosition()]
                            ?R.color.colorSelectedBackground
                            :R.color.colorCardArea));
                    else viewHolder.root.setBackgroundColor(getResources().getColor(R.color.colorCardArea));
                }

                viewHolder.root.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(isMultiSelectMode){
                            isSelected[viewHolder.getAdapterPosition()]=!isSelected[viewHolder.getAdapterPosition()];
                            refreshButtonStatus();
                            notifyItemChanged(viewHolder.getAdapterPosition());
                        }else{
                            if(listener!=null)listener.onItemClicked(item,viewHolder);
                        }

                    }
                });
                viewHolder.root.setOnLongClickListener(isMultiSelectMode?null:new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        isSelected=new boolean[list.size()];
                        isSelected[viewHolder.getAdapterPosition()]=true;
                        isMultiSelectMode=true;
                        refreshButtonStatus();
                        notifyDataSetChanged();
                        if(listener!=null)listener.onItemLongClicked();
                        return true;
                    }
                });

            }catch (Exception e){e.printStackTrace();}
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        void closeMultiSelectMode(){
            isMultiSelectMode=false;
            notifyDataSetChanged();
            if(listener!=null)listener.onMultiSelectModeClosed();
        }

        private boolean isMultiSelectMode(){
            return isMultiSelectMode;
        }

        void setSelectAll(boolean selected){
            if(!isMultiSelectMode||isSelected==null)return;
            for(int i=0;i<isSelected.length;i++)isSelected[i]=selected;
            refreshButtonStatus();
            notifyDataSetChanged();
        }

        void setSelectOrDeselectAll(){
            if(!isMultiSelectMode||isSelected==null)return;
            boolean flag=false;
            for(int i=0;i<isSelected.length;i++){
                if(!isSelected[i]){
                    flag=true;
                    break;
                }
            }
            for(int i=0;i<isSelected.length;i++)isSelected[i]=flag;
            refreshButtonStatus();
            notifyDataSetChanged();
        }

        private void refreshButtonStatus(){
            if(listener!=null)listener.onMultiSelectItemChanged(getSelectedItems(),getSelectedFileLength());
        }

        /**
         * 返回的是item的原始项
         */
        private List<DisplayItem> getSelectedItems(){
            ArrayList<DisplayItem>list_selected=new ArrayList<>();
            if(!isMultiSelectMode)return list_selected;
            for(int i=0;i<list.size();i++){
                if(isSelected[i])list_selected.add(list.get(i));
            }
            return list_selected;
        }

        private int getSelectedNum(){
            int i=0;
            for(boolean b:isSelected)if(b)i++;
            return i;
        }

        private long getSelectedFileLength(){
            long length=0;
            for(int i=0;i<isSelected.length;i++){
                if(isSelected[i])length+=list.get(i).getSize();
            }
            return length;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView icon;
        TextView title;
        TextView description;
        TextView right;
        CheckBox cb;
        View root;
        public ViewHolder(@NonNull View itemView,int mode) {
            super(itemView);
            root=itemView.findViewById(R.id.item_app_root);
            icon=itemView.findViewById(R.id.item_app_icon);
            title=itemView.findViewById(R.id.item_app_title);
            if(mode==0){
                description=itemView.findViewById(R.id.item_app_description);
                right=itemView.findViewById(R.id.item_app_right);
                cb=itemView.findViewById(R.id.item_app_cb);
            }
        }
    }
}
