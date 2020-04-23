package com.github.ghmxr.apkextractor.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.PermissionChecker;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.activities.PackageDetailActivity;
import com.github.ghmxr.apkextractor.adapters.RecyclerViewAdapter;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.RefreshImportListTask;
import com.github.ghmxr.apkextractor.tasks.SearchPackageTask;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportFragment extends Fragment implements RefreshImportListTask.RefreshImportListTaskCallback,RecyclerViewAdapter.ListAdapterOperationListener
, SearchPackageTask.SearchTaskCompletedCallback ,View.OnClickListener{

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter<ImportItem> adapter;
    private ViewGroup viewGroup_no_content;
    private CardView card_multi_select;
    private TextView tv_multi_select_head;
    private Button btn_import,btn_delete,btn_share,btn_select;
    private boolean isScrollable=false;
    private boolean isSearchMode=false;

    private OperationCallback callback;

    private final RecyclerView.OnScrollListener onScrollListener =new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if(adapter==null)return;
            boolean isMultiSelectMode=adapter.getIsMultiSelectMode();
            if (!recyclerView.canScrollVertically(-1)) {
                // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
                // onScrolledToBottom();
                if(isMultiSelectMode){
                    if(isScrollable&&card_multi_select.getVisibility()!= View.GONE)
                        setViewVisibilityWithAnimation(card_multi_select,View.GONE);
                }
            } else if (dy < 0) {
                // onScrolledUp();
                if(isMultiSelectMode){
                    if(isScrollable&&card_multi_select.getVisibility()!=View.VISIBLE){
                        setViewVisibilityWithAnimation(card_multi_select,View.VISIBLE);
                    }
                }
            } else if (dy > 0) {
                // onScrolledDown();
                isScrollable=true;
                if(isMultiSelectMode){
                    if(card_multi_select.getVisibility()!= View.GONE)
                        setViewVisibilityWithAnimation(card_multi_select,View.GONE);
                }
            }
        }
    };

    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST.equalsIgnoreCase(intent.getAction())){
                if(getActivity()==null)return;
                new RefreshImportListTask(getActivity(),ImportFragment.this).start();
            }
        }
    };

    public ImportFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_import,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        swipeRefreshLayout=view.findViewById(R.id.content_swipe);
        recyclerView=view.findViewById(R.id.content_recyclerview);
        viewGroup_no_content=view.findViewById(R.id.no_content_att);
        card_multi_select=view.findViewById(R.id.import_card_multi_select);
        tv_multi_select_head=view.findViewById(R.id.import_card_att);
        btn_select=view.findViewById(R.id.import_select_all);
        btn_delete=view.findViewById(R.id.import_delete);
        btn_import=view.findViewById(R.id.import_action);
        btn_share=view.findViewById(R.id.import_share);

        try {
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST);
            if(getActivity()!=null)getActivity().registerReceiver(receiver,intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try{
            if(getActivity()!=null)getActivity().unregisterReceiver(receiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if(getActivity()==null)return;
        switch (v.getId()){
            default:break;
            case R.id.import_select_all:{
                if(adapter!=null)adapter.setToggleSelectAll();
            }
            break;
            case R.id.import_action:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }
                if(adapter==null)return;
                ArrayList<ImportItem>arrayList=new ArrayList<>(adapter.getSelectedItems());
                for(ImportItem importItem:arrayList){
                    if(importItem.getImportType()== ImportItem.ImportType.APK){
                        new AlertDialog.Builder(getActivity())
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
                Global.showImportingDataObbDialog(getActivity(), arrayList, new Global.ImportTaskFinishedCallback() {
                    @Override
                    public void onImportFinished(String error_message) {
                        if(getActivity()==null)return;
                        if(!error_message.trim().equals("")){
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getResources().getString(R.string.dialog_import_finished_error_title))
                                    .setMessage(getResources().getString(R.string.dialog_import_finished_error_message)+error_message)
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                        }else{
                            ToastManager.showToast(getActivity(),getResources().getString(R.string.toast_import_complete),Toast.LENGTH_SHORT);
                        }
                    }
                });
            }
            break;
            case R.id.import_delete:{
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.dialog_import_delete_title))
                        .setMessage(getResources().getString(R.string.dialog_import_delete_message))
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(getActivity()==null)return;
                                if(Build.VERSION.SDK_INT>=23&& PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                                    Global.showRequestingWritePermissionSnackBar(getActivity());
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                                    return;
                                }
                                try{
                                    List<ImportItem>importItems=adapter.getSelectedItems();
                                    for(ImportItem importItem:importItems){
                                        FileItem fileItem=importItem.getFileItem();
                                        try{
                                            fileItem.delete();
                                        }catch (Exception e){e.printStackTrace();}
                                    }
                                    adapter.setMultiSelectMode(false);
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
                                }catch (Exception e){
                                    e.printStackTrace();
                                    ToastManager.showToast(getActivity(),e.toString(), Toast.LENGTH_SHORT);
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
            case R.id.import_share:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }
                if(adapter==null)return;
                adapter.setMultiSelectMode(false);
                Global.shareImportItems(getActivity(),new ArrayList<>(adapter.getSelectedItems()));
            }
            break;
        }
    }

    @Override
    public void onRefreshStarted() {
        if(getActivity()==null)return;
        isScrollable=false;
        recyclerView.setAdapter(null);
        swipeRefreshLayout.setRefreshing(true);
        viewGroup_no_content.setVisibility(View.GONE);
    }

    @Override
    public void onRefreshCompleted(List<ImportItem> list) {
        if(getActivity()==null)return;
        swipeRefreshLayout.setRefreshing(false);
        adapter=new RecyclerViewAdapter<>(getActivity(),recyclerView,list,this);
        recyclerView.setAdapter(adapter);
        viewGroup_no_content.setVisibility(list.size()==0?View.VISIBLE:View.GONE);
        //if(isSearchMode)adapter.setData(null);
    }

    @Override
    public void onItemClicked(DisplayItem displayItem, RecyclerViewAdapter.ViewHolder viewHolder, int position) {
        if(getActivity()==null)return;
        if(!(displayItem instanceof ImportItem))return;
        ImportItem importItem=(ImportItem)displayItem;
        Intent intent =new Intent(getActivity(), PackageDetailActivity.class);
        //intent.putExtra(PackageDetailActivity.EXTRA_IMPORT_ITEM_POSITION,position);
        intent.putExtra(PackageDetailActivity.EXTRA_IMPORT_ITEM_PATH,importItem.getFileItem().getPath());
        ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),new Pair<View, String>(viewHolder.icon,"icon"));
        try{
            ActivityCompat.startActivity(getActivity(), intent, compat.toBundle());
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void onMultiSelectItemChanged(List<DisplayItem> selected_items, long length) {
        if(getActivity()==null)return;
        tv_multi_select_head.setText(selected_items.size()+getResources().getString(R.string.unit_item)+"/"+ Formatter.formatFileSize(getActivity(),length));
        btn_import.setEnabled(selected_items.size()>0);
        btn_delete.setEnabled(selected_items.size()>0);
        btn_share.setEnabled(selected_items.size()>0);
    }

    @Override
    public void onMultiSelectModeOpened() {
        if(getActivity()==null)return;
        setViewVisibilityWithAnimation(card_multi_select,View.VISIBLE);
        swipeRefreshLayout.setEnabled(false);
        EnvironmentUtil.hideInputMethod(getActivity());
        if(callback!=null)callback.onItemLongClickedAndMultiSelectModeOpened(this);
    }

    @Override
    public void onSearchTaskCompleted(@NonNull List<ImportItem> importItems) {
        if(getActivity()==null)return;
        if(adapter==null)return;
        adapter.setData(importItems);
    }

    public void setSearchMode(boolean b){
        this.isSearchMode=b;
        if(card_multi_select!=null)card_multi_select.setVisibility(View.GONE);
        if(swipeRefreshLayout!=null){
            swipeRefreshLayout.setEnabled(!b);
        }
        if(adapter==null)return;
        adapter.setMultiSelectMode(false);
        adapter.setData(b?null: Global.item_list);
    }

    private SearchPackageTask searchPackageTask;

    public void updateSearchModeKeywords(@NonNull String key){
        if(getActivity()==null)return;
        if(adapter==null)return;
        if(!isSearchMode)return;
        if(searchPackageTask!=null)searchPackageTask.setInterrupted();
        searchPackageTask=new SearchPackageTask(Global.item_list,key,this);
        adapter.setData(null);
        searchPackageTask.start();
    }

    public boolean isSearchMode() {
        return isSearchMode;
    }

    public boolean isMultiSelectMode(){
        return adapter!=null&&adapter.getIsMultiSelectMode();
    }

    public void closeMultiSelectMode(){
        if(adapter==null)return;
        adapter.setMultiSelectMode(false);
        swipeRefreshLayout.setEnabled(true);
        setViewVisibilityWithAnimation(card_multi_select,View.GONE);
    }

    public void setOperationCallback(OperationCallback callback){
        this.callback=callback;
    }

    public void sortGlobalListAndRefresh(int value){
        ImportItem.sort_config=value;
        adapter.setData(null);
        swipeRefreshLayout.setRefreshing(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (Global.app_list){
                    Collections.sort(Global.item_list);
                }
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setData(Global.item_list);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    public void setViewMode(int mode){
        if(adapter==null)return;
        adapter.setLayoutManagerAndView(mode);
    }

    private void initView(){
        if(getActivity()==null)return;
        btn_select.setOnClickListener(this);
        btn_share.setOnClickListener(this);
        btn_delete.setOnClickListener(this);
        btn_import.setOnClickListener(this);
        swipeRefreshLayout.setColorSchemeColors(getActivity().getResources().getColor(R.color.colorTitle));
        recyclerView.addOnScrollListener(onScrollListener);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(getActivity()==null)return;
                if(isSearchMode){
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                if(adapter!=null&&adapter.getIsMultiSelectMode()){
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                new RefreshImportListTask(getActivity(), ImportFragment.this).start();
            }
        });
        new RefreshImportListTask(getActivity(),ImportFragment.this).start();
    }

    private void setViewVisibilityWithAnimation(View view, int visibility){
        if(getActivity()==null)return;
        if(visibility==View.GONE){
            view.startAnimation(AnimationUtils.loadAnimation(getActivity(),R.anim.exit_300));
            view.setVisibility(View.GONE);
        }else if(visibility==View.VISIBLE){
            view.startAnimation(AnimationUtils.loadAnimation(getActivity(),R.anim.entry_300));
            view.setVisibility(View.VISIBLE);
        }
    }
}
