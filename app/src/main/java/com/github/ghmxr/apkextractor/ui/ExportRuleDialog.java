package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.Constants;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

public class ExportRuleDialog extends AlertDialog implements View.OnClickListener ,TextWatcher,DialogInterface.OnClickListener{


    private EditText edit_apk,edit_zip;
    private TextView preview;
    private Spinner spinner;

    private SharedPreferences settings;

    /**
     * 编辑导出规则的UI，确定后会保存至SharedPreferences中
     */
    public ExportRuleDialog(Context context){
        super(context);

        settings= Global.getGlobalSharedPreferences(context);

        final View dialogView= LayoutInflater.from(context).inflate(R.layout.dialog_rule,null);
        edit_apk=(EditText)dialogView.findViewById(R.id.filename_apk);
        edit_zip=(EditText)dialogView.findViewById(R.id.filename_zip);
        preview=((TextView)dialogView.findViewById(R.id.filename_preview));
        spinner=((Spinner)dialogView.findViewById(R.id.spinner_zip_level));

        edit_apk.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT));
        edit_zip.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT));
        preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
        spinner.setAdapter(new ArrayAdapter<String>(context,R.layout.item_spinner_single_text,R.id.spinner_text,new String[]{context.getResources().getString(R.string.zip_level_default),
                getContext().getResources().getString(R.string.zip_level_stored),context.getResources().getString(R.string.zip_level_low),context.getResources().getString(R.string.zip_level_normal)
                ,getContext().getResources().getString(R.string.zip_level_high)}));

        int level_set=settings.getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT);
        try{
            switch(level_set){
                default:spinner.setSelection(0);break;
                case Constants.ZIP_LEVEL_STORED:spinner.setSelection(1);break;
                case Constants.ZIP_LEVEL_LOW:spinner.setSelection(2);break;
                case Constants.ZIP_LEVEL_NORMAL:spinner.setSelection(3);break;
                case Constants.ZIP_LEVEL_HIGH:spinner.setSelection(4);break;
            }
        }catch(Exception e){e.printStackTrace();}

        if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                &&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
            dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
        }else{
            dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
        }

        if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                &&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
            dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.VISIBLE);
        }else{
            dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.GONE);
        }
        setTitle(context.getResources().getString(R.string.dialog_filename_title));
        setView(dialogView);
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), this);
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),this);

        edit_apk.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
                if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                        &&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
                    dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
                }else{
                    dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
                }
            }

        });
        edit_zip.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
                if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                        &&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
                    dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.VISIBLE);
                }else{
                    dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.GONE);
                }
            }

        });

        dialogView.findViewById(R.id.filename_appname).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_packagename).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_version).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_versioncode).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_connector).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_upderline).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            default:break;
            case R.id.filename_appname:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_NAME);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_NAME);
                }
            }
            break;
            case R.id.filename_packagename:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_PACKAGE_NAME);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_PACKAGE_NAME);
                }
            }
            break;
            case R.id.filename_version:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_VERSIONNAME);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_VERSIONNAME);
                }
            }
            break;
            case R.id.filename_versioncode:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_VERSIONCODE);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_VERSIONCODE);
                }
            }
            break;
            case R.id.filename_connector:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), "-");
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), "-");
                }
            }
            break;
            case R.id.filename_upderline:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), "_");
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), "_");
                }
            }
            break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SharedPreferences.Editor editor=settings.edit();
        switch (which){
            default:break;
            case AlertDialog.BUTTON_POSITIVE:{
                if(edit_apk.getText().toString().trim().equals("")||edit_zip.getText().toString().trim().equals("")){
                    ToastManager.showToast(getContext(),getContext().getResources().getString(R.string.dialog_filename_toast_blank), Toast.LENGTH_SHORT);
                    return;
                }

                String apk_replaced_variables=edit_apk.getText().toString().replace(Constants.FONT_APP_NAME, "").replace(Constants.FONT_APP_PACKAGE_NAME, "").replace(Constants.FONT_APP_VERSIONCODE, "").replace(Constants.FONT_APP_VERSIONNAME, "");
                String zip_replaced_variables=edit_zip.getText().toString().replace(Constants.FONT_APP_NAME, "").replace(Constants.FONT_APP_PACKAGE_NAME, "").replace(Constants.FONT_APP_VERSIONCODE, "").replace(Constants.FONT_APP_VERSIONNAME, "");
                if(!EnvironmentUtil.isALegalFileName(apk_replaced_variables)||!EnvironmentUtil.isALegalFileName(zip_replaced_variables)){
                    ToastManager.showToast(getContext(),getContext().getResources().getString(R.string.file_invalid_name),Toast.LENGTH_SHORT);
                    return;
                }
                editor.putString(Constants.PREFERENCE_FILENAME_FONT_APK, edit_apk.getText().toString());
                editor.putString(Constants.PREFERENCE_FILENAME_FONT_ZIP, edit_zip.getText().toString());
                int zip_selection=spinner.getSelectedItemPosition();
                switch(zip_selection){
                    default:break;
                    case 0:editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT);break;
                    case 1:editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_STORED);break;
                    case 2:editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_LOW);break;
                    case 3:editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_NORMAL);break;
                    case 4:editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_HIGH);break;
                }
                editor.apply();

            }
            break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private String getFormatedExportFileName(String apk, String zip){
        final String PREVIEW_APPNAME=getContext().getResources().getString(R.string.dialog_filename_preview_appname);
        final String PREVIEW_PACKAGENAME=getContext().getResources().getString(R.string.dialog_filename_preview_packagename);
        final String PREVIEW_VERSION=getContext().getResources().getString(R.string.dialog_filename_preview_version);
        final String PREVIEW_VERSIONCODE=getContext().getResources().getString(R.string.dialog_filename_preview_versioncode);
        return getContext().getResources().getString(R.string.word_preview)+":\n\nAPK:  "+apk.replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME)
                .replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME).replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE).replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION)+".apk\n\n"
                +"ZIP:  "+zip.replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME)
                .replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME).replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE).replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION)+".zip";
    }
}
