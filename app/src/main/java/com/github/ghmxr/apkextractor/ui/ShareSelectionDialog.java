package com.github.ghmxr.apkextractor.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.activities.FileSendActivity;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.IpMessage;

import java.util.ArrayList;
import java.util.List;

public class ShareSelectionDialog extends Dialog implements View.OnClickListener {

    private final List<FileItem> fileItems;
    private final Context context;

    public ShareSelectionDialog(@NonNull Context context, @NonNull List<FileItem> fileItems) {
        super(context);
        this.context = context;
        this.fileItems = fileItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_share_function);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.BOTTOM;
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(R.style.DialogAnimStyle);
        }
        findViewById(R.id.dialog_share_direct).setOnClickListener(this);
        findViewById(R.id.dialog_share_system).setOnClickListener(this);
        findViewById(R.id.dialog_share_cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.dialog_share_direct: {
                try {
                    IpMessage testIpMessage = IpMessage.getSendingFileRequestIpMessgae(context, this.fileItems);
                    if (testIpMessage.toProtocolString().length() > 65530) {
                        ToastManager.showToast(context, context.getResources().getString(R.string.info_udp_too_long), Toast.LENGTH_SHORT);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(context, FileSendActivity.class);
                if (!(context instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                FileSendActivity.setSendingFiles(this.fileItems);//FileSendActivity不能写跨进程否则静态方法传值无效
                context.startActivity(intent);
            }
            break;
            case R.id.dialog_share_system: {
                ArrayList<Uri> uris = new ArrayList<>();
                for (FileItem fileItem : fileItems) {
                    if (fileItem.isFileInstance()) {
                        uris.add(Uri.fromFile(fileItem.getFile()));
                    } else if (fileItem.isDocumentFile()) {
                        uris.add(fileItem.getDocumentFile().getUri());
                    } else if (fileItem.isShareUriInstance()) {
                        ToastManager.showToast(context, context.getResources().getString(R.string.info_share_uri_invalid), Toast.LENGTH_SHORT);
                        return;
                    }
                }
                Global.shareCertainFiles(context, uris, context.getResources().getString(R.string.share_title));
            }
            break;
            case R.id.dialog_share_cancel: {
            }
            break;
        }
        cancel();
    }
}
