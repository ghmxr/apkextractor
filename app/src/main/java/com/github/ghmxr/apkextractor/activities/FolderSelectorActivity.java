package com.github.ghmxr.apkextractor.activities;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FolderSelectorActivity extends BaseActivity {

    private SharedPreferences settings;
    private FileItem fileItem;
    private final Bundle positions = new Bundle();

    private ListView listView;
    private ViewGroup group_storages;
    private ViewGroup item_others;
    private ProgressBar progressBar;
    private ViewGroup attention;
    private TextView textView;
    private String current_storage_path = "";

    private final LinkedList<String> segments = new LinkedList<>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_folder_selector);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_folder_selector));
        listView = findViewById(R.id.folder_selector_listview);
        group_storages = findViewById(R.id.folder_selector_storage_selection);
        item_others = findViewById(R.id.item_other);
        progressBar = findViewById(R.id.folder_selector_loading);
        attention = findViewById(R.id.folder_selector_att);
        textView = findViewById(R.id.folder_selector_current_path);
        listView.setDivider(null);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
        }

        try {
            getSupportActionBar().setTitle(getResources().getString(R.string.activity_folder_selector_title));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (DocumentFileUtil.canWrite2ExternalStorage(this) && Build.VERSION.SDK_INT >= 21) {
            item_others.setVisibility(View.VISIBLE);
        } else item_others.setVisibility(View.GONE);

        findViewById(R.id.item_internal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshList(new FileItem(StorageUtil.getMainExternalStoragePath()));
            }
        });
        findViewById(R.id.item_external).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (DocumentFileUtil.canWrite2ExternalStorage(FolderSelectorActivity.this)) {
                        try {
                            refreshList(new FileItem(FolderSelectorActivity.this, Uri.parse(SPUtil.getExternalStorageUri(FolderSelectorActivity.this)), null));
                        } catch (Exception e) {
                            e.printStackTrace();
                            restore2DefaultStoragePath();
                            ToastManager.showToast(FolderSelectorActivity.this, "Getting external storage environment error", Toast.LENGTH_SHORT);
                        }
                    } else {
                        showSelectingStorageDialog();
                    }
                } else {
                    fileItem = null;
                    List<String> storages = StorageUtil.getAvailableStoragePaths(FolderSelectorActivity.this);
                    ArrayList<FileItem> arrayList = new ArrayList<>();
                    for (String s : storages) {
                        arrayList.add(new FileItem(s));
                    }
                    listView.setVisibility(View.VISIBLE);
                    group_storages.setVisibility(View.GONE);
                    BasicListAdapter adapter = new BasicListAdapter(arrayList);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(adapter);
                }
            }
        });

        findViewById(R.id.item_other).setOnClickListener(new View.OnClickListener() {
            @Override
            @TargetApi(21)
            public void onClick(View v) {
                showSelectingStorageDialog();
            }
        });
        boolean external = SPUtil.getIsSaved2ExternalStorage(this);
        settings = SPUtil.getGlobalSharedPreferences(this);
        if (external) {
            try {
                String segments = SPUtil.getSaveSegment(this);
                fileItem = new FileItem(this, Uri.parse(SPUtil.getExternalStorageUri(this)), segments);
                if (segments != null) this.segments.addAll(Arrays.asList(segments.split("/")));
                else this.segments.clear();
                current_storage_path = DocumentFile.fromTreeUri(this, Uri.parse(SPUtil.getExternalStorageUri(this))).getUri().getPath();
            } catch (Exception e) {
                e.printStackTrace();
                ToastManager.showToast(this, "Initializing external storage error", Toast.LENGTH_SHORT);
                fileItem = new FileItem(StorageUtil.getMainExternalStoragePath());
                current_storage_path = StorageUtil.getMainExternalStoragePath();
            }
        } else {
            fileItem = new FileItem(settings.getString(Constants.PREFERENCE_SAVE_PATH, Constants.PREFERENCE_SAVE_PATH_DEFAULT));
            try {
                if (fileItem.exists() && !fileItem.isDirectory()) fileItem.delete();
                if (!fileItem.exists()) fileItem.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
                ToastManager.showToast(this, getResources().getString(R.string.toast_mkdirs_error) + "\n" + e.toString(), Toast.LENGTH_SHORT);
            }
            List<String> storage_paths = StorageUtil.getAvailableStoragePaths(this);
            for (String s : storage_paths) {
                if (fileItem.getPath().toLowerCase().startsWith(s.toLowerCase())) {
                    current_storage_path = s;
                    break;
                }
            }
        }

        refreshList(fileItem);
    }


    private void refreshList(@Nullable final FileItem fileItem) {
        //recyclerView.setAdapter(null);
        this.fileItem = fileItem;
        listView.setAdapter(null);
        attention.setVisibility(View.GONE);
        if (fileItem == null) {
            textView.setText(getResources().getString(R.string.activity_folder_selector_select_storage));
            listView.setVisibility(View.GONE);
            group_storages.setVisibility(View.VISIBLE);
            attention.setVisibility(View.GONE);
        } else {
            group_storages.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (FolderSelectorActivity.this) {
                        try {
                            final List<FileItem> list = new ArrayList<>();
                            List<FileItem> result = fileItem.listFileItems();
                            for (FileItem f : result) {
                                if (f.isDirectory() && !f.isHidden()) list.add(f);
                            }
                            Collections.sort(list);
                            Global.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    //recyclerView.setAdapter(new ListAdapter(arrayList));
                                    if (list.size() == 0) attention.setVisibility(View.VISIBLE);
                                    BasicListAdapter adapter = new BasicListAdapter(list);
                                    listView.setAdapter(adapter);
                                    listView.setOnItemClickListener(adapter);
                                    if (FolderSelectorActivity.this.fileItem != null) {
                                        //recyclerView.scrollTo(0,positions.getInt(getFormateLowercaseString(FolderSelectorActivity.this.file.getAbsolutePath())));
                                        listView.setSelection(positions.getInt(getFormateLowercaseString(FolderSelectorActivity.this.fileItem.getPath())));
                                    }
                                    if (fileItem.isDocumentFile())
                                        textView.setText(DocumentFileUtil.getDisplayPathForDocumentFile(FolderSelectorActivity.this, fileItem.getDocumentFile()));
                                    else textView.setText(fileItem.getPath());
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    private void showSelectingStorageDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.activity_folder_selector_external_title))
                .setMessage(getResources().getString(R.string.activity_folder_selector_external_message))
                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    @TargetApi(21)
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1);
                        ToastManager.showToast(FolderSelectorActivity.this, getResources().getString(R.string.activity_folder_selector_external_message), Toast.LENGTH_SHORT);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void showSelectingErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.activity_folder_selector_external_title))
                .setMessage(getResources().getString(R.string.activity_folder_selector_external_error))
                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();

    }

    private void restore2DefaultStoragePath() {
        fileItem = new FileItem(StorageUtil.getMainExternalStoragePath());
        current_storage_path = StorageUtil.getMainExternalStoragePath();
        refreshList(fileItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_folder_selector, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
            case android.R.id.home: {
                backToParentOrExit();
            }
            break;
            case R.id.action_confirm: {
                if (fileItem == null) {
                    ToastManager.showToast(this, getResources().getString(R.string.activity_attention_select_folder), Toast.LENGTH_SHORT);
                    return false;
                }
                SharedPreferences.Editor editor = settings.edit();
                if (fileItem.isDocumentFile()) {
                    //editor.putString(Constants.PREFERENCE_SAVE_PATH_URI,DocumentFile.fromTreeUri(this,Uri.parse(fileItem.getAbsolutePathOrUri())).getUri().toString());
                    editor.putBoolean(Constants.PREFERENCE_STORAGE_PATH_EXTERNAL, true);
                    if (segments.size() > 0) {
                        editor.putString(Constants.PREFERENCE_SAVE_PATH_SEGMENT, DocumentFileUtil.toSegmentString(segments.toArray()));
                    } else {
                        editor.remove(Constants.PREFERENCE_SAVE_PATH_SEGMENT);
                    }
                } else {
                    editor.putBoolean(Constants.PREFERENCE_STORAGE_PATH_EXTERNAL, false);
                    editor.putString(Constants.PREFERENCE_SAVE_PATH, fileItem.getPath());
                    editor.remove(Constants.PREFERENCE_SAVE_PATH_SEGMENT);
                }
                //editor.putBoolean(Constants.PREFERENCE_SAVE_PATH_EXTERNAL,fileItem.isDocumentFile());
                editor.apply();
                setResult(RESULT_OK);
                //ToastManager.showToast(this,getResources().getString(R.string.activity_attention_path_set)+fileItem.getPath(),Toast.LENGTH_SHORT);
                sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
                finish();
            }
            break;
            case R.id.action_cancel: {
                setResult(RESULT_CANCELED);
                finish();
            }
            break;
            case R.id.action_new_folder: {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_edittext, null);
                final EditText editText = dialogView.findViewById(R.id.dialog_edit_text);

                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.action_new_folder))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), null)
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String s = editText.getText().toString().trim();
                        if (!EnvironmentUtil.isALegalFileName(s)) {
                            ToastManager.showToast(FolderSelectorActivity.this, getResources().getString(R.string.file_invalid_name), Toast.LENGTH_SHORT);
                            return;
                        }
                        if (s.length() == 0) {
                            ToastManager.showToast(FolderSelectorActivity.this, getResources().getString(R.string.file_blank_name), Toast.LENGTH_SHORT);
                            return;
                        }
                        try {
                            if (fileItem.isDocumentFile()) {
                                fileItem.createDirectory(s);
                            } else {
                                FileItem fileItem = new FileItem(FolderSelectorActivity.this.fileItem.getPath() + File.separator + s);
                                fileItem.mkdirs();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        refreshList(FolderSelectorActivity.this.fileItem);
                        dialog.cancel();
                    }
                });
            }
            break;
            case R.id.action_name_ascend: {
                FileItem.setSort_config(0);
                refreshList(fileItem);
            }
            break;
            case R.id.action_name_descend: {
                FileItem.setSort_config(1);
                refreshList(fileItem);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @TargetApi(19)
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data == null) return;
            Uri uri = data.getData();
            if (uri == null) return;
            String uri_value = uri.getPath();
            if (uri_value == null) return;
            if (!uri_value.endsWith(":") || uri_value.contains("primary")) {
                // ToastManager.showToast(this,"Please select an available external storage",Toast.LENGTH_SHORT);
                showSelectingErrorDialog();
                return;
            }
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            SPUtil.getGlobalSharedPreferences(this).edit().putString(Constants.PREFERENCE_SAVE_PATH_URI, uri.toString()).apply();
            item_others.setVisibility(View.VISIBLE);
            try {
                refreshList(new FileItem(this, uri, null));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backToParentOrExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void backToParentOrExit() {
        try {
            if (group_storages.getVisibility() == View.VISIBLE) {
                finish();
                return;
            }
            FileItem parentFile = fileItem.getParent();
            if (Build.VERSION.SDK_INT < 21) {
                refreshList(parentFile);
                segments.clear();
                return;
            }
            if (parentFile != null && parentFile.isFileInstance() && parentFile.getPath().length() < current_storage_path.length()) {
                refreshList(null);
                segments.clear();
                return;
            }
            if (fileItem != null && fileItem.isDocumentFile()) {
                if (segments.size() > 0) segments.removeLast();
            }
            refreshList(parentFile);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private String getFormateLowercaseString(String s) {
        return s.trim().toLowerCase();
    }

    private class BasicListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        private List<FileItem> list;

        private BasicListAdapter(@NonNull List<FileItem> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(FolderSelectorActivity.this).inflate(R.layout.item_folder, parent, false);
                holder = new ViewHolder();
                holder.imageView = convertView.findViewById(R.id.item_folder_icon);
                holder.textView = convertView.findViewById(R.id.item_folder_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FileItem item = list.get(position);

            holder.imageView.setImageResource(R.drawable.icon_folder);

            holder.textView.setText(item.getName());
            return convertView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            FileItem item = this.list.get(position);
            String key;
            if (FolderSelectorActivity.this.fileItem == null) {
                key = "??";
                current_storage_path = item.getPath();
            } else {
                key = getFormateLowercaseString(FolderSelectorActivity.this.fileItem.getPath());
            }
            positions.putInt(key, listView.getFirstVisiblePosition());
            if (item.isDocumentFile()) {
                segments.addLast(item.getName());
            }
            refreshList(item);
        }
    }

    private static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }

}
