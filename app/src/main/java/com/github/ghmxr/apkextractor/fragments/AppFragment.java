package com.github.ghmxr.apkextractor.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.activities.AppDetailActivity;
import com.github.ghmxr.apkextractor.activities.BaseActivity;
import com.github.ghmxr.apkextractor.adapters.RecyclerViewAdapter;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.tasks.RefreshInstalledListTask;
import com.github.ghmxr.apkextractor.tasks.SearchAppItemTask;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppFragment extends Fragment implements View.OnClickListener, RefreshInstalledListTask.RefreshInstalledListTaskCallback
        , RecyclerViewAdapter.ListAdapterOperationListener<AppItem>, SearchAppItemTask.SearchTaskCompletedCallback {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter<AppItem> adapter;
    private ViewGroup loading_content;
    private ProgressBar progressBar;
    private TextView tv_progress;
    private ViewGroup viewGroup_no_content;
    private CardView card_multi_select, card_normal;
    private CheckBox cb_sys;
    private TextView tv_space_remaining, tv_multi_select_head;
    private Button btn_select_all, btn_export, btn_share, btn_more;
    private PopupWindow popupWindow;
    private boolean isScrollable = false;
    private boolean isSearchMode = false;

    private OperationCallback callback;

    private RefreshInstalledListTask refreshInstalledListTask = null;

    final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (getActivity() == null) return;
            if (adapter == null) return;
            if (card_normal == null || card_multi_select == null) return;
            boolean isMultiSelectMode = adapter.getIsMultiSelectMode();
            if (!recyclerView.canScrollVertically(-1)) {
                // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
                // onScrolledToBottom();
                if (isMultiSelectMode) {
                    if (isScrollable && card_multi_select.getVisibility() != View.GONE)
                        setViewVisibilityWithAnimation(card_multi_select, View.GONE);
                } else if (isScrollable && card_normal.getVisibility() != View.GONE && !isSearchMode)
                    setViewVisibilityWithAnimation(card_normal, View.GONE);
            } else if (dy < 0) {
                // onScrolledUp();
                if (isMultiSelectMode) {
                    if (isScrollable && card_multi_select.getVisibility() != View.VISIBLE)
                        setViewVisibilityWithAnimation(card_multi_select, View.VISIBLE);
                } else if (isScrollable && card_normal.getVisibility() != View.VISIBLE && !isSearchMode)
                    setViewVisibilityWithAnimation(card_normal, View.VISIBLE);
            } else if (dy > 0) {
                // onScrolledDown();
                isScrollable = true;
                if (isMultiSelectMode) {
                    if (card_multi_select.getVisibility() != View.GONE)
                        setViewVisibilityWithAnimation(card_multi_select, View.GONE);
                } else if (card_normal.getVisibility() != View.GONE && !isSearchMode)
                    setViewVisibilityWithAnimation(card_normal, View.GONE);
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null) return;
            if (Constants.ACTION_REFRESH_APP_LIST.equalsIgnoreCase(intent.getAction())) {
                setAndStartRefreshingTask();
            } else if (Constants.ACTION_REFRESH_AVAILIBLE_STORAGE.equalsIgnoreCase(intent.getAction())) {
                refreshAvailableStorage();
            }
        }
    };

    private final BroadcastReceiver receiver_app = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null) return;
            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) || Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    || Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
                setAndStartRefreshingTask();
            }
        }
    };

    public AppFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_export, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        swipeRefreshLayout = view.findViewById(R.id.content_swipe);
        recyclerView = view.findViewById(R.id.content_recyclerview);
        loading_content = view.findViewById(R.id.loading);
        progressBar = view.findViewById(R.id.loading_pg);
        tv_progress = view.findViewById(R.id.loading_text);
        viewGroup_no_content = view.findViewById(R.id.no_content_att);
        card_multi_select = view.findViewById(R.id.export_card_multi_select);
        card_normal = view.findViewById(R.id.export_card);
        cb_sys = view.findViewById(R.id.main_show_system_app);
        tv_space_remaining = view.findViewById(R.id.main_storage_remain);
        tv_multi_select_head = view.findViewById(R.id.main_select_num_size);
        btn_select_all = view.findViewById(R.id.main_select_all);
        btn_export = view.findViewById(R.id.main_export);
        btn_share = view.findViewById(R.id.main_share);
        btn_more = view.findViewById(R.id.main_more);
        View popupView = LayoutInflater.from(getActivity()).inflate(R.layout.pp_more, null);
        ViewGroup more_copy_package_names = popupView.findViewById(R.id.popup_copy_package_name);
        more_copy_package_names.setOnClickListener(this);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_popup_window)));
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_REFRESH_APP_LIST);
            intentFilter.addAction(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST);
            intentFilter.addAction(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE);
            if (getActivity() != null) getActivity().registerReceiver(receiver, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            intentFilter.addDataScheme("package");
            if (getActivity() != null) getActivity().registerReceiver(receiver_app, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initView();
    }

    private void initView() {
        if (getActivity() == null) return;
        cb_sys.setChecked(SPUtil.getGlobalSharedPreferences(getActivity()).getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT));
        swipeRefreshLayout.setColorSchemeColors(getActivity().getResources().getColor(R.color.colorTitle));

        btn_select_all.setOnClickListener(this);
        btn_export.setOnClickListener(this);
        btn_share.setOnClickListener(this);
        btn_more.setOnClickListener(this);

        recyclerView.addOnScrollListener(onScrollListener);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (getActivity() == null) return;
                if (isSearchMode) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                if (adapter != null && adapter.getIsMultiSelectMode()) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                setAndStartRefreshingTask();
            }
        });
        cb_sys.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setEnabled(false);
                if (getActivity() == null) return;
                SPUtil.getGlobalSharedPreferences(getActivity()).edit().putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, isChecked).apply();
                setAndStartRefreshingTask();
            }
        });
        setAndStartRefreshingTask();
        refreshAvailableStorage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (getActivity() != null) getActivity().unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (getActivity() != null) getActivity().unregisterReceiver(receiver_app);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if (getActivity() == null) return;
        switch (v.getId()) {
            default:
                break;
            case R.id.main_select_all: {
                if (adapter != null) adapter.setToggleSelectAll();
            }
            break;
            case R.id.main_export: {
                if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                    Global.showRequestingWritePermissionSnackBar(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return;
                }
                if (adapter == null) return;
                final ArrayList<AppItem> arrayList = new ArrayList<>(adapter.getSelectedItems());
                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(getActivity(), arrayList, true, new Global.ExportTaskFinishedListener() {
                    @Override
                    public void onFinished(@NonNull String error_message) {
                        if (getActivity() == null) return;
                        if (!error_message.trim().equals("")) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getResources().getString(R.string.exception_title))
                                    .setMessage(getResources().getString(R.string.exception_message) + error_message)
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();
                        } else {
                            ToastManager.showToast(getActivity(), getResources().getString(R.string.toast_export_complete) + " "
                                    + SPUtil.getDisplayingExportPath(getActivity()), Toast.LENGTH_SHORT);
                        }
                        closeMultiSelectMode();
                        refreshAvailableStorage();
                    }
                });
            }
            break;
            case R.id.main_share: {
                if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                    Global.showRequestingWritePermissionSnackBar(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return;
                }
                if (adapter == null) return;
                final ArrayList<AppItem> arrayList = new ArrayList<>(adapter.getSelectedItems());
                //closeMultiSelectMode();
                Global.shareCertainAppsByItems(getActivity(), arrayList);
            }
            break;
            case R.id.main_more: {
                int[] values = EnvironmentUtil.calculatePopWindowPos(btn_more, popupWindow.getContentView());
                popupWindow.showAtLocation(v, 0, values[0], values[1]);
            }
            break;
            case R.id.popup_copy_package_name: {
                List<AppItem> appItemList = adapter.getSelectedItems();
                if (appItemList.size() == 0) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), getResources().getString(R.string.snack_bar_no_app_selected), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                popupWindow.dismiss();
                StringBuilder stringBuilder = new StringBuilder();
                for (AppItem appItem : appItemList) {
                    if (stringBuilder.toString().length() > 0)
                        stringBuilder.append(SPUtil.getGlobalSharedPreferences(getActivity())
                                .getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT));
                    stringBuilder.append(appItem.getPackageName());
                }
                //closeMultiSelectMode();
                clip2ClipboardAndShowSnackbar(stringBuilder.toString());
            }
            break;
        }
    }

    @Override
    public void onRefreshProgressStarted(int total) {
        if (getActivity() == null) return;
        isScrollable = false;
        recyclerView.setAdapter(null);
        loading_content.setVisibility(View.VISIBLE);
        viewGroup_no_content.setVisibility(View.GONE);
        progressBar.setMax(total);
        progressBar.setProgress(0);
        swipeRefreshLayout.setRefreshing(true);
        cb_sys.setEnabled(false);
        card_multi_select.setVisibility(View.GONE);
    }

    @Override
    public void onRefreshProgressUpdated(int current, int total) {
        if (getActivity() == null) return;
        progressBar.setProgress(current);
        tv_progress.setText(getActivity().getResources().getString(R.string.dialog_loading_title) + " " + current + "/" + total);
    }

    @Override
    public void onRefreshCompleted(List<AppItem> appList) {
        if (getActivity() == null) return;
        loading_content.setVisibility(View.GONE);
        viewGroup_no_content.setVisibility(appList.size() == 0 ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setEnabled(true);
        /*int mode= SPUtil.getGlobalSharedPreferences(getActivity()).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE
                ,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);*/
        adapter = new RecyclerViewAdapter<>(getActivity()
                , recyclerView
                , appList
                , SPUtil.getGlobalSharedPreferences(getActivity()).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE, Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT)
                , this);
        recyclerView.setAdapter(adapter);
        cb_sys.setEnabled(true);
        //if(isSearchMode)adapter.setData(null);
    }

    @Override
    public void onItemClicked(AppItem appItem, RecyclerViewAdapter.ViewHolder viewHolder, int position) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), AppDetailActivity.class);
        intent.putExtra(BaseActivity.EXTRA_PACKAGE_NAME, appItem.getPackageName());
        ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), new Pair<View, String>(viewHolder.icon, "icon"));
        try {
            ActivityCompat.startActivity(getActivity(), intent, compat.toBundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMultiSelectItemChanged(List<AppItem> selected_items, long length) {
        if (getActivity() == null) return;
        tv_multi_select_head.setText(selected_items.size() + getResources().getString(R.string.unit_item) + "/" + Formatter.formatFileSize(getActivity(), length));
        btn_export.setEnabled(selected_items.size() > 0);
        btn_share.setEnabled(selected_items.size() > 0);
    }

    @Override
    public void onMultiSelectModeOpened() {
        if (getActivity() == null) return;
        card_normal.setVisibility(View.GONE);
        setViewVisibilityWithAnimation(card_multi_select, View.VISIBLE);
        //isCurrentPageMultiSelectMode=true;
        swipeRefreshLayout.setEnabled(false);
        //setBackButtonVisible(true);
        EnvironmentUtil.hideInputMethod(getActivity());
        if (callback != null) callback.onItemLongClickedAndMultiSelectModeOpened(this);
    }

    @Override
    public void onSearchTaskCompleted(@NonNull List<AppItem> appItems, @NonNull String keyword) {
        if (getActivity() == null) return;
        if (adapter == null) return;
        swipeRefreshLayout.setRefreshing(false);
        adapter.setData(appItems);
        adapter.setHighlightKeyword(keyword);
    }

    public void setOperationCallback(OperationCallback callback) {
        this.callback = callback;
    }

    public boolean isMultiSelectMode() {
        return adapter != null && adapter.getIsMultiSelectMode();
    }

    public void closeMultiSelectMode() {
        if (adapter == null) return;
        adapter.setMultiSelectMode(false);
        swipeRefreshLayout.setEnabled(true);
        if (isSearchMode) {
            card_normal.setVisibility(View.GONE);
            setViewVisibilityWithAnimation(card_multi_select, View.GONE);
        } else {
            setViewVisibilityWithAnimation(card_normal, View.VISIBLE);
            card_multi_select.setVisibility(View.GONE);
        }

    }

    public void setSearchMode(boolean b) {
        this.isSearchMode = b;
        if (b) {
            if (card_normal != null) card_normal.setVisibility(View.GONE);
        } else {
            setViewVisibilityWithAnimation(card_normal, View.VISIBLE);
            if (adapter != null) adapter.setHighlightKeyword(null);
        }
        if (card_multi_select != null) card_multi_select.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(!b);
        }
        if (adapter == null) return;
        adapter.setMultiSelectMode(false);
        if (b) {
            adapter.setData(null);
        } else {
            synchronized (Global.app_list) {
                adapter.setData(Global.app_list);
            }
        }
    }

    public boolean getIsSearchMode() {
        return isSearchMode;
    }

    private SearchAppItemTask searchAppItemTask;

    public void updateSearchModeKeywords(@NonNull String key) {
        if (getActivity() == null) return;
        if (adapter == null) return;
        if (!isSearchMode) return;
        if (searchAppItemTask != null) searchAppItemTask.setInterrupted();
        synchronized (Global.app_list) {
            searchAppItemTask = new SearchAppItemTask(Global.app_list, key, this);
        }
        adapter.setData(null);
        adapter.setMultiSelectMode(false);
        card_multi_select.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);
        searchAppItemTask.start();
    }

    public void sortGlobalListAndRefresh(int value) {
        closeMultiSelectMode();
        AppItem.sort_config = value;
        if (adapter != null) adapter.setData(null);
        swipeRefreshLayout.setRefreshing(true);
        cb_sys.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (Global.app_list) {
                    Collections.sort(Global.app_list);
                }
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter != null) {
                            synchronized (Global.app_list) {
                                adapter.setData(Global.app_list);
                            }
                        }
                        swipeRefreshLayout.setRefreshing(false);
                        cb_sys.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    public void setViewMode(int mode) {
        if (adapter == null) return;
        adapter.setLayoutManagerAndView(mode);
    }

    private void setAndStartRefreshingTask() {
        if (getActivity() == null) return;
        if (refreshInstalledListTask != null) refreshInstalledListTask.setInterrupted();
        refreshInstalledListTask = new RefreshInstalledListTask(getActivity(), this);
        swipeRefreshLayout.setRefreshing(true);
        recyclerView.setAdapter(null);
        cb_sys.setEnabled(false);
        refreshInstalledListTask.start();
    }

    private void setViewVisibilityWithAnimation(View view, int visibility) {
        if (getActivity() == null) return;
        if (visibility == View.GONE) {
            if (view.getVisibility() != View.GONE)
                view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.exit_300));
            view.setVisibility(View.GONE);
        } else if (visibility == View.VISIBLE) {
            if (view.getVisibility() != View.VISIBLE)
                view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.entry_300));
            view.setVisibility(View.VISIBLE);
        }
    }

    private void refreshAvailableStorage() {
        try {
            if (getActivity() == null) return;
            String head = getResources().getString(R.string.main_card_remaining_storage) + ":";
            boolean isExternal = SPUtil.getIsSaved2ExternalStorage(getActivity());
            if (isExternal) {
                long available = 0;
                if (Build.VERSION.SDK_INT >= 19) {
                    File[] files = getActivity().getExternalFilesDirs(null);
                    for (File file : files) {
                        if (file.getAbsolutePath().toLowerCase().startsWith(StorageUtil.getMainExternalStoragePath()))
                            continue;
                        available = StorageUtil.getAvaliableSizeOfPath(file.getAbsolutePath());
                    }
                }
                head += Formatter.formatFileSize(getActivity(), available);
            } else {
                head += Formatter.formatFileSize(getActivity(), StorageUtil.getAvaliableSizeOfPath(StorageUtil.getMainExternalStoragePath()));
            }

            tv_space_remaining.setText(head);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clip2ClipboardAndShowSnackbar(String s) {
        try {
            if (getActivity() == null) return;
            ClipboardManager manager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("message", s));
            Snackbar.make(getActivity().findViewById(android.R.id.content), getResources().getString(R.string.snack_bar_clipboard), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
