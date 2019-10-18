package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.AppItem;
import com.github.ghmxr.apkextractor.items.ImportItem;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.util.ArrayList;
import java.util.List;

public class ImportingDataObbDialog extends AlertDialog implements View.OnClickListener{
    private final View view;
    private final List<ImportItem>list;
    private final List<ImportItem> list_data_controlable=new ArrayList<>();
    private final List<ImportItem> list_obb_controlable=new ArrayList<>();
    private CheckBox cb_data;
    private CheckBox cb_obb;
    private CheckBox cb_apk;
    private TextView tv_att;
    private ImportDialogDataObbConfirmedCallback callback;

    public ImportingDataObbDialog(@NonNull Context context, @NonNull List<ImportItem>importItems,@Nullable ImportDialogDataObbConfirmedCallback callback) {
        super(context);
        this.list=importItems;
        this.callback=callback;
        view= LayoutInflater.from(context).inflate(R.layout.dialog_data_obb,null);
        cb_data=view.findViewById(R.id.dialog_checkbox_data);
        cb_obb=view.findViewById(R.id.dialog_checkbox_obb);
        tv_att=view.findViewById(R.id.data_obb_att);
        setView(view);
        setTitle(context.getResources().getString(R.string.dialog_import_data_obb_title));
        tv_att.setText(context.getResources().getString(R.string.dialog_import_data_obb_att));
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
    }

    @Override
    public void show() {
        super.show();
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                long import_data= 0;
                long import_obb=0;
                for(ImportItem importItem:list){
                    try{
                        long data=ZipFileUtil.getDataOrObbSizeOfZipInputStream(importItem.getZipInputStream(),"data");
                        long obb=ZipFileUtil.getDataOrObbSizeOfZipInputStream(importItem.getZipInputStream(),"obb");
                        import_data+=data;
                        import_obb+=obb;
                        if(data>0)list_data_controlable.add(importItem);
                        if(obb>0)list_obb_controlable.add(importItem);
                    }catch (Exception e){e.printStackTrace();}
                    final long total_data=import_data;
                    final long total_obb=import_obb;
                    Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            view.findViewById(R.id.dialog_data_obb_wait_area).setVisibility(View.GONE);
                            view.findViewById(R.id.dialog_data_obb_show_area).setVisibility(View.VISIBLE);
                            cb_data.setEnabled(total_data>0);
                            cb_obb.setEnabled(total_obb>0);
                            cb_data.setText("Data("+ Formatter.formatFileSize(getContext(),total_data)+")");
                            cb_obb.setText("Obb("+Formatter.formatFileSize(getContext(),total_obb)+")");
                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ImportingDataObbDialog.this);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        if(v.equals(getButton(AlertDialog.BUTTON_POSITIVE))){
            if(cb_data.isChecked()) for(ImportItem item:list_data_controlable) item.importData=true;
            if(cb_obb.isChecked()) for (ImportItem item:list_obb_controlable) item.importObb=true;
            if(callback!=null)callback.onImportingDataObbConfirmed(list);
            cancel();
        }
    }

    public interface ImportDialogDataObbConfirmedCallback{
        void onImportingDataObbConfirmed(@NonNull List<ImportItem>importItems);
    }
}
