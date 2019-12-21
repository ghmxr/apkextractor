package com.github.ghmxr.apkextractor.items;

import android.support.annotation.NonNull;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.net.IpMessageConstants;

public class IpMessage {
    private String version;
    private String packageNumber;
    private String deviceName= Constants.PREFERENCE_DEVICE_NAME_DEFAULT;
    private int command=-1;
    private String additionalMessage="null";

    public IpMessage(){
        version=String.valueOf(IpMessageConstants.VERSION);
        packageNumber=String.valueOf(System.currentTimeMillis());
    }

    public IpMessage(String protocolString){
        String[]args=protocolString.split(":");
        version=args[0];
        packageNumber=args[1];
        deviceName=args[2];
        command=Integer.parseInt(args[3]);
        if(args.length>=5){
            additionalMessage =args[4];
        }
        for(int i=5;i<args.length;i++){
            additionalMessage+= ":"+args[i];
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public String getAdditionalMessage() {
        return additionalMessage;
    }

    public void setAdditionalMessage(String additionalMessage) {
        this.additionalMessage = additionalMessage;
    }

    public String getPackageNumber() {
        return packageNumber;
    }

    public String toProtocolString(){
        StringBuilder builder=new StringBuilder();
        builder.append(version);
        builder.append(":");
        builder.append(packageNumber);
        builder.append(":");
        builder.append(deviceName);
        builder.append(":");
        builder.append(command);
        builder.append(":");
        builder.append(additionalMessage);
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toProtocolString();
    }

    /**
     * 单个文件信息的切割符 0x07
     */
    public static String fileInfoSplit(){
        return new String(new byte[]{0x07});
    }
}
