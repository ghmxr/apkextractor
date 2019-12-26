package com.github.ghmxr.apkextractor.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.activities.FileSendActivity;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.IpMessage;

import java.util.ArrayList;
import java.util.List;

public class ShareSelectionDialog extends Dialog implements View.OnClickListener{

    private final List<FileItem>fileItems;

    public ShareSelectionDialog(@NonNull Context context, @NonNull List<FileItem>fileItems){
        super(context);
        this.fileItems=fileItems;
        View dialogView=LayoutInflater.from(context).inflate(R.layout.dialog_share_function,null);
        setContentView(dialogView);
        Window window=getWindow();
        if(window!=null){
            WindowManager.LayoutParams layoutParams=window.getAttributes();
            layoutParams.gravity= Gravity.BOTTOM;
            layoutParams.width= WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height=WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
            window.setWindowAnimations(R.style.DialogAnimStyle);
        }

        dialogView.findViewById(R.id.dialog_share_direct).setOnClickListener(this);
        dialogView.findViewById(R.id.dialog_share_system).setOnClickListener(this);
        dialogView.findViewById(R.id.dialog_share_cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            default:break;
            case R.id.dialog_share_direct:{
                try{
                    IpMessage testIpMessage= IpMessage.getSendingFileRequestIpMessgae(getContext(),this.fileItems);
                    if(testIpMessage.toProtocolString().length()>65530){
                        ToastManager.showToast(getContext(),getContext().getResources().getString(R.string.info_udp_too_long),Toast.LENGTH_SHORT);
                        return;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                Intent intent=new Intent(getContext(), FileSendActivity.class);
                FileSendActivity.setSendingFiles(this.fileItems);
                getContext().startActivity(intent);
            }
            break;
            case R.id.dialog_share_system:{
                ArrayList<Uri>uris=new ArrayList<>();
                for(FileItem fileItem:fileItems){
                    if(fileItem.isFileInstance()){
                        uris.add(Uri.fromFile(fileItem.getFile()));
                    }else if(fileItem.isDocumentFile()){
                        uris.add(fileItem.getDocumentFile().getUri());
                    }
                }
                Global.shareCertainFiles(getContext(),uris,getContext().getResources().getString(R.string.share_title));
            }
            break;
            case R.id.dialog_share_cancel:{}
            break;
        }
        cancel();
    }
}
