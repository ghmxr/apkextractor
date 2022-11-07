package com.github.ghmxr.apkextractor.items;

import androidx.annotation.NonNull;

public class DeviceItem {
    private String deviceName;
    private String ip;

    public DeviceItem(String deviceName, String ip) {
        this.deviceName = deviceName;
        this.ip = ip;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    @NonNull
    public String toString() {
        return "DeviceItem{" +
                "deviceName='" + deviceName + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
