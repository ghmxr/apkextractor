package com.github.ghmxr.apkextractor.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.DeviceItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.net.IpMessageConstants;
import com.github.ghmxr.apkextractor.net.NetSendTask;
import com.github.ghmxr.apkextractor.ui.FileTransferringDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;

import java.util.ArrayList;
import java.util.List;

public class FileSendActivity extends BaseActivity implements NetSendTask.NetSendTaskCallback {

    private NetSendTask netSendTask;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;

    private static final ArrayList<FileItem>sendingFiles=new ArrayList<>();
    private AlertDialog wait_resp_diag;
    private FileTransferringDialog sendingDiag;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_file_send);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        swipeRefreshLayout=findViewById(R.id.activity_file_send_root);
        recyclerView=findViewById(R.id.activity_file_send_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        try{
            netSendTask=new NetSendTask(this,this);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                netSendTask.sendRequestOnlineDevicesBroadcast();
            }
        });
    }

    @Override
    public void onOnlineDevicesChanged(@NonNull List<DeviceItem> devices) {
        swipeRefreshLayout.setRefreshing(false);
        recyclerView.setAdapter(new ListAdapter(devices));
    }

    @Override
    public void onSendingFilesRequestRefused() {
        if(wait_resp_diag!=null&&wait_resp_diag.isShowing()){
            wait_resp_diag.cancel();
            wait_resp_diag=null;
        }
    }

    @Override
    public void onFileSendingInterrupted() {
        if(sendingDiag!=null&&sendingDiag.isShowing()){
            sendingDiag.cancel();
            sendingDiag=null;
        }
    }

    @Override
    public void onFileSendingStarted(@NonNull NetSendTask task) {
        if(wait_resp_diag!=null&&wait_resp_diag.isShowing()){
            wait_resp_diag.cancel();
            wait_resp_diag=null;
        }
        if(sendingDiag==null||!sendingDiag.isShowing()){
            sendingDiag=new FileTransferringDialog(this,getResources().getString(R.string.dialog_send_title));
            sendingDiag.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    netSendTask.sendStoppingSendingFilesCommand();
                }
            });
            sendingDiag.show();
        }
    }

    @Override
    public void onProgress(long progress, long total, String currentFile) {
        sendingDiag.setProgressOfSending(progress,total);
        sendingDiag.setCurrentFileInfo(getResources().getString(R.string.dialog_send_att_head)+currentFile);
    }

    @Override
    public void onSendingSpeed(long bytesOfSpeed) {
        sendingDiag.setSpeed(bytesOfSpeed);
    }

    @Override
    public void onFileSendCompleted() {
        sendingDiag.cancel();
        sendingDiag=null;
        ToastManager.showToast(this,getResources().getString(R.string.toast_sending_complete),Toast.LENGTH_SHORT);
    }

    public static void setSendingFiles(@NonNull List<FileItem>fileItems){
        sendingFiles.clear();
        sendingFiles.addAll(fileItems);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case android.R.id.home:{
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish(){
        super.finish();
        sendingFiles.clear();
        netSendTask.stopTask();
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder>{
        private List<DeviceItem>list;
        private ListAdapter(List<DeviceItem>list){
            this.list=list;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(FileSendActivity.this).inflate(R.layout.item_device,viewGroup,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            final DeviceItem deviceItem=list.get(viewHolder.getAdapterPosition());
            viewHolder.tv_device_name.setText(deviceItem.getDeviceName());
            viewHolder.tv_ip.setText(deviceItem.getIp());
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    netSendTask.sendFileRequestIpMessage(sendingFiles,deviceItem.getIp());
                    wait_resp_diag=new AlertDialog.Builder(FileSendActivity.this)
                            .setTitle(getResources().getString(R.string.dialog_send_title))
                            .setMessage(getResources().getString(R.string.dialog_send_request_head1)+deviceItem.getIp()+
                                    getResources().getString(R.string.dialog_send_request_head2))
                            .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    netSendTask.sendIpMessageByCommandToTargetIp(IpMessageConstants.MSG_SEND_FILE_CANCELED,deviceItem.getIp());
                                }
                            }).setCancelable(false)
                            .create();
                    wait_resp_diag.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder{
        TextView tv_device_name,tv_ip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_device_name=itemView.findViewById(R.id.item_device_name);
            tv_ip=itemView.findViewById(R.id.item_device_ip);
        }

    }
}
