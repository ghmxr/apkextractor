package com.github.ghmxr.apkextractor.net;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.text.format.Formatter;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.items.IpMessage;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
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

public class NetReceiveTask implements UdpThread.UdpThreadCallback {

    private Context context;
    private UdpThread udpThread;
    private final String deviceName;

    private String senderIp = null;
    private int port = 0;
    private final ArrayList<ReceiveFileItem> receiveFileItems = new ArrayList<>();

    private final NetReceiveTaskCallback callback;

    private NetTcpFileReceiveTask netTcpFileReceiveTask;

    private String broadcastAddress = "255.255.255.255";

    public NetReceiveTask(@NonNull Context context, NetReceiveTaskCallback callback) throws Exception {
        this.context = context;
        this.callback = callback;
        deviceName = SPUtil.getDeviceName(context);
        udpThread = new UdpThread(context, this);
        udpThread.start();
        postLogInfoToCallback(context.getResources().getString(R.string.receive_log_self_ip) + EnvironmentUtil.getSelfIp(context));
        sendOnlineBroadcastUdp();
    }

    public void sendOnlineBroadcastUdp() {
        IpMessage ipMessage = new IpMessage();
        ipMessage.setDeviceName(deviceName);
        ipMessage.setCommand(IpMessageConstants.MSG_ONLINE_ANSWER);
        try {
            final int portNumber = SPUtil.getPortNumber(context);
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(broadcastAddress),
                    portNumber, null).start();
            postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_online_broadcast) + broadcastAddress + "," + context.getResources().getString(R.string.receive_log_port) + portNumber);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * 热点模式，如果连接的是对方手机热点，则尝试将上线报文发送至routerIp(对方手机地址)
     *
     * @param isApMode true 将发送上线报文至router地址
     */
    public void switchApMode(boolean isApMode) {
        this.broadcastAddress = isApMode ? EnvironmentUtil.getRouterIpAddress(context) : "255.255.255.255";
        sendOnlineBroadcastUdp();
    }

    public void sendRefuseReceivingFilesUdp() {
        IpMessage ipMessage = new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_RECEIVE_FILE_REFUSE);
        try {
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(this.senderIp), port, null).start();
            postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_refuse_broadcast) + this.senderIp + ","
                    + context.getResources().getString(R.string.receive_log_port) + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        senderIp = null;
    }

    public void sendStopReceivingFilesCommand() {
        IpMessage ipMessage = new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT);
        try {
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(this.senderIp), port, null).start();
            postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_stop_broadcast) + this.senderIp + ","
                    + context.getResources().getString(R.string.receive_log_port) + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (netTcpFileReceiveTask != null) {
                netTcpFileReceiveTask.setInterrupted();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        senderIp = null;
    }

    public void sendOfflineBroadcastUdp(UdpThread.UdpSendTask.UdpSendTaskCallback callback) {
        IpMessage ipMessage = new IpMessage();
        ipMessage.setCommand(IpMessageConstants.MSG_OFFLINE);
        try {
            new UdpThread.UdpSendTask(ipMessage.toProtocolString(), InetAddress.getByName(broadcastAddress), SPUtil.getPortNumber(context), callback).start();
            postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_offline_broadcast) + broadcastAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startReceiveTask() {
        if (netTcpFileReceiveTask != null) netTcpFileReceiveTask.setInterrupted();
        netTcpFileReceiveTask = new NetTcpFileReceiveTask(senderIp, receiveFileItems);
        netTcpFileReceiveTask.start();
    }

    public void stopTask() {
        sendOfflineBroadcastUdp(new UdpThread.UdpSendTask.UdpSendTaskCallback() {
            @Override
            public void onUdpSentCompleted(IpMessage ipMessage) {
                udpThread.stopUdp();
            }
        });
    }

    @Override
    public void onIpMessageReceived(@NonNull final String senderIp, final int port, @NonNull final IpMessage ipMessage) {
        switch (ipMessage.getCommand()) {
            default:
                break;
            case IpMessageConstants.MSG_REQUEST_ONLINE_DEVICES: {
                postLogInfoToCallback(context.getResources().getString(R.string.receive_log_received_answer_request)
                        + context.getResources().getString(R.string.receive_log_ip) + senderIp + "," + context.getResources().getString(R.string.receive_log_port)
                        + port);
                IpMessage ipMessage_answer = new IpMessage();
                ipMessage_answer.setDeviceName(deviceName);
                ipMessage_answer.setCommand(IpMessageConstants.MSG_ONLINE_ANSWER);
                try {
                    new UdpThread.UdpSendTask(ipMessage_answer.toProtocolString(), InetAddress.getByName(senderIp), port, null).start();
                    postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_online_broadcast) + senderIp
                            + "," + context.getResources().getString(R.string.receive_log_port) + port);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            break;
            case IpMessageConstants.MSG_SEND_FILE_REQUEST: {
                postLogInfoToCallback(context.getResources().getString(R.string.receive_log_received_file_request)
                        + context.getResources().getString(R.string.receive_log_ip) + senderIp + ","
                        + context.getResources().getString(R.string.receive_log_port) + port);
                if (this.senderIp != null) return;
                final List<ReceiveFileItem> receiveFileItems = new ArrayList<>();
                final String deviceName = String.valueOf(ipMessage.getDeviceName());
                try {
                    String[] fileInfos = ipMessage.getAdditionalMessage().split(":");
                    for (int i = 0; i < fileInfos.length; i++) {
                        String fileInfo = fileInfos[i];
                        String[] singleFileInfos = fileInfo.split(IpMessage.fileInfoSplit());
                        ReceiveFileItem receiveFileItem = new ReceiveFileItem();
                        receiveFileItem.setFileName(singleFileInfos[0]);
                        receiveFileItem.setLength(Long.parseLong(singleFileInfos[1]));
                        receiveFileItems.add(receiveFileItem);
                    }
                    this.senderIp = senderIp;
                    this.port = port;
                    this.receiveFileItems.clear();
                    this.receiveFileItems.addAll(receiveFileItems);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileReceiveRequest(NetReceiveTask.this, senderIp, deviceName, receiveFileItems);
                        }
                    });
                }
            }
            break;
            case IpMessageConstants.MSG_SEND_FILE_CANCELED: {
                postLogInfoToCallback(context.getResources().getString(R.string.receive_log_received_send_cancel)
                        + context.getResources().getString(R.string.receive_log_ip) + senderIp + ","
                        + context.getResources().getString(R.string.receive_log_port) + port);
                if (senderIp.equals(this.senderIp)) {
                    if (callback != null) Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSendSiteCanceled(senderIp);
                        }
                    });
                    this.senderIp = null;
                }

            }
            break;
            case IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT: {
                if (netTcpFileReceiveTask != null) netTcpFileReceiveTask.setInterrupted();
                this.senderIp = null;
                if (callback != null) Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileReceiveInterrupted();
                    }
                });
                postLogInfoToCallback(context.getResources().getString(R.string.receive_log_received_transfer_interrupt)
                        + context.getResources().getString(R.string.receive_log_ip) + senderIp + ","
                        + context.getResources().getString(R.string.receive_log_port) + port);
            }
            break;
        }
    }

    private void postLogInfoToCallback(final String logInfo) {
        if (callback != null) {
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onLog(logInfo);
                }
            });
        }
    }

    public interface NetReceiveTaskCallback {
        void onFileReceiveRequest(@NonNull NetReceiveTask task, @NonNull String ip, @NonNull String deviceName, @NonNull List<ReceiveFileItem> fileItems);

        void onSendSiteCanceled(@NonNull String ip);

        void onFileReceiveStarted();

        void onFileReceiveInterrupted();

        void onFileReceiveProgress(long progress, long total, @NonNull String currentWritePath);

        void onSpeed(long speedOfBytes);

        void onFileReceivedCompleted(@NonNull String error_info);

        void onLog(String logInfo);
    }

    public static class ReceiveFileItem {
        private String fileName = "nullFile";
        private long length = 0;

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

    private class NetTcpFileReceiveTask extends Thread {
        private boolean isInterrupted = false;
        private final ArrayList<ReceiveFileItem> receiveFileItems = new ArrayList<>();
        private final String targetIp;
        private Socket socket;
        private long total = 0;
        private long progress = 0, progressCheck = 0;
        private long speedOfBytes = 0;
        private long checkTime = 0;

        private FileItem currentWritingFileItem = null;
        private final StringBuilder error_info = new StringBuilder();

        private NetTcpFileReceiveTask(@NonNull String targetIp, List<ReceiveFileItem> receiveFileItems) {
            this.targetIp = targetIp;
            this.receiveFileItems.addAll(receiveFileItems);
        }

        @Override
        public void run() {
            super.run();
            for (ReceiveFileItem fileItem : receiveFileItems) {
                total += fileItem.length;
            }

            try {
                //初始化File导出路径
                if (!SPUtil.getIsSaved2ExternalStorage(context)) {
                    File export_path = new File(SPUtil.getInternalSavePath(context));
                    if (!export_path.exists()) {
                        export_path.mkdirs();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                error_info.append(e);
                error_info.append("\n\n");
            }

            for (int i = 0; i < receiveFileItems.size(); i++) {
                if (isInterrupted) return;
                final ReceiveFileItem receiveFileItem = receiveFileItems.get(i);
                FileItem writingFileItemThisLoop = null;
                try {
                    socket = new Socket(targetIp, SPUtil.getPortNumber(context));
                    if (callback != null) Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileReceiveStarted();
                        }
                    });
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream;

                    final String initialFileName = receiveFileItem.getFileName();
                    String fileName = initialFileName;

                    if (SPUtil.getIsSaved2ExternalStorage(context)) {
                        /*DocumentFile documentFile=OutputUtil.getExportPathDocumentFile(context).findFile(fileName);
                        int count=1;
                        while (documentFile!=null&&documentFile.exists()){
                            fileName=EnvironmentUtil.getFileMainName(initialFileName)+(count++)+"."+EnvironmentUtil.getFileExtensionName(initialFileName);
                            documentFile=OutputUtil.getExportPathDocumentFile(context).findFile(fileName);
                        }*/
                        DocumentFile writingDocumentFile = OutputUtil.getExportPathDocumentFile(context).createFile("application/x-zip-compressed", fileName);
                        outputStream = OutputUtil.getOutputStreamForDocumentFile(context,
                                writingDocumentFile);//documentFile接口在根据文件名创建文件时，如果文件名已存在会自动加后缀
                        writingFileItemThisLoop = new FileItem(context, writingDocumentFile);
                    } else {
                        File destinationFile = new File(SPUtil.getInternalSavePath(context) + "/" + fileName);
                        int count = 1;
                        while (destinationFile.exists()) {
                            fileName = EnvironmentUtil.getFileMainName(initialFileName) + "(" + (count++) + ")" + "." + EnvironmentUtil.getFileExtensionName(initialFileName);
                            destinationFile = new File(SPUtil.getInternalSavePath(context) + "/" + fileName);
                        }
                        outputStream = new FileOutputStream(destinationFile);
                        writingFileItemThisLoop = new FileItem(destinationFile);
                    }
                    currentWritingFileItem = writingFileItemThisLoop;
                    final String fileNameOfMessage = fileName;
                    if (callback != null) Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFileReceiveProgress(progress, total, SPUtil.getDisplayingExportPath(context)
                                    + "/" + fileNameOfMessage);
                        }
                    });
                    try {
                        postLogInfoToCallback(context.getResources().getString(R.string.receive_log_starting_receiving_files).replace("#N", String.valueOf(i + 1))
                                .replace("#P", writingFileItemThisLoop.getPath())
                                .replace("#L", Formatter.formatFileSize(context, receiveFileItem.length)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                                    callback.onFileReceiveProgress(progress, total, SPUtil.getDisplayingExportPath(context)
                                            + "/" + fileNameOfMessage);
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
                                    callback.onSpeed(speed);
                                }
                            });
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    if (!isInterrupted) currentWritingFileItem = null;
                    inputStream.close();
                } catch (Exception e) {
                    if (writingFileItemThisLoop != null)
                        error_info.append(writingFileItemThisLoop.getPath());
                    else error_info.append(receiveFileItem.getFileName());
                    error_info.append(" : ");
                    error_info.append(e.toString());
                    error_info.append("\n\n");
                    e.printStackTrace();
                    try {
                        postLogInfoToCallback(context.getResources().getString(R.string.receive_log_receiving_file_exception)
                                .replace("#N", String.valueOf(i + 1)) + e);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } finally {
                    try {
                        if (socket != null) socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            EnvironmentUtil.requestUpdatingMediaDatabase(context);
            context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
            if (callback != null && !isInterrupted) {
                Global.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileReceivedCompleted(error_info.toString());
                    }
                });
            }
            senderIp = null;
            postLogInfoToCallback(context.getResources().getString(R.string.receive_log_receiving_files_completed));
        }

        void setInterrupted() {
            isInterrupted = true;
            interrupt();
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (currentWritingFileItem != null) currentWritingFileItem.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
