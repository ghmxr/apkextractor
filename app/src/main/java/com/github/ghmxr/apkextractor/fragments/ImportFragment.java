package com.github.ghmxr.apkextractor.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.activities.PackageDetailActivity;
import com.github.ghmxr.apkextractor.adapters.RecyclerViewAdapter;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.tasks.RefreshImportListTask;
import com.github.ghmxr.apkextractor.tasks.SearchPackageTask;
import com.github.ghmxr.apkextractor.ui.FileRenamingDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ImportFragment extends Fragment implements RefreshImportListTask.RefreshImportListTaskCallback, RecyclerViewAdapter.ListAdapterOperationListener<ImportItem>
        , SearchPackageTask.SearchTaskCompletedCallback, View.OnClickListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter<ImportItem> adapter;
    private ViewGroup viewGroup_no_content;
    private ViewGroup viewGroup_progress;
    private ProgressBar progressBar;
    private TextView progressTextView;
    private CardView card_multi_select;
    private TextView tv_multi_select_head;
    private Button btn_import, btn_delete, btn_more, btn_select;
    private PopupWindow popupWindow;
    private boolean isScrollable = false;
    private boolean isSearchMode = false;

    private OperationCallback callback;

    public static final int REQUEST_CODE_PACKAGE_DETAIL = 101;

    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (adapter == null) return;
            boolean isMultiSelectMode = adapter.getIsMultiSelectMode();
            if (!recyclerView.canScrollVertically(-1)) {
                // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
                // onScrolledToBottom();
                if (isMultiSelectMode) {
                    if (isScrollable && card_multi_select.getVisibility() != View.GONE)
                        setViewVisibilityWithAnimation(card_multi_select, View.GONE);
                }
            } else if (dy < 0) {
                // onScrolledUp();
                if (isMultiSelectMode) {
                    if (isScrollable && card_multi_select.getVisibility() != View.VISIBLE) {
                        setViewVisibilityWithAnimation(card_multi_select, View.VISIBLE);
                    }
                }
            } else if (dy > 0) {
                // onScrolledDown();
                isScrollable = true;
                if (isMultiSelectMode) {
                    if (card_multi_select.getVisibility() != View.GONE)
                        setViewVisibilityWithAnimation(card_multi_select, View.GONE);
                }
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST.equalsIgnoreCase(intent.getAction())) {
                if (getActivity() == null) return;
                new RefreshImportListTask(ImportFragment.this).start();
            }
            if (Constants.ACTION_REFILL_IMPORT_LIST.equalsIgnoreCase(intent.getAction())) {
                if (adapter != null) {
                    adapter.setData(Global.item_list);
                    if (viewGroup_no_content != null) {
                        viewGroup_no_content.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }
                }
            }
        }
    };

    public ImportFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        swipeRefreshLayout = view.findViewById(R.id.content_swipe);
        recyclerView = view.findViewById(R.id.content_recyclerview);
        viewGroup_no_content = view.findViewById(R.id.no_content_att);
        viewGroup_progress = view.findViewById(R.id.bottomBar);
        progressBar = view.findViewById(R.id.loading_pg);
        progressTextView = view.findViewById(R.id.bottomBarText);
        card_multi_select = view.findViewById(R.id.import_card_multi_select);
        tv_multi_select_head = view.findViewById(R.id.import_card_att);
        btn_select = view.findViewById(R.id.import_select_all);
        btn_delete = view.findViewById(R.id.import_delete);
        btn_import = view.findViewById(R.id.import_action);
        btn_more = view.findViewById(R.id.import_more);
        View popupView = LayoutInflater.from(getActivity()).inflate(R.layout.pp_more_import, null);
        popupView.findViewById(R.id.popup_file_rename).setOnClickListener(this);
        popupView.findViewById(R.id.popup_share).setOnClickListener(this);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_popup_window)));
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);

        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST);
            intentFilter.addAction(Constants.ACTION_REFILL_IMPORT_LIST);
            if (getActivity() != null) getActivity().registerReceiver(receiver, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (getActivity() != null) getActivity().unregisterReceiver(receiver);
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
            case R.id.import_select_all: {
                if (adapter != null) adapter.setToggleSelectAll();
            }
            break;
            case R.id.import_action: {
                if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                    Global.showRequestingWritePermissionSnackBar(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return;
                }
                if (adapter == null) return;
                ArrayList<ImportItem> arrayList = new ArrayList<>(adapter.getSelectedItems());
                for (ImportItem importItem : arrayList) {
                    if (importItem.getImportType() == ImportItem.ImportType.APK) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getResources().getString(R.string.dialog_import_contains_apk_title))
                                .setMessage(getResources().getString(R.string.dialog_import_contains_apk_message))
                                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                        return;
                    }
                }
                Global.showImportingDataObbDialog(getActivity(), arrayList, new Global.ImportTaskFinishedCallback() {
                    @Override
                    public void onImportFinished(String error_message) {
                        if (getActivity() == null) return;
                        if (!error_message.trim().equals("")) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getResources().getString(R.string.dialog_import_finished_error_title))
                                    .setMessage(getResources().getString(R.string.dialog_import_finished_error_message) + error_message)
                                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();
                        } else {
                            ToastManager.showToast(getActivity(), getResources().getString(R.string.toast_import_complete), Toast.LENGTH_SHORT);
                        }
                        closeMultiSelectMode();
                    }
                });
            }
            break;
            case R.id.import_delete: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.dialog_import_delete_title))
                        .setMessage(getResources().getString(R.string.dialog_import_delete_message))
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getActivity() == null) return;
                                if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                                    Global.showRequestingWritePermissionSnackBar(getActivity());
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                                    return;
                                }
                                try {
                                    List<ImportItem> importItems = adapter.getSelectedItems();
                                    HashSet<ImportItem> deleted = new HashSet<>();
                                    for (ImportItem importItem : importItems) {
                                        FileItem fileItem = importItem.getFileItem();
                                        try {
                                            if (fileItem.delete()) deleted.add(importItem);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    closeMultiSelectMode();
//                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                                    if (adapter != null) {
                                        adapter.removeItems(deleted);
                                        if (viewGroup_no_content != null) {
                                            viewGroup_no_content.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                                        }
                                    }

                                    Global.item_list.removeAll(deleted);

                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    ToastManager.showToast(getActivity(), e.toString(), Toast.LENGTH_SHORT);
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
            case R.id.import_more: {
                int[] values = EnvironmentUtil.calculatePopWindowPos(btn_more, popupWindow.getContentView());
                popupWindow.showAtLocation(v, 0, values[0], values[1]);
            }
            break;
            case R.id.popup_share: {
                if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                    Global.showRequestingWritePermissionSnackBar(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return;
                }
                if (adapter == null) return;
                if (adapter.getSelectedItems().size() == 0) return;
                Global.shareImportItems(getActivity(), new ArrayList<>(adapter.getSelectedItems()));
                popupWindow.dismiss();
            }
            break;
            case R.id.popup_file_rename: {
                if (adapter == null || adapter.getSelectedItems().size() == 0) return;
                popupWindow.dismiss();
                new FileRenamingDialog(getActivity(), adapter.getSelectedItems(), new FileRenamingDialog.CompletedCallback() {
                    @Override
                    public void onCompleted(@NonNull String errorInfo) {
                        if (errorInfo.length() > 0) {
                            if (getActivity() == null) return;
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getActivity().getResources().getString(R.string.word_error))
                                    .setMessage(getActivity().getResources().getString(R.string.dialog_filename_failure) + errorInfo)
                                    .setPositiveButton(getActivity().getResources().getString(R.string.action_confirm), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();
//                            new RefreshImportListTask(ImportFragment.this).start();
                        }
                        sortGlobalListAndRefresh(ImportItem.sort_config);
                    }
                }).show();
            }
            break;
        }
    }

    private boolean isRefreshing = true;

    @Override
    public void onRefreshStarted() {
        isRefreshing = true;
        if (getActivity() == null) return;
        isScrollable = false;
//        if (adapter != null) adapter.setData(null);
        if (adapter == null) adapter = new RecyclerViewAdapter<>(getActivity()
                , recyclerView
                , null
                , SPUtil.getGlobalSharedPreferences(getActivity()).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT, Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT_DEFAULT)
                , this);
        else adapter.setData(null, true);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(true);
        viewGroup_no_content.setVisibility(View.GONE);
        viewGroup_progress.setVisibility(View.VISIBLE);
        /*ViewGroup.LayoutParams layoutParams = viewGroup_progress.getLayoutParams();
        layoutParams.height = EnvironmentUtil.dp2px(getActivity(), 160);
        viewGroup_progress.setLayoutParams(layoutParams);
        progressTextView.setText(getActivity().getResources().getString(R.string.att_scanning));
        progressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        progressBar.setIndeterminate(true);*/
        card_multi_select.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgress(boolean canAdd, @NonNull ImportItem importItem) {
        if (getActivity() == null) return;
        if (progressTextView != null) {
            progressTextView.setText(getActivity().getResources().getString(R.string.att_scanning) + " " + importItem.getFileItem().getPath());
        }
//        recyclerView.setItemAnimator(null);
        if (adapter != null && !isSearchMode && canAdd) {
            adapter.updateData(importItem);
        }
    }

    @Override
    public void onRefreshCompleted(List<ImportItem> list) {
        isRefreshing = false;
        if (getActivity() == null) return;
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setEnabled(!adapter.getIsMultiSelectMode());
        /*if (adapter == null) adapter = new RecyclerViewAdapter<>(getActivity()
                , recyclerView
                , list
                , SPUtil.getGlobalSharedPreferences(getActivity()).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT, Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT_DEFAULT)
                , this);
        else adapter.setData(list);*/
        if (adapter != null && !isSearchMode) {
            adapter.setData(list, false);
        }

        viewGroup_no_content.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
        viewGroup_progress.setVisibility(View.GONE);
//        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //if(isSearchMode)adapter.setData(null);
    }

    @Override
    public void onItemClicked(ImportItem importItem, RecyclerViewAdapter.ViewHolder viewHolder, int position) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), PackageDetailActivity.class);
        //intent.putExtra(PackageDetailActivity.EXTRA_IMPORT_ITEM_POSITION,position);
        intent.putExtra(PackageDetailActivity.EXTRA_IMPORT_ITEM_PATH, importItem.getFileItem().getPath());
        ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), new Pair<View, String>(viewHolder.icon, "icon"));
        try {
            ActivityCompat.startActivityForResult(getActivity(), intent, REQUEST_CODE_PACKAGE_DETAIL, compat.toBundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMultiSelectItemChanged(List<ImportItem> selected_items, long length) {
        if (getActivity() == null) return;
        tv_multi_select_head.setText(selected_items.size() + getResources().getString(R.string.unit_item) + "/" + Formatter.formatFileSize(getActivity(), length));
        btn_import.setEnabled(selected_items.size() > 0);
        btn_delete.setEnabled(selected_items.size() > 0);
    }

    @Override
    public void onMultiSelectModeOpened() {
        if (getActivity() == null) return;
        setViewVisibilityWithAnimation(card_multi_select, View.VISIBLE);
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setEnabled(false);
        }
        EnvironmentUtil.hideInputMethod(getActivity());
        if (callback != null) callback.onItemLongClickedAndMultiSelectModeOpened(this);
    }

    @Override
    public void onSearchTaskCompleted(@NonNull List<ImportItem> importItems, @NonNull String keyword) {
        if (getActivity() == null) return;
        if (adapter == null) return;
        swipeRefreshLayout.setRefreshing(false);
        if (isMultiSelectMode()) {
            swipeRefreshLayout.setEnabled(false);
        }
        adapter.setData(importItems);
        adapter.setHighlightKeyword(keyword);
    }

    public void setSearchMode(boolean b) {
        this.isSearchMode = b;
//        if (card_multi_select != null) card_multi_select.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) {
            //swipeRefreshLayout.setEnabled(!b);
            if (b) {
                swipeRefreshLayout.setRefreshing(false);
                swipeRefreshLayout.setEnabled(false);
                viewGroup_progress.setVisibility(View.GONE);
            } else {
                swipeRefreshLayout.setEnabled(true);
                if (isRefreshing) {
                    swipeRefreshLayout.setRefreshing(true);
                    viewGroup_progress.setVisibility(View.VISIBLE);
                }
            }
        }
        if (adapter == null) return;
//        adapter.setMultiSelectMode(false);
        if (b) {
            adapter.setData(null);
        } else {
            adapter.setData(Global.item_list);
        }
        if (!b) adapter.setHighlightKeyword(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PACKAGE_DETAIL && resultCode == Activity.RESULT_OK && data != null) {
            String packagePath = data.getStringExtra(PackageDetailActivity.EXTRA_IMPORT_ITEM_PATH);
            List<ImportItem> importItems = Global.item_list;
            ImportItem deleted;
            Iterator<ImportItem> itemIterator = importItems.iterator();
            while (itemIterator.hasNext()) {
                deleted = itemIterator.next();
                if (TextUtils.equals(deleted.getFileItem().getPath(), packagePath)) {
                    itemIterator.remove();
                    adapter.removeItem(deleted);
                }
            }
        }
    }

    private SearchPackageTask searchPackageTask;

    public void updateSearchModeKeywords(@NonNull String key) {
        if (getActivity() == null) return;
        if (adapter == null) return;
        if (!isSearchMode) return;
        if (searchPackageTask != null) searchPackageTask.setInterrupted();
        searchPackageTask = new SearchPackageTask(Global.item_list, key, this);
        adapter.setData(null);
//        adapter.setMultiSelectMode(false);
//        card_multi_select.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);
        searchPackageTask.start();
    }

    public boolean isSearchMode() {
        return isSearchMode;
    }

    public boolean isMultiSelectMode() {
        return adapter != null && adapter.getIsMultiSelectMode();
    }

    public void closeMultiSelectMode() {
        if (adapter == null) return;
        adapter.setMultiSelectMode(false);
        swipeRefreshLayout.setEnabled(true);
        setViewVisibilityWithAnimation(card_multi_select, View.GONE);
    }

    public void setOperationCallback(OperationCallback callback) {
        this.callback = callback;
    }

    public void sortGlobalListAndRefresh(int value) {
        ImportItem.sort_config = value;
        if (isRefreshing) {
            return;
        }
//        closeMultiSelectMode();
        if (adapter != null) adapter.setData(null);
        swipeRefreshLayout.setRefreshing(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(Global.item_list);
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter != null) {
                            adapter.setData(Global.item_list);
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    public void setViewMode(int mode) {
        if (adapter == null) return;
        adapter.setLayoutManagerAndView(mode);
    }

    private void initView() {
        if (getActivity() == null) return;
        btn_select.setOnClickListener(this);
        btn_more.setOnClickListener(this);
        btn_delete.setOnClickListener(this);
        btn_import.setOnClickListener(this);
        swipeRefreshLayout.setColorSchemeColors(getActivity().getResources().getColor(R.color.colorTitle));
        recyclerView.addOnScrollListener(onScrollListener);
        adapter = new RecyclerViewAdapter<>(getActivity()
                , recyclerView
                , null
                , SPUtil.getGlobalSharedPreferences(getActivity()).getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT, Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT_DEFAULT)
                , this);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (getActivity() == null) return;
                if (isSearchMode) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                /*if (adapter != null && adapter.getIsMultiSelectMode()) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }*/
                new RefreshImportListTask(ImportFragment.this).start();
            }
        });
        new RefreshImportListTask(ImportFragment.this).start();
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
}
