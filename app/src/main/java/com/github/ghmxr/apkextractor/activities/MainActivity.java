package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.tasks.ImportTask;
import com.github.ghmxr.apkextractor.tasks.RefreshImportListTask;
import com.github.ghmxr.apkextractor.tasks.RefreshInstalledListTask;
import com.github.ghmxr.apkextractor.ui.ImportingDataObbDialog;
import com.github.ghmxr.apkextractor.ui.ImportingDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.tasks.SearchTask;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener,ViewPager.OnPageChangeListener, CompoundButton.OnCheckedChangeListener {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private int currentSelection=0;
    private boolean isSearchMode=false;
    private SearchView searchView;
    private Menu menu;
    private InputMethodManager inputMethodManager;

    private SearchTask searchTask;

    private final ArrayList<AppItem>applist=new ArrayList<>();
    private final ArrayList<ImportItem>importList=new ArrayList<>();

    private RecyclerView recyclerView_export;
    private RecyclerView recyclerView_import;

    //private ListAdapter current_adapter;
    private boolean isCurrentPageMultiSelectMode=false;

    private CardView card_export_normal,card_export_multi_select;
    private TextView tv_card_export_multi_select_title,tv_card_import_multi_select_title;
    private CardView card_import_multi_select;

    private ViewGroup loading_area_export;
    private ProgressBar pg_export;
    private TextView tv_export_progress;

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
            swipeRefreshLayout_export.setRefreshing(false);
            int mode= SPUtil.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                    ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            ListAdapter adapter=new ListAdapter<>(appList,mode,list_export_operation_callback);
            setRecyclerViewLayoutManagers(recyclerView_export,mode);
            recyclerView_export.setAdapter(adapter);
            //adapter.setListAdapterOperationListener(list_export_operation_callback);
            //recyclerView_export.addOnScrollListener(onScrollListener_export);
            cb_show_sys_app.setEnabled(true);
            swipeRefreshLayout_export.setRefreshing(false);
            card_export_normal.setVisibility(View.VISIBLE);
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
        }

        @Override
        public void onMultiSelectModeClosed() {
            swipeRefreshLayout_export.setEnabled(true);
            card_export_multi_select.setVisibility(View.GONE);
            if(!isSearchMode)setViewVisibilityWithAnimation(card_export_normal,View.VISIBLE);
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
        public void onRefreshCompleted(List<ImportItem> list) {
            swipeRefreshLayout_import.setRefreshing(false);
            isScrollable_import=false;
            int mode= SPUtil.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                    ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            ListAdapter adapter=new ListAdapter<>(list,mode,listAdapterOperationListener_import);
            setRecyclerViewLayoutManagers(recyclerView_import,mode);
            recyclerView_import.setAdapter(adapter);
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

                return;
            }
            ArrayList<ImportItem>arrayList=new ArrayList<>();
            arrayList.add(item);
            if(item.getImportType()== ImportItem.ImportType.ZIP)
            new ImportingDataObbDialog(MainActivity.this, arrayList, new ImportingDataObbDialog.ImportDialogDataObbConfirmedCallback() {
                @Override
                public void onImportingDataObbConfirmed(@NonNull final List<ImportItem> importItems, @NonNull List<ZipFileUtil.ZipFileInfo>zipFileInfos) {
                    long total=0;
                    StringBuilder stringBuilder=new StringBuilder();
                    for(int i=0;i<importItems.size();i++){
                        if(importItems.get(i).importData){
                            total+=zipFileInfos.get(i).getDataSize();
                            stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage());
                        }
                        if(importItems.get(i).importObb){
                            total+=zipFileInfos.get(i).getObbSize();
                            stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage());
                        }
                        if(importItems.get(i).importApk){
                            total+=zipFileInfos.get(i).getApkSize();
                            stringBuilder.append(zipFileInfos.get(i).getAlreadyExistingFilesInfoInMainStorage());
                        }

                    }
                    final long total_1=total;
                    if(stringBuilder.length()>0){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("已存在的内容")
                                .setMessage("主存储和导出路径存在下列重复文件\n\n"+stringBuilder.toString())
                                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final ImportingDialog dialog1=new ImportingDialog(MainActivity.this,total_1);
                                        final ImportTask importTask=new ImportTask(MainActivity.this, importItems, new ImportTask.ImportTaskCallback() {
                                            @Override
                                            public void onImportTaskStarted() {

                                            }

                                            @Override
                                            public void onRefreshSpeed(long speed) {
                                                dialog1.setSpeed(speed);
                                            }

                                            @Override
                                            public void onImportTaskProgress(@NonNull String writePath, long progress) {
                                                dialog1.setCurrentWritingName(writePath);
                                                dialog1.setProgress(progress);
                                            }

                                            @Override
                                            public void onImportTaskFinished(@NonNull String errorMessage) {
                                                dialog1.cancel();
                                                ToastManager.showToast(MainActivity.this,"导入完成",Toast.LENGTH_SHORT);
                                            }
                                        });
                                        dialog1.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                importTask.setInterrupted();
                                            }
                                        });
                                        dialog1.show();
                                        importTask.start();
                                    }
                                })
                                .setNegativeButton(getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                    }else{
                        final ImportingDialog dialog1=new ImportingDialog(MainActivity.this,total_1);
                        final ImportTask importTask=new ImportTask(MainActivity.this, importItems, new ImportTask.ImportTaskCallback() {
                            @Override
                            public void onImportTaskStarted() {

                            }

                            @Override
                            public void onRefreshSpeed(long speed) {
                                dialog1.setSpeed(speed);
                            }

                            @Override
                            public void onImportTaskProgress(@NonNull String writePath, long progress) {
                                dialog1.setCurrentWritingName(writePath);
                                dialog1.setProgress(progress);
                            }

                            @Override
                            public void onImportTaskFinished(@NonNull String errorMessage) {
                                dialog1.cancel();
                                ToastManager.showToast(MainActivity.this,"导入完成",Toast.LENGTH_SHORT);
                            }
                        });
                        dialog1.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                importTask.setInterrupted();
                            }
                        });
                        dialog1.show();
                        importTask.start();
                    }
                }
            }).show();
            else ;

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
        }

        @Override
        public void onMultiSelectModeClosed() {
            swipeRefreshLayout_import.setEnabled(true);
            card_import_multi_select.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                            //ListAdapter adapter_export=;
                            //ListAdapter adapter_import=;
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

       /* final SharedPreferences settings=Global.getGlobalSharedPreferences(this);
        final CheckBox cb_show_sys=findViewById(R.id.main_show_system_app);
        cb_show_sys.setChecked(settings.getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT));
        cb_show_sys.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cb_show_sys.setEnabled(false);
                SharedPreferences.Editor editor=settings.edit();
                editor.putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,isChecked);
                editor.apply();
                refreshList();
            }
        });*/


        //main_select_all.setOnClickListener(this);

        //main_deselect_all.setOnClickListener(this);

        //main_export.setOnClickListener(this);

        //main_share.setOnClickListener(this);

        //refreshList();
        viewPager.setAdapter(new MyPagerAdapter());
        tabLayout.setupWithViewPager(viewPager,true);
        viewPager.addOnPageChangeListener(this);

        if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        refreshAvailableStorage();
    }

    private MyPagerAdapter getMyPagerAdapter(){
        return (MyPagerAdapter)viewPager.getAdapter();
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {}

    @Override
    public void onPageSelected(int i) {
        this.currentSelection=i;
        //if(isSearchMode)return;
        try{
            ListAdapter adapter=getMyPagerAdapter().getRecyclerViewListAdapter(i);
            if(adapter!=null)isCurrentPageMultiSelectMode=adapter.isMultiSelectMode();
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
                //bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                //bottom_card_multi_select.setVisibility(View.GONE);
                final ArrayList<AppItem>arrayList=new ArrayList<>();
                try{
                    arrayList.addAll(((ListAdapter)recyclerView_export.getAdapter()).getSelectedItems());
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
                        ToastManager.showToast(MainActivity.this,getResources().getString(R.string.toast_export_complete)+ SPUtil.getInternalSavePath(MainActivity.this),Toast.LENGTH_SHORT);
                        refreshAvailableStorage();
                    }
                });
                try{
                    //((ListAdapter)recyclerView.getAdapter()).closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
                closeMultiSelectModeForExternalVariables(true);
            }
            break;
            case R.id.main_share:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(this);
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }
                //bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                //bottom_card_multi_select.setVisibility(View.GONE);
                final ArrayList<AppItem>arrayList=new ArrayList<>();
                try{
                    //arrayList.addAll(((ListAdapter)recyclerView.getAdapter()).getSelectedAppItems());
                }catch (Exception e){e.printStackTrace();}
                Global.shareCertainAppsByItems(this,arrayList);
                try{
                    //((ListAdapter)recyclerView.getAdapter()).closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
                closeMultiSelectModeForExternalVariables(true);
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
            if(grantResults[0]!=PermissionChecker.PERMISSION_GRANTED){
                ToastManager.showToast(this,getResources().getString(R.string.permission_denied_att),Toast.LENGTH_SHORT);
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
        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(this,SettingActivity.class),REQUEST_CODE_SETTINGS);
            return true;
        }

        if(id==android.R.id.home){
            checkAndExit();
        }

        if(id==R.id.action_search){
            openSearchMode();
        }

        if(id==R.id.action_view){
            SharedPreferences settings= SPUtil.getGlobalSharedPreferences(this);
            int mode=settings.getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,mode==0?1:0);
            editor.apply();
            if(isSearchMode)return false;
            resetRecyclerViewsWithDataAndAdapter();
        }

        if(id==R.id.action_sort){
            final SharedPreferences settings= SPUtil.getGlobalSharedPreferences(this);
            final SharedPreferences.Editor editor=settings.edit();
            int sort=settings.getInt(Constants.PREFERENCE_SORT_CONFIG,0);
            View dialogView=LayoutInflater.from(this).inflate(R.layout.dialog_sort,null);
            RadioButton ra_default=dialogView.findViewById(R.id.sort_ra_default);
            RadioButton ra_name_ascend=dialogView.findViewById(R.id.sort_ra_ascending_appname);
            RadioButton ra_name_descend=dialogView.findViewById(R.id.sort_ra_descending_appname);
            RadioButton ra_size_ascend=dialogView.findViewById(R.id.sort_ra_ascending_appsize);
            RadioButton ra_size_descend=dialogView.findViewById(R.id.sort_ra_descending_appsize);
            RadioButton ra_update_time_ascend=dialogView.findViewById(R.id.sort_ra_ascending_date);
            RadioButton ra_update_time_descend=dialogView.findViewById(R.id.sort_ra_descending_date);
            RadioButton ra_install_time_ascend=dialogView.findViewById(R.id.sort_ra_ascending_install_date);
            RadioButton ra_install_time_descend=dialogView.findViewById(R.id.sort_ra_descending_install_date);
            ra_default.setChecked(sort==0);
            ra_name_ascend.setChecked(sort==1);
            ra_name_descend.setChecked(sort==2);
            ra_size_ascend.setChecked(sort==3);
            ra_size_descend.setChecked(sort==4);
            ra_update_time_ascend.setChecked(sort==5);
            ra_update_time_descend.setChecked(sort==6);
            ra_install_time_ascend.setChecked(sort==7);
            ra_install_time_descend.setChecked(sort==8);
            final AlertDialog dialog=new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.action_sort))
                    .setView(dialogView)
                    .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {}
                    })
                    .show();
            ra_default.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,0);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_name_ascend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,1);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_name_descend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,2);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_size_ascend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,3);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_size_descend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,4);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_update_time_ascend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,5);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_update_time_descend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,6);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_install_time_ascend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,7);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });
            ra_install_time_descend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putInt(Constants.PREFERENCE_SORT_CONFIG,8);
                    editor.apply();
                    dialog.cancel();
                    refreshList();
                }
            });

        }


        return super.onOptionsItemSelected(item);
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

    void refreshList(){}
    void closeMultiSelectModeForExternalVariables(boolean b){}

    private void openSearchMode(){
        isCurrentPageMultiSelectMode=false;
        isSearchMode=true;
        setBackButtonVisible(true);
        setMenuVisible(false);
        searchView.setQuery("",false);
        searchView.requestFocus();
        inputMethodManager.showSoftInput(searchView.findFocus(),0);
        setActionbarDisplayCustomView(true);
        setBackButtonVisible(true);
        recyclerView_export.setAdapter(null);
        recyclerView_import.setAdapter(null);
        swipeRefreshLayout_import.setEnabled(false);
        swipeRefreshLayout_export.setEnabled(false);
        card_export_normal.setVisibility(View.GONE);
        card_export_multi_select.setVisibility(View.GONE);
        card_import_multi_select.setVisibility(View.GONE);
    }

    private void closeSearchMode(){
        isSearchMode=false;
        setBackButtonVisible(false);
        setMenuVisible(true);
        resetRecyclerViewsWithDataAndAdapter();
        setActionbarDisplayCustomView(false);
        //setBackButtonVisible(false);
        try{
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
        }catch (Exception e){e.printStackTrace();}
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
        /*((TextView)findViewById(R.id.main_storage_remain)).setText(getResources().getString(R.string.main_card_remaining_storage)+":"+
                Formatter.formatFileSize(this, Storage.getAvaliableSizeOfPath(Global.getInternalSavePath(this))));*/
    }

    private void setBackButtonVisible(boolean b){
        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(b);
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
            setBackButtonVisible(false);
            isCurrentPageMultiSelectMode=false;
            return;
        }
        finish();
    }

    private void changeViewStates(int mode){
        if(mode==1){
            GridLayoutManager gridLayoutManager=new GridLayoutManager(MainActivity.this,4);
            recyclerView_export.setLayoutManager(gridLayoutManager);
        }else{

        }
    }

    private class MyPagerAdapter extends PagerAdapter{
        private List<AppItem>list_export;
        private List<ImportItem>list_import;
        private final View[] pageViews=new View[2];
        private boolean isScrollable_export=false;
        private boolean isScrollable_import=false;
        MyPagerAdapter(){
            //this.list_export=list_export;
            //this.list_import=list_import;
        }
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
                        view.findViewById(R.id.main_select_all).setOnClickListener(MainActivity.this);
                        view.findViewById(R.id.main_deselect_all).setOnClickListener(MainActivity.this);
                        view.findViewById(R.id.main_export).setOnClickListener(MainActivity.this);
                        view.findViewById(R.id.main_share).setOnClickListener(MainActivity.this);
                        card_export_normal=view.findViewById(R.id.export_card);
                        card_export_multi_select=view.findViewById(R.id.export_card_multi_select);
                        tv_card_export_multi_select_title=view.findViewById(R.id.main_select_num_size);

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
                        //final ProgressBar progressBar=view.findViewById(R.id.loading_pg);
                        //final TextView progress_att=view.findViewById(R.id.loading_text);
                        card_import_multi_select=view.findViewById(R.id.import_card_multi_select);
                        tv_card_import_multi_select_title=view.findViewById(R.id.import_card_att);
                        view.findViewById(R.id.import_select_all).setOnClickListener(MainActivity.this);
                        view.findViewById(R.id.import_deselect_all).setOnClickListener(MainActivity.this);
                        view.findViewById(R.id.import_delete).setOnClickListener(MainActivity.this);
                        view.findViewById(R.id.import_action).setOnClickListener(MainActivity.this);
                        swipeRefreshLayout_import.setColorSchemeColors(getResources().getColor(R.color.colorTitle));
                        swipeRefreshLayout_import.setRefreshing(true);
                        //recyclerView.removeOnScrollListener(onScrollListener);
                        recyclerView_import.addOnScrollListener(onScrollListener_import);
                        swipeRefreshLayout_import.setRefreshing(false);

                        new RefreshImportListTask(MainActivity.this, refreshImportListTaskCallback).start();

                        swipeRefreshLayout_import.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                            @Override
                            public void onRefresh() {
                                try{
                                    ListAdapter adapter=(ListAdapter)recyclerView_import.getAdapter();
                                    if(adapter.isMultiSelectMode()||isSearchMode)return;
                                }catch (Exception e){e.printStackTrace();}
                                recyclerView_import.setAdapter(null);
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
            //super.destroyItem(container, position, object);
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

        View getPageView(int position){
            return pageViews[position];
        }

        RecyclerView getRecyclerView(int position){
            return getPageView(position).findViewById(R.id.content_recyclerview);
        }

        ListAdapter getRecyclerViewListAdapter(int position){
            return (ListAdapter)((RecyclerView)pageViews[position].findViewById(R.id.content_recyclerview)).getAdapter();
        }

        boolean isMultiSelectMode(int position){
            try{
                return ((ListAdapter)((RecyclerView)pageViews[position].findViewById(R.id.content_recyclerview)).getAdapter()).isMultiSelectMode();
            }catch (Exception e){e.printStackTrace();}
            return false;
        }

        void notifyIsSwipeable(boolean b){
            for(View view:pageViews){
                try{
                    ((SwipeRefreshLayout)view.findViewById(R.id.content_swipe)).setEnabled(b);
                }catch (Exception e){e.printStackTrace();}
            }
        }


    }

    interface ListAdapterOperationListener{
        void onItemClicked(DisplayItem displayItem,ViewHolder viewHolder);
        void onMultiSelectItemChanged(List<DisplayItem>selected_items,long length);
        void onMultiSelectModeClosed();
        void onItemLongClicked();
    }

    private class ListAdapter<T extends DisplayItem> extends RecyclerView.Adapter<ViewHolder>{

        static final int MODE_LINEAR=0;
        static final int MODE_GRID=1;

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

        /*void setListAdapterOperationListener(ListAdapterOperationListener listener){
            this.listener=listener;
        }*/

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
                            /*Intent intent=new Intent(MainActivity.this,AppDetailActivity.class);
                            //intent.putExtra(EXTRA_PARCELED_APP_ITEM,item);
                            intent.putExtra(EXTRA_PACKAGE_NAME,item.getPackageName());
                            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this,new Pair<View, String>(viewHolder.icon,"icon"));
                            try{
                                ActivityCompat.startActivity(MainActivity.this, intent, compat.toBundle());
                            }catch (Exception e){e.printStackTrace();}*/
                            if(listener!=null)listener.onItemClicked(item,viewHolder);
                        }

                    }
                });
                viewHolder.root.setOnLongClickListener(isMultiSelectMode?null:new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ///swipeRefreshLayout.setEnabled(false);
                        isSelected=new boolean[list.size()];
                        isSelected[viewHolder.getAdapterPosition()]=true;
                        isMultiSelectMode=true;
                        refreshButtonStatus();
                        notifyDataSetChanged();
                        /*try{
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }catch (Exception e){e.printStackTrace();}
                        try{
                            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
                        }catch (Exception e){e.printStackTrace();}*/
                        /*bottom_card.setVisibility(View.GONE);
                        bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.entry_300));
                        bottom_card_multi_select.setVisibility(View.VISIBLE);*/
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
            //main_export.setEnabled(selected);
           // main_share.setEnabled(selected);
            refreshButtonStatus();
            notifyDataSetChanged();
        }

        private void refreshButtonStatus(){
            //main_export.setEnabled(getSelectedNum()>0);
            //main_share.setEnabled(getSelectedNum()>0);
            //((TextView)findViewById(R.id.main_select_num_size)).setText(getSelectedNum()+getResources().getString(R.string.unit_item)+"/"+Formatter.formatFileSize(MainActivity.this,getSelectedFileLength()));
            /*main_export.setText(getResources().getString(R.string.main_card_multi_select_export)+"("
                    +getSelectedNum()+getResources().getString(R.string.unit_item)+"/"
                    +Formatter.formatFileSize(MainActivity.this,getSelectedFileLength())+")");
            main_share.setText(getResources().getString(R.string.main_card_multi_select_share)+"("+getSelectedNum()
                    +getResources().getString(R.string.unit_item)+")");*/
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
