package com.github.ghmxr.apkextractor.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.net.NetReceiveTask;
import com.github.ghmxr.apkextractor.ui.FileTransferringDialog;

import java.util.List;

public class FileReceiveActivity extends BaseActivity implements NetReceiveTask.NetReceiveTaskCallback{

    private NetReceiveTask netReceiveTask;

    private FileTransferringDialog receiving_diag;
    private AlertDialog request_diag;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            netReceiveTask=new NetReceiveTask(this,this);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileReceiveRequest(@NonNull NetReceiveTask task, @NonNull String ip, @NonNull String deviceName, @NonNull List<NetReceiveTask.ReceiveFileItem> fileItems) {

        request_diag=new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.dialog_file_receive_title))
                .setMessage(getResources().getString(R.string.dialog_file_receive_device_name)+deviceName+"\n"
                        +getResources().getString(R.string.dialog_file_receive_ip)+ip+"\n"
                        +getResources().getString(R.string.dialog_file_receive_files_info)+"\n\n"+getFileInfoMessage(fileItems))
                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        netReceiveTask.startReceiveTask();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        netReceiveTask.sendRefuseReceivingFilesUdp();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onFileReceiveInterrupted() {
        if(receiving_diag!=null&&receiving_diag.isShowing()){
            receiving_diag.cancel();
            receiving_diag=null;
        }
    }

    @Override
    public void onSendSiteCanceled(@NonNull String ip) {
        if(request_diag!=null&&request_diag.isShowing()){
            request_diag.cancel();
            request_diag=null;
        }
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
    }

    @Override
    public void onFileReceiveProgress(long progress, long total, @NonNull String currentWritePath) {
        receiving_diag.setProgressOfSending(progress,total);
        receiving_diag.setCurrentFileInfo(getResources().getString(R.string.dialog_file_receiving_att)+currentWritePath);
    }

    @Override
    public void onSpeed(long speedOfBytes) {
        receiving_diag.setSpeed(speedOfBytes);
    }

    @Override
    public void onFileReceivedCompleted() {
        receiving_diag.cancel();
        receiving_diag=null;
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
        netReceiveTask.stopTask();
    }
}
