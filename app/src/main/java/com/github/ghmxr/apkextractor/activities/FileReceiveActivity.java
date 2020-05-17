package com.github.ghmxr.apkextractor.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.net.NetReceiveTask;
import com.github.ghmxr.apkextractor.ui.FileTransferringDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileReceiveActivity extends BaseActivity implements NetReceiveTask.NetReceiveTaskCallback{

    private static FileReceiveActivity fileReceiveActivity;
    private NetReceiveTask netReceiveTask;

    private FileTransferringDialog receiving_diag;
    private AlertDialog request_diag;
    private RecyclerView recyclerView;
    private ListAdapter listAdapter;
    private LinearLayoutManager linearLayoutManager;

    private final ArrayList<MessageBean>logMessages=new ArrayList<>();

    @Override
    protected void onCreate(Bundle bundle) {
        if(fileReceiveActivity!=null){
            fileReceiveActivity.finish();
        }
        fileReceiveActivity=this;
        super.onCreate(bundle);
        setContentView(R.layout.activity_file_receive);
        setTitle(getResources().getString(R.string.activity_receive_title));
        recyclerView=findViewById(R.id.activity_file_receive_recyclerview);
        linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        listAdapter=new ListAdapter();
        recyclerView.setAdapter(listAdapter);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            netReceiveTask=new NetReceiveTask(this,this);
        }catch (Exception e){
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.word_error))
                    .setMessage(getResources().getString(R.string.info_bind_port_error)+e.toString())
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                           finish();
                        }
                    })
                    .show();
        }

        ((AppCompatCheckBox)findViewById(R.id.activity_file_receive_apmode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(netReceiveTask==null)return;
                netReceiveTask.switchApMode(isChecked);
            }
        });

        findViewById(R.id.activity_file_receive_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(netReceiveTask!=null)netReceiveTask.sendOnlineBroadcastUdp();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!EnvironmentUtil.isWifiConnected(this)){
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_no_network_title))
                    .setMessage(getResources().getString(R.string.dialog_no_network_message))
                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    })
                    .show();
        }
    }

    @Override
    public void onFileReceiveRequest(@NonNull NetReceiveTask task, @NonNull String ip, @NonNull String deviceName, @NonNull List<NetReceiveTask.ReceiveFileItem> fileItems) {
        request_diag=new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.dialog_file_receive_title))
                .setMessage(getResources().getString(R.string.dialog_file_receive_device_name)+deviceName+"\n\n"
                        +getResources().getString(R.string.dialog_file_receive_ip)+ip+"\n\n"
                        +getResources().getString(R.string.dialog_file_receive_files_info)+"\n\n"+getFileInfoMessage(fileItems))
                .setPositiveButton(getResources().getString(R.string.dialog_button_accept), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        netReceiveTask.startReceiveTask();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.dialog_button_deny), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        netReceiveTask.sendRefuseReceivingFilesUdp();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onLog(String logInfo) {
        MessageBean messageBean=new MessageBean(System.currentTimeMillis(),logInfo);
        logMessages.add(messageBean);
        if(listAdapter!=null){
            listAdapter.notifyDataSetChanged();
            if(linearLayoutManager!=null){
                linearLayoutManager.smoothScrollToPosition(recyclerView,null,logMessages.size()-1);
            }
        }
    }

    @Override
    public void onFileReceiveInterrupted() {
        if(receiving_diag!=null&&receiving_diag.isShowing()){
            receiving_diag.cancel();
            receiving_diag=null;
        }
        ToastManager.showToast(this,getResources().getString(R.string.toast_send_interrupt),Toast.LENGTH_SHORT);
    }

    @Override
    public void onSendSiteCanceled(@NonNull String ip) {
        if(request_diag!=null&&request_diag.isShowing()){
            request_diag.cancel();
            request_diag=null;
        }
        ToastManager.showToast(this,getResources().getString(R.string.toast_send_canceled),Toast.LENGTH_SHORT);
    }

    @Override
    public void onFileReceiveStarted() {
        if(request_diag!=null&&request_diag.isShowing()){
            request_diag.cancel();
            request_diag=null;
        }
        if(receiving_diag==null||!receiving_diag.isShowing()){
            receiving_diag=new FileTransferringDialog(this,getResources().getString(R.string.dialog_file_receiving_title));
            receiving_diag.setButton(AlertDialog.BUTTON_NEGATIVE,getResources().getString(R.string.word_stop),new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    netReceiveTask.sendStopReceivingFilesCommand();
                }
            });
            receiving_diag.show();
        }
        setResult(RESULT_OK);
    }

    @Override
    public void onFileReceiveProgress(long progress, long total, @NonNull String currentWritePath) {
        if(receiving_diag!=null){
            receiving_diag.setProgressOfSending(progress,total);
            receiving_diag.setCurrentFileInfo(getResources().getString(R.string.dialog_file_receiving_att)+currentWritePath);
        }
    }

    @Override
    public void onSpeed(long speedOfBytes) {
        if(receiving_diag!=null)receiving_diag.setSpeed(speedOfBytes);
    }

    @Override
    public void onFileReceivedCompleted(@NonNull String error_info) {
        if(receiving_diag!=null){
            receiving_diag.cancel();
            receiving_diag=null;
        }
        if(TextUtils.isEmpty(error_info)){
            ToastManager.showToast(this,getResources().getString(R.string.toast_receiving_complete),Toast.LENGTH_SHORT);
        }else{
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_receive_error_title))
                    .setMessage(getResources().getString(R.string.dialog_receive_error_message)+error_info)
                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    })
                    .show();
        }
    }

    private String getFileInfoMessage(List<NetReceiveTask.ReceiveFileItem>receiveFileItems){
        StringBuilder stringBuilder=new StringBuilder();
        for(NetReceiveTask.ReceiveFileItem receiveFileItem:receiveFileItems){
            stringBuilder.append(receiveFileItem.getFileName());
            stringBuilder.append("(");
            stringBuilder.append(Formatter.formatFileSize(this,receiveFileItem.getLength()));
            stringBuilder.append(")");
            stringBuilder.append("\n\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_receive,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case android.R.id.home:{
                finish();
            }
            break;
            case R.id.action_help:{
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.action_help))
                        .setMessage(getResources().getString(R.string.help_receive))
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .setNeutralButton(getResources().getString(R.string.dialog_button_share_this_app), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Global.shareThisApp(FileReceiveActivity.this);
                            }
                        })
                        .show();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish(){
        super.finish();
        fileReceiveActivity=null;
        try {
            if(netReceiveTask!=null)netReceiveTask.stopTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder>{
        ListAdapter(){}

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(FileReceiveActivity.this).inflate(R.layout.item_receive_log,viewGroup,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            MessageBean messageBean=logMessages.get(viewHolder.getAdapterPosition());
            viewHolder.time.setText(messageBean.getFormattedTime());
            viewHolder.message.setText(messageBean.message);
        }

        @Override
        public int getItemCount() {
            return logMessages.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder{
        TextView time,message;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            time=itemView.findViewById(R.id.item_receive_log_time);
            message=itemView.findViewById(R.id.item_receive_log_content);
        }
    }

    private static class MessageBean{
        long time;
        String message;
        private MessageBean(long time,String message){
            this.time=time;
            this.message=String.valueOf(message);
        }
        String getFormattedTime(){
            SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return simpleDateFormat.format(new Date(time));
        }
    }
}
