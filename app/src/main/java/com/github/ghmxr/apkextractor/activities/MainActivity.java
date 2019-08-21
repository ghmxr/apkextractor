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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.AppItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.Constants;
import com.github.ghmxr.apkextractor.ui.LoadingListDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.SearchTask;
import com.github.ghmxr.apkextractor.utils.Storage;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener{

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CardView bottom_card;
    private CardView bottom_card_multi_select;
    private boolean isScrollable=false;
    private boolean isSearchMode=false;
    private SearchView searchView;
    private Menu menu;
    private InputMethodManager inputMethodManager;

    private Button main_select_all,main_deselect_all,main_export,main_share;

    //private ListAdapter adapter;
    private SearchTask searchTask;
    private ProgressBar progressBar;

    final RecyclerView.OnScrollListener onScrollListener=new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            //recyclerView.getScrollState()
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if(isSearchMode)return;
            if(bottom_card==null)return;
            if(bottom_card_multi_select==null)return;
            boolean isMultiSelectMode=false;
            try{
                isMultiSelectMode=((ListAdapter)recyclerView.getAdapter()).getIsMultiSelectMode();
            }catch (Exception e){e.printStackTrace();}
            if (!recyclerView.canScrollVertically(-1)) {
               // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
               // onScrolledToBottom();
                if(isMultiSelectMode){
                    if(isScrollable&&bottom_card_multi_select.getVisibility()!= View.GONE){
                        bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                        bottom_card_multi_select.setVisibility(View.GONE);
                    }
                }else {
                    if(isScrollable&&bottom_card.getVisibility()!=View.GONE&&!isSearchMode){
                        bottom_card.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.exit_300));
                        bottom_card.setVisibility(View.GONE);
                    }
                }

            } else if (dy < 0) {
               // onScrolledUp();
                if(isMultiSelectMode){
                    if(isScrollable&&bottom_card_multi_select.getVisibility()!=View.VISIBLE){
                        bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.entry_300));
                        bottom_card_multi_select.setVisibility(View.VISIBLE);
                    }
                }else {
                    if(isScrollable&&bottom_card.getVisibility()!=View.VISIBLE&&!isSearchMode){
                        bottom_card.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.entry_300));
                        bottom_card.setVisibility(View.VISIBLE);
                    }
                }

            } else if (dy > 0) {
               // onScrolledDown();
                isScrollable=true;
                if(isMultiSelectMode){
                    if(bottom_card_multi_select.getVisibility()!= View.GONE){
                        bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                        bottom_card_multi_select.setVisibility(View.GONE);
                    }
                }else{
                    if(bottom_card.getVisibility()!=View.GONE&&!isSearchMode){
                        bottom_card.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.exit_300));
                        bottom_card.setVisibility(View.GONE);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView=findViewById(R.id.main_recycler_view);
        bottom_card=findViewById(R.id.main_card);
        bottom_card_multi_select=findViewById(R.id.main_card_multi_select);

        main_select_all=findViewById(R.id.main_select_all);
        main_deselect_all=findViewById(R.id.main_deselect_all);
        main_export=findViewById(R.id.main_export);
        main_share=findViewById(R.id.main_share);

        swipeRefreshLayout=findViewById(R.id.main_swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorTitle));

        progressBar=findViewById(R.id.main_search_pg);
        inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

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
                    recyclerView.setAdapter(null);
                    bottom_card_multi_select.setVisibility(View.GONE);
                    List<AppItem>total_list=Global.list;
                    if(total_list==null)return false;
                    progressBar.setVisibility(View.VISIBLE);
                    searchTask=new SearchTask(total_list, newText, new SearchTask.SearchTaskCompletedCallback() {
                        @Override
                        public void onSearchTaskCompleted(@NonNull List<AppItem> result) {
                            progressBar.setVisibility(View.GONE);
                            //recyclerView.setAdapter(new ListAdapter(result,1));
                            ListAdapter adapter;
                            int mode=Global.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                                    ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
                            if(mode==1){
                                GridLayoutManager gridLayoutManager=new GridLayoutManager(MainActivity.this,4);
                                recyclerView.setLayoutManager(gridLayoutManager);
                                adapter=new ListAdapter(result,1);
                            }else{
                                LinearLayoutManager linearLayoutManager=new LinearLayoutManager(MainActivity.this);
                                linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                                recyclerView.setLayoutManager(linearLayoutManager);
                                adapter=new ListAdapter(result,0);
                            }

                            recyclerView.setAdapter(adapter);
                        }
                    });
                    searchTask.start();
                    return true;
                }
            });
        }catch (Exception e){e.printStackTrace();}

        final SharedPreferences settings=Global.getGlobalSharedPreferences(this);
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
        });


        main_select_all.setOnClickListener(this);

        main_deselect_all.setOnClickListener(this);

        main_export.setOnClickListener(this);

        main_share.setOnClickListener(this);

        refreshList();

        if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        refreshAvailableStorage();
    }

    private void refreshList(){
        isScrollable=false;
        swipeRefreshLayout.setRefreshing(true);
        closeMultiSelectModeForExternalVariables(false);
        recyclerView.setAdapter(null);

        final boolean is_show_sys=Global.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT);
        final CheckBox cb_show_sys=findViewById(R.id.main_show_system_app);
        final LoadingListDialog dialog=new LoadingListDialog(this);
        try{dialog.show();}catch (Exception e){e.printStackTrace();}
        new Global.RefreshInstalledListTask(this, is_show_sys, new Global.RefreshInstalledListTaskCallback() {
            @Override
            public void onRefreshProgressUpdated(int current, int total) {
                dialog.setProgress(current,total);
                try{
                    if(current==total)dialog.cancel();
                }catch (Exception e){e.printStackTrace();}
            }

            @Override
            public void onRefreshCompleted(List<AppItem> appList) {
               try {
                   dialog.cancel();
               }catch (Exception e){e.printStackTrace();}
                int mode=Global.getGlobalSharedPreferences(MainActivity.this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                        ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
               ListAdapter adapter;
               if(mode==1){
                   GridLayoutManager gridLayoutManager=new GridLayoutManager(MainActivity.this,4);
                   recyclerView.setLayoutManager(gridLayoutManager);
                   adapter=new ListAdapter(appList,1);
               }else{
                   LinearLayoutManager linearLayoutManager=new LinearLayoutManager(MainActivity.this);
                   linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                   recyclerView.setLayoutManager(linearLayoutManager);
                   adapter=new ListAdapter(appList,0);
               }

                recyclerView.setAdapter(adapter);
                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        try{
                            ListAdapter adapter=(ListAdapter)recyclerView.getAdapter();
                            if(adapter.getIsMultiSelectMode()||isSearchMode)return;
                        }catch (Exception e){e.printStackTrace();}
                        refreshList();
                    }
                });
                recyclerView.removeOnScrollListener(onScrollListener);
                recyclerView.addOnScrollListener(onScrollListener);
                cb_show_sys.setEnabled(true);
                swipeRefreshLayout.setRefreshing(false);

            }
        }).start();
    }

    @Override
    public void onClick(View v){
        ListAdapter adapter;
        try{
            adapter=(ListAdapter)recyclerView.getAdapter();
            if(adapter==null)return;
        }catch (Exception e){
            e.printStackTrace();
            return;
        }

        switch (v.getId()){
            default:break;
            case R.id.main_select_all:{
                adapter.setSelectAll(true);
            }
            break;
            case R.id.main_deselect_all:{
                adapter.setSelectAll(false);
            }
            break;
            case R.id.main_export:{
                bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                bottom_card_multi_select.setVisibility(View.GONE);
                final ArrayList<AppItem>arrayList=new ArrayList<>();
                try{
                    arrayList.addAll(((ListAdapter)recyclerView.getAdapter()).getSelectedAppItems());
                }catch (Exception e){e.printStackTrace();}
                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(this, arrayList, new Global.ExportTaskFinishedListener() {
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
                        ToastManager.showToast(MainActivity.this,getResources().getString(R.string.toast_export_complete)+Global.getSavePath(MainActivity.this),Toast.LENGTH_SHORT);
                        refreshAvailableStorage();
                    }
                });
                try{
                    ((ListAdapter)recyclerView.getAdapter()).closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
                closeMultiSelectModeForExternalVariables(true);
            }
            break;
            case R.id.main_share:{
                bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                bottom_card_multi_select.setVisibility(View.GONE);
                final ArrayList<AppItem>arrayList=new ArrayList<>();
                try{
                    arrayList.addAll(((ListAdapter)recyclerView.getAdapter()).getSelectedAppItems());
                }catch (Exception e){e.printStackTrace();}
                Global.shareCertainAppsByItems(this,arrayList);
                try{
                    ((ListAdapter)recyclerView.getAdapter()).closeMultiSelectMode();
                }catch (Exception e){e.printStackTrace();}
                closeMultiSelectModeForExternalVariables(true);
            }
            break;
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
            SharedPreferences settings=Global.getGlobalSharedPreferences(this);
            int mode=settings.getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,mode==0?1:0);
            editor.apply();

            closeMultiSelectModeForExternalVariables(false);

            refreshList();
        }

        if(id==R.id.action_sort){
            final SharedPreferences settings=Global.getGlobalSharedPreferences(this);
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

    private void openSearchMode(){
        try{
            isSearchMode=true;
            swipeRefreshLayout.setEnabled(false);
            closeMultiSelectModeForExternalVariables(false);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setMenuVisible(false);
            recyclerView.setAdapter(null);
            bottom_card.setVisibility(View.GONE);
            searchView.requestFocus();
            inputMethodManager.showSoftInput(searchView.findFocus(),0);
        }catch (Exception e){e.printStackTrace();}
    }

    private void closeSearchMode(){
        isSearchMode=false;
        swipeRefreshLayout.setEnabled(true);
        closeMultiSelectModeForExternalVariables(false);
        setMenuVisible(true);
        List<AppItem>list=Global.list;
        if(list==null)list=new ArrayList<>();
        recyclerView.setAdapter(new ListAdapter(list,Global.getGlobalSharedPreferences(this).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT)));
        bottom_card.setVisibility(View.VISIBLE);
        try{
            getSupportActionBar().setDisplayShowCustomEnabled(false);
        }catch (Exception e){e.printStackTrace();}
        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }catch (Exception e){e.printStackTrace();}
        try{
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
        }catch (Exception e){e.printStackTrace();}
    }

    private void closeMultiSelectModeForExternalVariables(boolean b){
        swipeRefreshLayout.setEnabled(true);
        bottom_card_multi_select.setVisibility(View.GONE);
        if(!isSearchMode){
            if(b)bottom_card.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.entry_300));
            bottom_card.setVisibility(View.VISIBLE);
            try{getSupportActionBar().setDisplayHomeAsUpEnabled(false);}catch (Exception e){e.printStackTrace();}
        }
    }

    private void setMenuVisible(boolean b){
        if(menu==null)return;
        for(int i=0;i<menu.size();i++){
            menu.getItem(i).setVisible(b);
        }
    }

    private void refreshAvailableStorage(){
        ((TextView)findViewById(R.id.main_storage_remain)).setText(getResources().getString(R.string.main_card_remaining_storage)+":"+
                Formatter.formatFileSize(this, Storage.getAvaliableSizeOfPath(Global.getSavePath(this))));
    }

    private void checkAndExit(){
        if(isSearchMode){
            try{
                ListAdapter adapter=(ListAdapter)recyclerView.getAdapter();
                if(adapter!=null&&adapter.getIsMultiSelectMode()){
                    adapter.closeMultiSelectMode();
                    bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(this,R.anim.exit_300));
                    bottom_card_multi_select.setVisibility(View.GONE);
                    return;
                }
            }catch (Exception e){e.printStackTrace();}
            closeSearchMode();
            return;
        }
        try{
            ListAdapter adapter=(ListAdapter)recyclerView.getAdapter();
            if(adapter!=null&&adapter.getIsMultiSelectMode()){
                adapter.closeMultiSelectMode();
                closeMultiSelectModeForExternalVariables(true);
                return;
            }
        }catch (Exception e){e.printStackTrace();}
        finish();
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder>{

        static final int MODE_LINEAR=0;
        static final int MODE_GRID=1;

        private List<AppItem>list;
        private boolean[] isSelected;
        private boolean isMultiSelectMode=false;
        private int mode=0;

        private ListAdapter(@NonNull List<AppItem> list,int mode){
            this.list=list;
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
                final AppItem item=list.get(viewHolder.getAdapterPosition());
                if(item==null)return;
                viewHolder.title.setText(String.valueOf(item.getAppName()));
                //viewHolder.icon.setImageDrawable(item.getIcon());
                viewHolder.icon.setImageDrawable(item.getIcon(MainActivity.this));
                if(mode==0){
                    viewHolder.description.setText(String.valueOf(item.getPackageName()));
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
                            Intent intent=new Intent(MainActivity.this,AppDetailActivity.class);
                            //intent.putExtra(EXTRA_PARCELED_APP_ITEM,item);
                            intent.putExtra(EXTRA_PACKAGE_NAME,item.getPackageName());
                            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this,new Pair<View, String>(viewHolder.icon,"icon"));
                            try{
                                ActivityCompat.startActivity(MainActivity.this, intent, compat.toBundle());
                            }catch (Exception e){e.printStackTrace();}
                        }

                    }
                });
                viewHolder.root.setOnLongClickListener(isMultiSelectMode?null:new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        swipeRefreshLayout.setEnabled(false);
                        isSelected=new boolean[list.size()];
                        isSelected[viewHolder.getAdapterPosition()]=true;
                        isMultiSelectMode=true;
                        refreshButtonStatus();
                        notifyDataSetChanged();
                        try{
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }catch (Exception e){e.printStackTrace();}
                        try{
                            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
                        }catch (Exception e){e.printStackTrace();}
                        bottom_card.setVisibility(View.GONE);
                        bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.entry_300));
                        bottom_card_multi_select.setVisibility(View.VISIBLE);
                        return true;
                    }
                });

            }catch (Exception e){e.printStackTrace();}
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private void closeMultiSelectMode(){
            isMultiSelectMode=false;
            notifyDataSetChanged();
        }

        private boolean getIsMultiSelectMode(){
            return isMultiSelectMode;
        }

        private void setSelectAll(boolean selected){
            if(!isMultiSelectMode||isSelected==null)return;
            for(int i=0;i<isSelected.length;i++)isSelected[i]=selected;
            main_export.setEnabled(selected);
            main_share.setEnabled(selected);
            refreshButtonStatus();
            notifyDataSetChanged();
        }

        private void refreshButtonStatus(){
            main_export.setEnabled(getSelectedNum()>0);
            main_share.setEnabled(getSelectedNum()>0);
            ((TextView)findViewById(R.id.main_select_num_size)).setText(getSelectedNum()+getResources().getString(R.string.unit_item)+"/"+Formatter.formatFileSize(MainActivity.this,getSelectedFileLength()));
            /*main_export.setText(getResources().getString(R.string.main_card_multi_select_export)+"("
                    +getSelectedNum()+getResources().getString(R.string.unit_item)+"/"
                    +Formatter.formatFileSize(MainActivity.this,getSelectedFileLength())+")");
            main_share.setText(getResources().getString(R.string.main_card_multi_select_share)+"("+getSelectedNum()
                    +getResources().getString(R.string.unit_item)+")");*/
        }


        /**
         * 返回的是初始化data,obb为false的副本
         */
        private List<AppItem> getSelectedAppItems(){
            ArrayList<AppItem>list_selected=new ArrayList<>();
            if(!isMultiSelectMode)return list_selected;
            for(int i=0;i<list.size();i++){
                if(isSelected[i])list_selected.add(new AppItem(list.get(i),false,false));
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
