package com.github.ghmxr.apkextractor.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

    private Button main_select_all,main_deselect_all,main_export,main_share;

    private ListAdapter adapter;
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
            if(adapter==null)return;
            if(bottom_card==null)return;
            if(bottom_card_multi_select==null)return;
            boolean isMultiSelectMode=adapter.getIsMultiSelectMode();
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
                    if(isScrollable&&bottom_card.getVisibility()!=View.GONE){
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
                    if(isScrollable&&bottom_card.getVisibility()!=View.VISIBLE){
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
                    if(bottom_card.getVisibility()!=View.GONE){
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
                    if(adapter==null)return false;
                    progressBar.setVisibility(View.VISIBLE);
                    searchTask=new SearchTask(adapter.list, newText, new SearchTask.SearchTaskCompletedCallback() {
                        @Override
                        public void onSearchTaskCompleted(@NonNull List<AppItem> result) {
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setAdapter(new ListAdapter(result));
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
        recyclerView.setAdapter(null);

        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        final boolean is_show_sys=Global.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP,Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT);
        final CheckBox cb_show_sys=findViewById(R.id.main_show_system_app);
        final LoadingListDialog dialog=new LoadingListDialog(this);
        try{dialog.show();}catch (Exception e){e.printStackTrace();}
        new Global.RefreshInstalledListTask(this, is_show_sys, new Global.RefreshInstalledListTaskCallback() {
            @Override
            public void onRefreshProgressUpdated(int current, int total) {
                dialog.setProgress(current,total);
            }

            @Override
            public void onRefreshCompleted(List<AppItem> appList) {
               try {
                   dialog.cancel();
               }catch (Exception e){e.printStackTrace();}
                adapter=new ListAdapter(appList);
                recyclerView.setAdapter(adapter);
                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        try{
                            ListAdapter adapter=(ListAdapter)recyclerView.getAdapter();
                            if(adapter.getIsMultiSelectMode())return;
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
        if(adapter==null)return;
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
                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(this, adapter.getSelectedAppItems(), new Global.ExportTaskFinishedListener() {
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
                adapter.closeMultiSelectMode();
            }
            break;
            case R.id.main_share:{
                bottom_card_multi_select.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.exit_300));
                bottom_card_multi_select.setVisibility(View.GONE);
                Global.shareCertainAppsByItems(this,adapter.getSelectedAppItems());
                adapter.closeMultiSelectMode();
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
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setMenuVisible(false);
            recyclerView.setAdapter(null);
            bottom_card.setVisibility(View.GONE);
        }catch (Exception e){e.printStackTrace();}
    }

    private void closeSearchMode(){
        try{
            isSearchMode=false;
            getSupportActionBar().setDisplayShowCustomEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            setMenuVisible(true);
            recyclerView.setAdapter(adapter);
            bottom_card.setVisibility(View.VISIBLE);
        }catch (Exception e){e.printStackTrace();}
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
            closeSearchMode();
            return;
        }
        try{
            if(adapter.getIsMultiSelectMode()){
                adapter.closeMultiSelectMode();
                return ;
            }
        }catch (Exception e){e.printStackTrace();}
        finish();
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder>{
        private List<AppItem>list;
        private boolean[] isSelected;
        private boolean isMultiSelectMode=false;
        private ListAdapter(@NonNull List<AppItem> list){
            this.list=list;
            isSelected=new boolean[list.size()];
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.item_app_info,viewGroup,false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
            try{
                final AppItem item=list.get(viewHolder.getAdapterPosition());
                if(item==null)return;
                viewHolder.title.setText(String.valueOf(item.getAppName()));
                //viewHolder.icon.setImageDrawable(item.getIcon());
                viewHolder.icon.setImageDrawable(item.getIcon(MainActivity.this));
                viewHolder.description.setText(String.valueOf(item.getPackageName()));
                viewHolder.right.setText(Formatter.formatFileSize(MainActivity.this,item.getSize()));
                viewHolder.cb.setChecked(isSelected[viewHolder.getAdapterPosition()]);
                viewHolder.right.setVisibility(isMultiSelectMode?View.GONE:View.VISIBLE);
                viewHolder.cb.setVisibility(isMultiSelectMode?View.VISIBLE:View.GONE);
                viewHolder.root.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(isMultiSelectMode){
                            isSelected[viewHolder.getAdapterPosition()]=!isSelected[viewHolder.getAdapterPosition()];
                            refreshButtonStatus();
                            notifyItemChanged(viewHolder.getAdapterPosition());
                        }else{
                            Intent intent=new Intent(MainActivity.this,AppDetailActivity.class);
                            intent.putExtra(EXTRA_PARCELED_APP_ITEM,item);
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
            swipeRefreshLayout.setEnabled(true);
            isMultiSelectMode=false;
            bottom_card_multi_select.setVisibility(View.GONE);
            bottom_card.startAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.entry_300));
            bottom_card.setVisibility(View.VISIBLE);
            notifyDataSetChanged();
            try{getSupportActionBar().setDisplayHomeAsUpEnabled(false);}catch (Exception e){e.printStackTrace();}
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
            main_export.setText(getResources().getString(R.string.main_card_multi_select_export)+"("
                    +getSelectedNum()+getResources().getString(R.string.unit_item)+"/"
                    +Formatter.formatFileSize(MainActivity.this,getSelectedFileLength())+")");
            main_share.setText(getResources().getString(R.string.main_card_multi_select_share)+"("+getSelectedNum()
                    +getResources().getString(R.string.unit_item)+")");
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
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            root=itemView.findViewById(R.id.item_app_root);
            icon=itemView.findViewById(R.id.item_app_icon);
            title=itemView.findViewById(R.id.item_app_title);
            description=itemView.findViewById(R.id.item_app_description);
            right=itemView.findViewById(R.id.item_app_right);
            cb=itemView.findViewById(R.id.item_app_cb);
        }
    }
}
