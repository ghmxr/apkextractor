package com.github.ghmxr.apkextractor.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.util.Calendar;
import java.util.List;

public class FileRenamingDialog extends AlertDialog implements View.OnClickListener, TextWatcher {

    private final List<ImportItem> importItems;
    private final EditText editText;
    private final RecyclerView recyclerView;
    private final ViewGroup vg_warn;
    private final CompletedCallback callback;

    private boolean isAllApk = true;

    @SuppressLint("SetTextI18n")
    public FileRenamingDialog(@NonNull Context context, @NonNull List<ImportItem> importItems, @NonNull CompletedCallback callback) {
        super(context);
        this.importItems = importItems;
        this.callback = callback;

        for (ImportItem importItem : importItems) {
            if (importItem.getImportType() != ImportItem.ImportType.APK) {
                isAllApk = false;
                break;
            }
        }
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);
        editText = dialogView.findViewById(R.id.rename_edit);
        recyclerView = dialogView.findViewById(R.id.filename_result_recyclerview);
        Button btn_A = dialogView.findViewById(R.id.filename_sequence_number);
        Button btn_C = dialogView.findViewById(R.id.filename_versioncode);
        Button btn_middleLine = dialogView.findViewById(R.id.filename_connector);
        Button btn_underline = dialogView.findViewById(R.id.filename_underline);
        Button btn_N = dialogView.findViewById(R.id.filename_appname);
        Button btn_P = dialogView.findViewById(R.id.filename_packagename);
        Button btn_V = dialogView.findViewById(R.id.filename_version);
        Button btn_Y = dialogView.findViewById(R.id.filename_year);
        Button btn_M = dialogView.findViewById(R.id.filename_month);
        Button btn_D = dialogView.findViewById(R.id.filename_day_of_month);
        Button btn_H = dialogView.findViewById(R.id.filename_hour_of_day);
        Button btn_I = dialogView.findViewById(R.id.filename_minute);
        Button btn_S = dialogView.findViewById(R.id.filename_second);
        vg_warn = dialogView.findViewById(R.id.filename_warn);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        editText.addTextChangedListener(this);

        btn_V.setOnClickListener(this);
        btn_P.setOnClickListener(this);
        btn_N.setOnClickListener(this);
        btn_underline.setOnClickListener(this);
        btn_middleLine.setOnClickListener(this);
        btn_C.setOnClickListener(this);
        btn_Y.setOnClickListener(this);
        btn_M.setOnClickListener(this);
        btn_D.setOnClickListener(this);
        btn_H.setOnClickListener(this);
        btn_I.setOnClickListener(this);
        btn_S.setOnClickListener(this);
        btn_A.setOnClickListener(this);

        /*if(!isAllApk){
            btn_C.setVisibility(View.GONE);
            btn_N.setVisibility(View.GONE);
            btn_P.setVisibility(View.GONE);
            btn_V.setVisibility(View.GONE);
        }*/
        if (importItems.size() == 1) {
            editText.setText(EnvironmentUtil.getFileMainName(importItems.get(0).getFileItem().getName()));
        } else {
            editText.setText("NewFile-" + Constants.FONT_AUTO_SEQUENCE_NUMBER);
        }

        setView(dialogView);
        setTitle(context.getResources().getString(R.string.more_file_rename));
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.action_confirm), (OnClickListener) null);
        setButton(AlertDialog.BUTTON_NEGATIVE, context.getResources().getString(R.string.action_cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    private long confirmCheckTime = 0L;

    @Override
    public void show() {
        super.show();
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText() == null || String.valueOf(editText.getText()).trim().equals("")) {
                    ToastManager.showToast(getContext(), getContext().getResources().getString(R.string.dialog_filename_toast_blank), Toast.LENGTH_SHORT);
                    return;
                }
                if (!EnvironmentUtil.isALegalFileName(EnvironmentUtil.getEmptyVariableString(editText.getText().toString()))) {
                    ToastManager.showToast(getContext(), getContext().getResources().getString(R.string.file_invalid_name), Toast.LENGTH_SHORT);
                    return;
                }
                //final String content=editText.getText().toString();
                String toastContent;
                boolean containVariables = isAllApk ? containsAnyVariables() : containSequenceVariable();
                if (!containVariables && importItems.size() > 1) {
                    toastContent = getContext().getResources().getString(R.string.dialog_filename_rename_warn_no_variables_confirm);
                } else {
                    toastContent = getContext().getResources().getString(R.string.dialog_filename_confirm);
                }
                final long currentTime = System.currentTimeMillis();
                if (currentTime - confirmCheckTime > 2000L) {
                    confirmCheckTime = currentTime;
                    ToastManager.showToast(getContext(), toastContent, Toast.LENGTH_SHORT);
                    return;
                }

                final StringBuilder errorBuilder = new StringBuilder();
                for (int i = 0; i < importItems.size(); i++) {
                    final ImportItem importItem = importItems.get(i);
                    final String initialName = importItem.getFileItem().getName();
                    final String newName = getPreviewRenamedFileName(i);
                    try {
                        if (!importItem.renameFileItemTo(newName)) {
                            errorBuilder.append(initialName);
                            errorBuilder.append(":");
                            errorBuilder.append("renaming failed for returning value is false");
                            errorBuilder.append("\n\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorBuilder.append(initialName);
                        errorBuilder.append(":");
                        errorBuilder.append(e);
                        errorBuilder.append("\n\n");
                    }
                }
                cancel();
                if (callback != null) callback.onCompleted(errorBuilder.toString());
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        recyclerView.setAdapter(new ListAdapter());
        if ((isAllApk ? !containsAnyVariables() : !containSequenceVariable()) && importItems.size() > 1) {
            vg_warn.setVisibility(View.VISIBLE);
        } else {
            vg_warn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.filename_versioncode: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_APP_VERSIONCODE);
            }
            break;
            case R.id.filename_connector: {
                editText.getText().insert(editText.getSelectionStart(), "-");
            }
            break;
            case R.id.filename_underline: {
                editText.getText().insert(editText.getSelectionStart(), "_");
            }
            break;
            case R.id.filename_appname: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_APP_NAME);
            }
            break;
            case R.id.filename_packagename: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_APP_PACKAGE_NAME);
            }
            break;
            case R.id.filename_version: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_APP_VERSIONNAME);
            }
            break;
            case R.id.filename_year: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_YEAR);
            }
            break;
            case R.id.filename_month: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_MONTH);
            }
            break;
            case R.id.filename_day_of_month: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_DAY_OF_MONTH);
            }
            break;
            case R.id.filename_hour_of_day: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_HOUR_OF_DAY);
            }
            break;
            case R.id.filename_minute: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_MINUTE);
            }
            break;
            case R.id.filename_second: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_SECOND);
            }
            break;
            case R.id.filename_sequence_number: {
                editText.getText().insert(editText.getSelectionStart(), Constants.FONT_AUTO_SEQUENCE_NUMBER);
            }
            break;
        }
    }

    private String getCurrentFileName(int position) {
        return importItems.get(position).getFileItem().getName();
    }

    private String getPreviewRenamedFileName(int position) {
        final ImportItem importItem = importItems.get(position);
        final PackageInfo packageInfo = importItem.getPackageInfo();
        String value = editText.getText().toString();
        value = value.replace(Constants.FONT_APP_NAME, packageInfo != null ? importItem.getApkLabel() : "");
        value = value.replace(Constants.FONT_APP_PACKAGE_NAME, packageInfo != null ? String.valueOf(packageInfo.packageName) : "");
        value = value.replace(Constants.FONT_APP_VERSIONNAME, packageInfo != null ? String.valueOf(packageInfo.versionName) : "");
        value = value.replace(Constants.FONT_APP_VERSIONCODE, packageInfo != null ? String.valueOf(packageInfo.versionCode) : "");
        value = value.replace(Constants.FONT_YEAR, EnvironmentUtil.getCurrentTimeValue(Calendar.YEAR));
        value = value.replace(Constants.FONT_MONTH, EnvironmentUtil.getCurrentTimeValue(Calendar.MONTH));
        value = value.replace(Constants.FONT_DAY_OF_MONTH, EnvironmentUtil.getCurrentTimeValue(Calendar.DAY_OF_MONTH));
        value = value.replace(Constants.FONT_HOUR_OF_DAY, EnvironmentUtil.getCurrentTimeValue(Calendar.HOUR_OF_DAY));
        value = value.replace(Constants.FONT_MINUTE, EnvironmentUtil.getCurrentTimeValue(Calendar.MINUTE));
        value = value.replace(Constants.FONT_SECOND, EnvironmentUtil.getCurrentTimeValue(Calendar.SECOND));
        value = value.replace(Constants.FONT_AUTO_SEQUENCE_NUMBER, String.valueOf(position));
        value = value + "." + EnvironmentUtil.getFileExtensionName(importItem.getItemName());
        return value;
    }

    private boolean containsAnyVariables() {
        return editText.getText().toString().contains(Constants.FONT_APP_NAME) || editText.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                || editText.getText().toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER);
    }

    private boolean containSequenceVariable() {
        return editText.getText().toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER);
    }

    private static final String FILE_RENAME_ARROW = " -> ";

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            TextView textView = new TextView(getContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            textView.setTextColor(getContext().getResources().getColor(R.color.color_text_normal));
//            ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            textView.setLayoutParams(layoutParams);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            final String oldName = getCurrentFileName(viewHolder.getAdapterPosition());
            final String newName = getPreviewRenamedFileName(viewHolder.getAdapterPosition());
            SpannableStringBuilder builder = new SpannableStringBuilder(oldName
                    + FILE_RENAME_ARROW
                    + newName
                    + "\n\n");
            builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.color_text_normal)), 0, oldName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.colorAccent)), oldName.length(), oldName.length() + FILE_RENAME_ARROW.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.colorFirstAttention)), oldName.length() + FILE_RENAME_ARROW.length(), builder.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewHolder.textView.setText(builder);
        }

        @Override
        public int getItemCount() {
            return importItems.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        ViewHolder(@NonNull TextView textView) {
            super(textView);
            this.textView = textView;
        }
    }

    public interface CompletedCallback {
        void onCompleted(@NonNull String errorInfo);
    }
}
