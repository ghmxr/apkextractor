package com.github.ghmxr.apkextractor.net;

public class IpMessageConstants {

    /**
     * 协议版本号
     */
    public static final int VERSION=1;

    /**
     * 请求在线设备
     */
    public static final int MSG_REQUEST_ONLINE_DEVICES=0x0001;

    /**
     * 请求在线设备回应
     */
    public static final int MSG_ONLINE_ANSWER=0x0002;

    /**
     * 设备下线
     */
    public static final int MSG_OFFLINE=0x0003;

    /**
     * 发送文件请求
     */
    public static final int MSG_SEND_FILE_REQUEST=0x0004;

    /**
     * 发送端取消了发送文件
     */
    public static final int MSG_SEND_FILE_CANCELED =0x0005;

    /**
     * 接收端拒绝接收文件
     */
    public static final int MSG_RECEIVE_FILE_REFUSE =0x0006;

    /**
     * 文件传输中途任意一端取消了发送
     */
    public static final int MSG_FILE_TRANSFERRING_INTERRUPT=0x0007;
}
