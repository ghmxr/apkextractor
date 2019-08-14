package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Global;

public class ToastManager {

    private static Toast toast;

    public static void showToast(@NonNull final Context context, @NonNull final String content, final int length){
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(toast!=null){
                    toast.cancel();
                    toast=null;
                }
                toast=Toast.makeText(context,content,length);
                toast.show();
            }
        });
    }

}
