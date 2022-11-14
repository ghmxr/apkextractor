package com.github.ghmxr.apkextractor.net;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.items.DeviceItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.IpMessage;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NetSendTask implements UdpThread.UdpThreadCallback {

    private static ServerSocket serverSocket;
    private final Context context;
    private final UdpThread udpThread;
    //private final String deviceName;
    private final ConcurrentHashMap<String, DeviceItem> onlineDevices = new ConcurrentHashMap<>();
    //private final ArrayList<FileItem>sendFiles=new ArrayList<>();
    private NetTcpFileSendTask sendTask;

    private final NetSendTaskCallback callback;
    private String targetIp = null;

    private boolean isApMode = false;


    public NetSendTask(@NonNull Context context, @Nullable NetSendTaskCallback callback) throws Exception {
        super();
        this.context = context;
        this.callback = callback;
        //deviceName= SPUtil.getDeviceName(context);
        if (serverSocket != null) {
            serverSocket.close();
        }
        udpThread = new UdpThread(context, this);
        serverSocket = new ServerSocket(SPUtil.getPortNumber(context));
        udpThread.start();
        sendRequestOnlineDevicesBroadcast();
    }

    public void sendRequestOnlineDevicesBroadcast() {
        onlineDevices.clear();
        IpMessage ipMessage = new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_REQUEST_ONLINE_DEVICES);
        try {
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(getBroadcastIp()), SPUtil.getPortNumber(context), null).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void setApMode(boolean isApMode) {
        this.isApMode = isApMode;
        sendRequestOnlineDevicesBroadcast();
    }

    private String getBroadcastIp() {
        return isApMode ? "192.168.43.255" : "255.255.255.255";
    }

    public void sendFileRequestIpMessage(@NonNull List<FileItem> sendFiles, @NonNull String targetIp) {
        //this.sendFiles.clear();
        //this.sendFiles.addAll(sendFiles);
        try {
            /*StringBuilder ipMsg_addtional=new StringBuilder();
            for(int i=0;i<sendFiles.size();i++){
                StringBuilder ipMsg_fileInfo=new StringBuilder();
                FileItem fileItem=sendFiles.get(i);
                ipMsg_fileInfo.append(fileItem.getName());
                ipMsg_fileInfo.append(IpMessage.fileInfoSplit());
                ipMsg_fileInfo.append(fileItem.length());
                ipMsg_addtional.append(ipMsg_fileInfo.toString());
                if(i<sendFiles.size()-1)ipMsg_addtional.append(":");
            }

            IpMessage ipMessage=new IpMessage();
            ipMessage.setCommand(IpMessageConstants.MSG_SEND_FILE_REQUEST);
            ipMessage.setDeviceName(SPUtil.getDeviceName(context));
            ipMessage.setAdditionalMessage(ipMsg_addtional.toString());*/
            IpMessage ipMessage = IpMessage.getSendingFileRequestIpMessgae(context, sendFiles);

            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(targetIp), SPUtil.getPortNumber(context), null).start();
            if (sendTask != null) sendTask.setInterrupted();
            sendTask = new NetTcpFileSendTask(sendFiles);
            sendTask.start();
            this.targetIp = targetIp;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendStoppingSendingFilesCommand() {
        sendIpMessageByCommandToTargetIp(IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT, this.targetIp);
        clearTcpFileSendTask();
    }

    public void sendIpMessageByCommandToTargetIp(int command, String targetIp) {
        IpMessage ipMessage = new IpMessage();
        ipMessage.setCommand(command);
        try {
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(targetIp), SPUtil.getPortNumber(context), null).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void clearTcpFileSendTask() {
        try {
            if (sendTask != null) sendTask.setInterrupted();
            sendTask = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public @NonNull
    List<DeviceItem> getOnlineDevicesData() {
        return new ArrayList<>(onlineDevices.values());
    }

    public void stopTask() {
        udpThread.stopUdp();
        try {
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIpMessageReceived(@NonNull String ip, int port, @NonNull IpMessage ipMessage) {
        switch (ipMessage.getCommand()) {
            default:
                break;
            case IpMessageConstants.MSG_ONLINE_ANSWER: {
                DeviceItem deviceItem = new DeviceItem(ipMessage.getDeviceName(), ip);
                onlineDevices.put(ip, deviceItem);
                postDeviceChangesToCallback();
            }
            break;
            case IpMessageConstants.MSG_OFFLINE: {
                onlineDevices.remove(ip);
                postDeviceChangesToCallback();
            }
            break;
            case IpMessageConstants.MSG_RECEIVE_FILE_REFUSE: {
                if (callback != null) Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSendingFilesRequestRefused();
                    }
                });
                clearTcpFileSendTask();
            }
            break;
            case IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT: {
                clearTcpFileSendTask();
                if (callback != null) Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileSendingInterrupted();
                    }
                });
            }
            break;
        }
    }

    private void postDeviceChangesToCallback() {
        if (callback != null) {
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onOnlineDevicesChanged(getOnlineDevicesData());
                }
            });
        }
    }

    public interface NetSendTaskCallback {
        void onOnlineDevicesChanged(@NonNull List<DeviceItem> devices);

        void onSendingFilesRequestRefused();

        void onFileSendingStarted(@NonNull NetSendTask task);

        void onFileSendingInterrupted();

        void onProgress(long progress, long total, String currentFile);

        void onSendingSpeed(long bytesOfSpeed);

        void onFileSendCompleted(@NonNull String error_info);
    }


    private class NetTcpFileSendTask extends Thread {
        private Socket socket;
        private boolean isInterrupted = false;
        private long total = 0;
        private long progress = 0, progressCheck = 0;
        private long checkTime = 0;
        private long speedOfBytes = 0;
        private final StringBuilder error_info = new StringBuilder();

        private final ArrayList<FileItem> fileItems = new ArrayList<>();

        NetTcpFileSendTask(@NonNull List<FileItem> fileItems) {
            this.fileItems.addAll(fileItems);
        }

        @Override
        public void run() {
            super.run();
            for (FileItem fileItem : this.fileItems) {
                total += fileItem.length();
            }
            for (int i = 0; i < fileItems.size(); i++) {
                if (isInterrupted) return;
                final FileItem fileItem = fileItems.get(i);
                try {
                    socket = serverSocket.accept();
                    if (callback != null) Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileSendingStarted(NetSendTask.this);
                        }
                    });
                    OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    InputStream inputStream = fileItem.getInputStream();
                    if (callback != null) Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onProgress(progress, total, fileItem.getPath());
                        }
                    });
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1 && !isInterrupted) {
                        outputStream.write(buffer, 0, length);

                        progress += length;
                        speedOfBytes += length;

                        if (progress - progressCheck > 100 * 1024) {
                            progressCheck = progress;
                            if (callback != null) Global.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onProgress(progress, total, fileItem.getPath());
                                }
                            });
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - checkTime > 1000) {
                            checkTime = currentTime;
                            final long speed = speedOfBytes;
                            speedOfBytes = 0;
                            if (callback != null) Global.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSendingSpeed(speed);
                                }
                            });
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    error_info.append(fileItem.getName());
                    error_info.append(" : ");
                    error_info.append(e.toString());
                    error_info.append("\n\n");
                } finally {
                    try {
                        if (socket != null) socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (callback != null && !isInterrupted) Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onFileSendCompleted(error_info.toString());
                }
            });
        }

        void setInterrupted() {
            isInterrupted = true;
            interrupt();
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
