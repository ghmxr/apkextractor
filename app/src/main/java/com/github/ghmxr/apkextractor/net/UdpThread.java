package com.github.ghmxr.apkextractor.net;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ghmxr.apkextractor.items.IpMessage;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpThread extends Thread {

    private static DatagramSocket datagramSocket;

    private boolean workFlag = true;
    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[65500], 65500);

    private final UdpThreadCallback callback;

    public UdpThread(@NonNull Context context, @Nullable UdpThreadCallback callback) throws Exception {
        super();
        this.callback = callback;
        openUdpSocket(SPUtil.getPortNumber(context));
    }

    @Override
    public void run() {
        super.run();
        while (workFlag) {
            if (datagramSocket == null) {
                workFlag = false;
                return;
            }
            try {
                datagramSocket.receive(datagramPacket);
                String ipMsgStr = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                Log.d("UDPReceived", ipMsgStr + " fromIP:" + datagramPacket.getAddress().getHostName() + " fromPost:" + datagramPacket.getPort());
                final IpMessage ipMessage = new IpMessage(ipMsgStr);
                if (callback != null) {
                    callback.onIpMessageReceived(datagramPacket.getAddress().getHostAddress(), datagramPacket.getPort(), ipMessage);
                }
                datagramPacket.setLength(65500);
            } catch (Exception e) {
                e.printStackTrace();
                workFlag = false;
                return;
            }
        }
    }

    public DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    public void stopUdp() {
        workFlag = false;
        interrupt();
        closeUdpSocket();
    }

    private static void openUdpSocket(int portNumber) throws Exception {
        if (datagramSocket != null) datagramSocket.close();
        datagramSocket = new DatagramSocket(portNumber);
    }

    public static void closeUdpSocket() {
        if (datagramSocket != null) datagramSocket.close();
        datagramSocket = null;
    }

    public interface UdpThreadCallback {
        void onIpMessageReceived(@NonNull String ip, int port, @NonNull IpMessage ipMessage);
    }

    public static class UdpSendTask extends Thread {
        private String ipMsgStr;
        private InetAddress targetInetAddress;
        private final int targetPort;
        private final UdpSendTaskCallback callback;

        public UdpSendTask(@NonNull String ipMsgStr, @NonNull InetAddress targetAddress, int targetPort, @Nullable UdpSendTaskCallback callback) {
            this.ipMsgStr = ipMsgStr;
            this.targetInetAddress = targetAddress;
            this.targetPort = targetPort;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                if (datagramSocket == null || datagramSocket.isClosed()) {
                    return;
                }
                byte[] sendBuffer = ipMsgStr.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(sendBuffer, 0, sendBuffer.length, targetInetAddress, targetPort);
                datagramSocket.send(datagramPacket);
                Log.d("UDPSent", ipMsgStr + "  targetIP:" + datagramPacket.getAddress().getHostName() + " targetPort:" + datagramPacket.getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                IpMessage ipMessage = new IpMessage(ipMsgStr);
                if (callback != null) callback.onUdpSentCompleted(ipMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public interface UdpSendTaskCallback {
            void onUdpSentCompleted(IpMessage ipMessage);
        }
    }
}
