package com.github.ghmxr.apkextractor.net;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.IpMessage;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.OutputUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class NetReceiveTask implements UdpThread.UdpThreadCallback{

    private Context context;
    private UdpThread udpThread;
    private final String deviceName;

    private String senderIp=null;
    private int port=0;
    private final ArrayList<ReceiveFileItem>receiveFileItems=new ArrayList<>();

    private final NetReceiveTaskCallback callback;

    private NetTcpFileReceiveTask netTcpFileReceiveTask;

    public NetReceiveTask(@NonNull Context context,NetReceiveTaskCallback callback) throws Exception{
        this.context=context;
        this.callback=callback;
        deviceName= SPUtil.getDeviceName(context);
        udpThread=new UdpThread(context,this);
        udpThread.start();
        sendOnlineBroadcastUdp();
    }

    private void sendOnlineBroadcastUdp(){
        IpMessage ipMessage=new IpMessage();
        ipMessage.setDeviceName(deviceName);
        ipMessage.setCommand(IpMessageConstants.MSG_ONLINE_ANSWER);
        try {
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName("255.255.255.255"),
                    SPUtil.getPortNumber(context),null).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendRefuseReceivingFilesUdp(){
        IpMessage ipMessage=new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_RECEIVE_FILE_REFUSE);
        try{
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(this.senderIp),port,null).start();
        }catch (Exception e){
            e.printStackTrace();
        }
        senderIp=null;
    }

    public void sendStopReceivingFilesCommand(){
        IpMessage ipMessage=new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT);
        try{
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(this.senderIp),port,null).start();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            if(netTcpFileReceiveTask!=null)netTcpFileReceiveTask.setInterrupted();
        }catch (Exception e){e.printStackTrace();}
    }

    public void sendOfflineBroadcastUdp(UdpThread.UdpSendTask.UdpSendTaskCallback callback){
        IpMessage ipMessage=new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_OFFLINE);
        try{
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName("255.255.255.255"),SPUtil.getPortNumber(context),callback).start();
        }catch (Exception e){e.printStackTrace();}
    }

    public void startReceiveTask(){
        if(netTcpFileReceiveTask!=null)netTcpFileReceiveTask.setInterrupted();
        netTcpFileReceiveTask=new NetTcpFileReceiveTask(senderIp,receiveFileItems);
        netTcpFileReceiveTask.start();
    }

    public void stopTask(){
        sendOfflineBroadcastUdp(new UdpThread.UdpSendTask.UdpSendTaskCallback() {
            @Override
            public void onUdpSentCompleted(IpMessage ipMessage) {
                udpThread.stopUdp();
            }
        });
    }

    @Override
    public void onIpMessageReceived(@NonNull final String senderIp,final int port,@NonNull final IpMessage ipMessage) {
        switch (ipMessage.getCommand()){
            default:break;
            case IpMessageConstants.MSG_REQUEST_ONLINE_DEVICES:{
                IpMessage ipMessage_answer=new IpMessage();
                ipMessage_answer.setDeviceName(deviceName);
                ipMessage_answer.setCommand(IpMessageConstants.MSG_ONLINE_ANSWER);
                try {
                    new UdpThread.UdpSendTask(ipMessage_answer.toProtocolString(), InetAddress.getByName(senderIp),port,null).start();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            break;
            case IpMessageConstants.MSG_SEND_FILE_REQUEST:{
                if(this.senderIp!=null)return;
                final List<ReceiveFileItem>receiveFileItems=new ArrayList<>();
                final String deviceName=String.valueOf(ipMessage.getDeviceName());
                try{
                    String[]fileInfos=ipMessage.getAdditionalMessage().split(":");
                    for(int i=0;i<fileInfos.length;i++){
                        String fileInfo=fileInfos[i];
                        String [] singleFileInfos=fileInfo.split(IpMessage.fileInfoSplit());
                        ReceiveFileItem receiveFileItem=new ReceiveFileItem();
                        receiveFileItem.setFileName(singleFileInfos[0]);
                        receiveFileItem.setLength(Long.parseLong(singleFileInfos[1]));
                        receiveFileItems.add(receiveFileItem);
                    }
                    this.senderIp=senderIp;
                    this.port=port;
                    this.receiveFileItems.clear();
                    this.receiveFileItems.addAll(receiveFileItems);
                }catch (Exception e){
                    e.printStackTrace();
                }
                if(callback!=null){
                    Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileReceiveRequest(NetReceiveTask.this,senderIp,deviceName,receiveFileItems);
                        }
                    });
                }
            }
            break;
            case IpMessageConstants.MSG_SEND_FILE_CANCELED:{
                this.senderIp=null;
                if(callback!=null)Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSendSiteCanceled(senderIp);
                    }
                });
            }
            break;
            case IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT:{
                if(netTcpFileReceiveTask!=null)netTcpFileReceiveTask.setInterrupted();
                if(callback!=null)Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileReceiveInterrupted();
                    }
                });
            }
            break;
        }
    }

    public interface NetReceiveTaskCallback{
        void onFileReceiveRequest(@NonNull NetReceiveTask task,@NonNull String ip,@NonNull String deviceName,@NonNull List<ReceiveFileItem>fileItems);
        void onSendSiteCanceled(@NonNull String ip);
        void onFileReceiveStarted();
        void onFileReceiveInterrupted();
        void onFileReceiveProgress(long progress,long total,@NonNull String currentWritePath);
        void onSpeed(long speedOfBytes);
        void onFileReceivedCompleted();
    }

    public static class ReceiveFileItem{
        private String fileName="nullFile";
        private long length=0;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }
    }

    private class NetTcpFileReceiveTask extends Thread{
        private boolean isInterrupted=false;
        private final ArrayList<ReceiveFileItem>receiveFileItems=new ArrayList<>();
        private final String targetIp;
        private Socket socket;
        private long total=0;
        private long progress=0,progressCheck=0;
        private long speedOfBytes=0;
        private long checkTime=0;

        private NetTcpFileReceiveTask(@NonNull String targetIp, List<ReceiveFileItem>receiveFileItems){
            this.targetIp=targetIp;
            this.receiveFileItems.addAll(receiveFileItems);
        }

        @Override
        public void run() {
            super.run();
            for(ReceiveFileItem fileItem:receiveFileItems){
                total+=fileItem.length;
            }

            for(int i=0;i<receiveFileItems.size();i++){
                if(isInterrupted)return;
                final ReceiveFileItem receiveFileItem=receiveFileItems.get(i);
                try{
                    socket=new Socket(targetIp,SPUtil.getPortNumber(context));
                    if(callback!=null)Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileReceiveStarted();
                        }
                    });
                    InputStream inputStream=socket.getInputStream();
                    OutputStream outputStream;
                    if(SPUtil.getIsSaved2ExternalStorage(context)){
                        outputStream= OutputUtil.getOutputStreamForDocumentFile(context,
                                OutputUtil.getWritingDocumentFileForFileName(context,receiveFileItem.getFileName()));
                    }else{
                        outputStream=new FileOutputStream(new File(SPUtil.getInternalSavePath(context)+"/"+receiveFileItem.getFileName()));
                    }
                    if(callback!=null)Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileReceiveProgress(progress,total,SPUtil.getDisplayingExportPath(context)
                                    +"/"+receiveFileItem.getFileName());
                        }
                    });
                    byte [] buffer=new byte[1024];
                    int length;
                    while ((length=inputStream.read(buffer))!=-1&&!isInterrupted){
                        outputStream.write(buffer,0,length);
                        progress+=length;
                        speedOfBytes+=length;
                        if(progress-progressCheck>100*1024){
                            progressCheck=progress;
                            if(callback!=null)Global.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFileReceiveProgress(progress,total,SPUtil.getDisplayingExportPath(context)
                                            +"/"+receiveFileItem.getFileName());
                                }
                            });
                        }
                        long currentTime=System.currentTimeMillis();
                        if(currentTime-checkTime>1000){
                            checkTime=currentTime;
                            final long speed=speedOfBytes;
                            speedOfBytes=0;
                            if(callback!=null)Global.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSpeed(speed);
                                }
                            });
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(callback!=null&&!isInterrupted){
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileReceivedCompleted();
                    }
                });
            }
            senderIp=null;
        }

        void setInterrupted(){
            isInterrupted=true;
            interrupt();
            try{
                if(socket!=null)socket.close();
            }catch (Exception e){e.printStackTrace();}
            senderIp=null;
        }
    }

}
