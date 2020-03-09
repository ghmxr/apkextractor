package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

public class CompressExtensionDialog extends AlertDialog implements DialogInterface.OnClickListener, View.OnClickListener{

    private RadioButton ra_zip,ra_xapk,ra_custom;
    private EditText editText;
    private String extension;
    private final OnConfirmedCallback callback;

    public CompressExtensionDialog(@NonNull Context context,@NonNull String extension,@NonNull OnConfirmedCallback callback) {
        super(context);
        this.extension=extension;
        this.callback=callback;
        if(TextUtils.isEmpty(this.extension)){
            this.extension="zip";
        }

        View dialogView= LayoutInflater.from(context).inflate(R.layout.dialog_custom_compressed_extension,null);

        setTitle(context.getResources().getString(R.string.activity_settings_extension));
        setView(dialogView);

        ra_zip=dialogView.findViewById(R.id.compress_zip);
        ra_xapk=dialogView.findViewById(R.id.compress_xapk);
        ra_custom=dialogView.findViewById(R.id.compress_custom);
        editText=dialogView.findViewById(R.id.compress_custom_edit);
        ra_zip.setOnClickListener(this);
        ra_xapk.setOnClickListener(this);
        ra_custom.setOnClickListener(this);
        refreshRadioAndEditView();
        setButton(AlertDialog.BUTTON_POSITIVE, getContext().getResources().getString(R.string.dialog_button_confirm),(DialogInterface.OnClickListener)null);
        setButton(AlertDialog.BUTTON_NEGATIVE,getContext().getResources().getString(R.string.dialog_button_cancel),this);
    }

    @Override
    public void show() {
        super.show();
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ra_custom.isChecked()){
                    extension= editText.getText().toString();
                }
                if(TextUtils.isEmpty(extension)|| !EnvironmentUtil.isALegalFileName(extension)){
                    ToastManager.showToast(getContext(),getContext().getResources().getString(R.string.dialog_compress_extension_empty), Toast.LENGTH_SHORT);
                }else{
                    if(callback!=null)callback.onExtensionConfirmed(extension);
                    cancel();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.compress_zip:{
                extension="zip";
                refreshRadioAndEditView();
            }
            break;
            case R.id.compress_xapk:{
                extension="xapk";
                refreshRadioAndEditView();
            }
            break;
            case R.id.compress_custom:{
                extension="";
                refreshRadioAndEditView();
            }
            break;
            default:break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        cancel();
    }

    public interface OnConfirmedCallback{
        void onExtensionConfirmed(@NonNull String extension);
    }

    private void refreshRadioAndEditView(){
        ra_zip.setChecked(extension.equalsIgnoreCase("zip"));
        ra_xapk.setChecked(extension.equalsIgnoreCase("xapk"));
        final boolean isCustom=!extension.equalsIgnoreCase("zip")&&!extension.equalsIgnoreCase("xapk");
        ra_custom.setChecked(isCustom);
        editText.setEnabled(isCustom);
        editText.setText(isCustom?extension:"");
    }
}
