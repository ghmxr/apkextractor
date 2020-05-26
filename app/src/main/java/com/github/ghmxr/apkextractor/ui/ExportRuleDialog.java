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

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.util.Calendar;

public class ExportRuleDialog extends AlertDialog implements View.OnClickListener,DialogInterface.OnClickListener{


    private EditText edit_apk,edit_zip;
    private TextView preview;
    private Spinner spinner;

    private SharedPreferences settings;

    /**
     * 编辑导出规则的UI，确定后会保存至SharedPreferences中
     */
    public ExportRuleDialog(Context context){
        super(context);

        settings= SPUtil.getGlobalSharedPreferences(context);

        final View dialogView= LayoutInflater.from(context).inflate(R.layout.dialog_rule,null);
        edit_apk= dialogView.findViewById(R.id.filename_apk);
        edit_zip= dialogView.findViewById(R.id.filename_zip);
        preview= dialogView.findViewById(R.id.filename_preview);
        spinner= dialogView.findViewById(R.id.spinner_zip_level);

        ((TextView)dialogView.findViewById(R.id.filename_zip_end)).setText("."+SPUtil.getCompressingExtensionName(context));
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
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), (DialogInterface.OnClickListener)null);
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),this);

        edit_apk.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
                if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                        &&!edit_apk.getText().toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)){
                    dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
                }else{
                    dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
                }
            }

        });
        edit_zip.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
                if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                &&!edit_zip.getText().toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)){
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
        dialogView.findViewById(R.id.filename_underline).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_year).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_month).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_day_of_month).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_hour_of_day).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_minute).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_second).setOnClickListener(this);
        dialogView.findViewById(R.id.filename_sequence_number).setOnClickListener(this);
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
            case R.id.filename_underline:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(), "_");
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), "_");
                }
            }
            break;
            case R.id.filename_year:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_YEAR);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_YEAR);
                }
            }
            break;
            case R.id.filename_month:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_MONTH);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_MONTH);
                }
            }
            break;
            case R.id.filename_day_of_month:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_DAY_OF_MONTH);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_DAY_OF_MONTH);
                }
            }
            break;
            case R.id.filename_hour_of_day:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_HOUR_OF_DAY);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_HOUR_OF_DAY);
                }
            }
            break;
            case R.id.filename_minute:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_MINUTE);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_MINUTE);
                }
            }
            break;
            case R.id.filename_second:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_SECOND);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_SECOND);
                }
            }
            break;
            case R.id.filename_sequence_number:{
                if(edit_apk.isFocused()){
                    edit_apk.getText().insert(edit_apk.getSelectionStart(),Constants.FONT_AUTO_SEQUENCE_NUMBER);
                }
                if(edit_zip.isFocused()){
                    edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_AUTO_SEQUENCE_NUMBER);
                }
            }
            break;
        }
    }

    @Override
    public void show(){
        super.show();
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edit_apk.getText().toString().trim().equals("")||edit_zip.getText().toString().trim().equals("")){
                    ToastManager.showToast(getContext(),getContext().getResources().getString(R.string.dialog_filename_toast_blank), Toast.LENGTH_SHORT);
                    return;
                }

                final String apk_replaced_variables=EnvironmentUtil.getEmptyVariableString(edit_apk.getText().toString());
                final String zip_replaced_variables=EnvironmentUtil.getEmptyVariableString(edit_zip.getText().toString());
                if(!EnvironmentUtil.isALegalFileName(apk_replaced_variables)||!EnvironmentUtil.isALegalFileName(zip_replaced_variables)){
                    ToastManager.showToast(getContext(),getContext().getResources().getString(R.string.file_invalid_name),Toast.LENGTH_SHORT);
                    return;
                }

                SharedPreferences.Editor editor=settings.edit();
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
                cancel();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {}

    private String getFormatedExportFileName(String apk, String zip){
        return getContext().getResources().getString(R.string.word_preview)+":\n\nAPK:  "+getReplacedString(apk)+".apk\n\n"
                +getContext().getResources().getString(R.string.word_compressed)+":  "+getReplacedString(zip)+"."
                +SPUtil.getCompressingExtensionName(getContext());
    }

    private String getReplacedString(String value){
        final String PREVIEW_APPNAME=getContext().getResources().getString(R.string.dialog_filename_preview_appname);
        final String PREVIEW_PACKAGENAME=getContext().getResources().getString(R.string.dialog_filename_preview_packagename);
        final String PREVIEW_VERSION=getContext().getResources().getString(R.string.dialog_filename_preview_version);
        final String PREVIEW_VERSIONCODE=getContext().getResources().getString(R.string.dialog_filename_preview_versioncode);
        value=value.replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME);
        value=value.replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME);
        value=value.replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION);
        value=value.replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE);
        value=value.replace(Constants.FONT_YEAR,EnvironmentUtil.getCurrentTimeValue(Calendar.YEAR));
        value=value.replace(Constants.FONT_MONTH,EnvironmentUtil.getCurrentTimeValue(Calendar.MONTH));
        value=value.replace(Constants.FONT_DAY_OF_MONTH,EnvironmentUtil.getCurrentTimeValue(Calendar.DAY_OF_MONTH));
        value=value.replace(Constants.FONT_HOUR_OF_DAY,EnvironmentUtil.getCurrentTimeValue(Calendar.HOUR_OF_DAY));
        value=value.replace(Constants.FONT_MINUTE,EnvironmentUtil.getCurrentTimeValue(Calendar.MINUTE));
        value=value.replace(Constants.FONT_SECOND,EnvironmentUtil.getCurrentTimeValue(Calendar.SECOND));
        value=value.replace(Constants.FONT_AUTO_SEQUENCE_NUMBER,String.valueOf(2));
        return value;
    }
}
