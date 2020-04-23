package com.github.ghmxr.apkextractor.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.format.Formatter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.ToastManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EnvironmentUtil {

    public static void showInputMethod(@NonNull View view){
        try{
            InputMethodManager inputMethodManager=(InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            view.requestFocus();
            inputMethodManager.showSoftInput(view,0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void hideInputMethod(@NonNull final Activity activity){
        try{
            ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),0);
        }catch (Exception e){e.printStackTrace();}
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity()!=PixelFormat.OPAQUE?Bitmap.Config.ARGB_8888:Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 返回 yyyy/mm/dd/hh:mm::ss 字符串
     */
    public static @NonNull String getFormatDateAndTime(long time){
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.YEAR)+"/"+getFormatNumberWithZero(calendar.get(Calendar.MONTH)+1)
                +"/"+getFormatNumberWithZero(calendar.get(Calendar.DAY_OF_MONTH))
                +"/"+getFormatNumberWithZero(calendar.get(Calendar.HOUR_OF_DAY))
                +":"+getFormatNumberWithZero(calendar.get(Calendar.MINUTE))
                +":"+getFormatNumberWithZero(calendar.get(Calendar.SECOND));
    }

    public static @NonNull String getFormatNumberWithZero(int value){
        if(value<0)return String.valueOf(0);
        if(value<=9)return "0"+value;
        return String.valueOf(value);
    }

    /**
     * 获取apk包签名基本信息
     * @return string[0]证书发行者,string[1]证书所有者,string[2]序列号
     * string[3]证书起始时间 string[4]证书结束时间
     */
    public static @NonNull String[] getAPKSignInfo(String filePath) {
        String subjectDN = "";
        String issuerDN = "";
        String serial = "";
        String notBefore="";
        String notAfter="";
        try {
            JarFile JarFile = new JarFile(filePath);
            JarEntry JarEntry = JarFile.getJarEntry("AndroidManifest.xml");
            if (JarEntry != null) {
                byte[] readBuffer = new byte[8192];
                InputStream is = new BufferedInputStream(JarFile.getInputStream(JarEntry));
                while (is.read(readBuffer, 0, readBuffer.length) != -1) {
                    //notusing
                }
                Certificate[] certs = JarEntry.getCertificates();
                if (certs != null && certs.length > 0) {
                    //获取证书
                    X509Certificate x509cert = (X509Certificate) certs[0];
                    //获取证书发行者
                    issuerDN = x509cert.getIssuerDN().toString();
                    //System.out.println("发行者：" + issuerDN);
                    //获取证书所有者
                    subjectDN = x509cert.getSubjectDN().toString();
                    //System.out.println("所有者：" + subjectDN);
                    //证书序列号
                    serial = x509cert.getSerialNumber().toString();
                    //System.out.println("publicKey：" + publicKey);
                    //证书起始有效期
                    notBefore=x509cert.getNotBefore().toString();
                    //证书结束有效期
                    notAfter=x509cert.getNotAfter().toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[]{subjectDN,issuerDN,serial,notBefore,notAfter};
    }

    public static @NonNull String hashMD5Value(@NonNull InputStream inputStream){
        return getHashValue(inputStream,"MD5");
    }

    public static @NonNull String hashSHA256Value(@NonNull InputStream inputStream){
        return getHashValue(inputStream,"SHA256");
    }

    public static @NonNull String hashSHA1Value(@NonNull InputStream inputStream){
        return getHashValue(inputStream,"SHA1");
    }

    public static @NonNull String getSignatureMD5StringOfPackageInfo(@NonNull PackageInfo info){
        return getSignatureStringOfPackageInfo(info,"MD5");
    }

    public static @NonNull String getSignatureSHA1OfPackageInfo(@NonNull PackageInfo info){
        return getSignatureStringOfPackageInfo(info,"SHA1");
    }

    public static @NonNull String getSignatureSHA256OfPackageInfo(@NonNull PackageInfo info){
        return getSignatureStringOfPackageInfo(info,"SHA256");
    }

    private static @NonNull String getSignatureStringOfPackageInfo(@NonNull PackageInfo packageInfo,@NonNull String type){
        try{
            MessageDigest localMessageDigest = MessageDigest.getInstance(type);
            localMessageDigest.update(packageInfo.signatures[0].toByteArray());
            return getHexString(localMessageDigest.digest());
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private static @NonNull String getHashValue(@NonNull InputStream inputStream,@NonNull String type){
        try{
            MessageDigest messageDigest=MessageDigest.getInstance(type);
            int length;
            byte [] buffer=new byte[1024];
            while ((length=inputStream.read(buffer))!=-1){
                messageDigest.update(buffer,0,length);
            }
            inputStream.close();
            return getHexString(messageDigest.digest());
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    private static @NonNull String getHexString(byte[] paramArrayOfByte){
        if (paramArrayOfByte == null) {
            return "";
        }
        StringBuilder localStringBuilder = new StringBuilder(2 * paramArrayOfByte.length);
        for (int i = 0; ; i++) {
            if (i >= paramArrayOfByte.length) {
                return localStringBuilder.toString();
            }
            String str = Integer.toString(0xFF & paramArrayOfByte[i], 16);
            if (str.length() == 1) {
                str = "0" + str;
            }
            localStringBuilder.append(str);
        }
    }

    /**
     * 获取指定包名的自启接收器以及IntentFilter的Action参数，此方法较为耗时(遍历操作)
     * @deprecated 建议用获取Bundle实例的方法
     * @param package_name 指定包名
     * @return < Receiver的class名，IntentFilter的Actions >
     */
    public static @NonNull HashMap<String,List<String>> getStaticRegisteredReceiversForPackageName(@NonNull Context context, @NonNull String package_name){
        HashMap<String,List<String>>map=new HashMap<>();
        PackageManager packageManager=context.getPackageManager();
        String[] static_filters=context.getResources().getStringArray(R.array.static_receiver_filters);
       // if(static_filters==null)return new HashMap<>();
        for(String s:static_filters){
            List<ResolveInfo>list=packageManager.queryBroadcastReceivers(new Intent(s),0);
            if(list==null)continue;
            for(ResolveInfo info:list){
                String pn=info.activityInfo.packageName;
                if(pn==null)continue;
                List<String> filters_class=map.get(info.activityInfo.name);
                if(filters_class==null){
                    filters_class=new ArrayList<>();
                    filters_class.add(s);
                    if(pn.equals(package_name))map.put(info.activityInfo.name,filters_class);
                }
                else{
                    if(!filters_class.contains(s)) filters_class.add(s);
                }

            }
        }

        return map;

    }

    /**
     * 当SharedPreference中设置了加载启动项的值，则会查询启动Receiver，否则会直接返回一个空Bundle（查询为耗时操作，此方法会阻塞）
     */
    public static @NonNull Bundle getStaticRegisteredReceiversOfBundleTypeForPackageName(@NonNull Context context,@NonNull String package_name){
        Bundle bundle=new Bundle();
        if(!SPUtil.getGlobalSharedPreferences(context)
                .getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS,Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)){
            return bundle;
        }
        PackageManager packageManager=context.getPackageManager();
        String[] static_filters=context.getResources().getStringArray(R.array.static_receiver_filters);

        for(String s:static_filters){
            List<ResolveInfo>list=packageManager.queryBroadcastReceivers(new Intent(s),0);
            if(list==null)continue;
            for(ResolveInfo info:list){
                String pn=info.activityInfo.packageName;
                if(pn==null)continue;
                ArrayList<String> filters_class=bundle.getStringArrayList(info.activityInfo.name);
                if(filters_class==null){
                    filters_class=new ArrayList<>();
                    filters_class.add(s);
                    if(pn.equals(package_name))bundle.putStringArrayList(info.activityInfo.name,filters_class);
                }
                else{
                    if(!filters_class.contains(s)) filters_class.add(s);
                }

            }
        }
        return bundle;
    }

    /**
     * 判断一个字符串是否为标准Linux/Windows的标准合法文件名（不包含非法字符）
     * @param name 文件名称（仅文件名，不包含路径）
     * @return true-合法文件名  false-包含非法字符
     */
    public static boolean isALegalFileName(@NonNull String name){
        try{
            if(name.contains("?")||name.contains("\\")||name.contains("/")||name.contains(":")||name.contains("*")||name.contains("\"")
                    ||name.contains("<")||name.contains(">")||name.contains("|")) return false;
        }catch (Exception e){e.printStackTrace();}
        return true;
    }

    /**
     * 截取文件扩展名，例如Test.apk 则返回 apk
     */
    public static @NonNull String getFileExtensionName(@NonNull String fileName){
        try{
            return fileName.substring(fileName.lastIndexOf(".")+1);
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 返回文件主体的文件名，例如 Test.File.java 则返回Test.File
     */
    public static @NonNull String getFileMainName(@NonNull String fileName){
        try{
            return fileName.substring(0,fileName.lastIndexOf("."));
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 判断当前是否连接了WiFi网络
     * @return true-连接了WiFi网络
     */
    public static boolean isWifiConnected(@NonNull Context context){
        try{
            WifiInfo wifiInfo=((WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
            return wifiInfo!=null&&wifiInfo.getIpAddress()!=0;
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    /**
     * 获取系统热点是否开启
     */
    public static boolean isAPEnabled(Context context){
        try{
            WifiManager wifiManager=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method=wifiManager.getClass().getDeclaredMethod("getWifiApState");
            Field field=wifiManager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            int value_wifi_enabled=(int)field.get(wifiManager);
            return ((int)method.invoke(wifiManager))==value_wifi_enabled;
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    /**
     * 跳转到系统热点配置页
     */
    public static void goToApPageActivity(@NonNull Context context){
        try{
            Intent intent = new Intent();
            ComponentName cm = new ComponentName("com.android.settings",
                    "com.android.settings.TetherSettings");
            intent.setComponent(cm);
            intent.setAction("android.intent.action.VIEW");
            context.startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
            ToastManager.showToast(context,e.toString(), Toast.LENGTH_SHORT);
        }
    }

    public static String getRouterIpAddress(@NonNull Context context){
        try{
            WifiManager wifiManager=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo=wifiManager.getDhcpInfo();
            return Formatter.formatIpAddress(dhcpInfo.gateway);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "192.168.1.1";
    }

    /**
     * 获取本机连接WiFi网络的IP地址
     */
    public static String getSelfIp(@NonNull Context context){
        try{
            WifiManager wifiManager=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return Formatter.formatIpAddress(wifiManager.getDhcpInfo().ipAddress);
        }catch (Exception e){e.printStackTrace();}
        return "0.0.0.0";
    }

    /**
     * 获取本应用名称
     */
    public static @NonNull String getAppName(@NonNull Context context){
        try{
            PackageManager packageManager=context.getPackageManager();
            ApplicationInfo applicationInfo=packageManager.getApplicationInfo(context.getPackageName(),0);
            return String.valueOf(packageManager.getApplicationLabel(applicationInfo));
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 获取本应用版本名
     */
    public static @NonNull String getAppVersionName(@NonNull Context context){
        try{
            PackageManager packageManager=context.getPackageManager();
            return String.valueOf(packageManager.getPackageInfo(context.getPackageName(),0).versionName);
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 通过contentUri获取文件名
     */
    public static @Nullable
    String getFileNameFromContentUri(@NonNull Context context, @NonNull Uri contentUri){
        return queryResultByContentResolver(context,contentUri,MediaStore.Files.FileColumns.DISPLAY_NAME);
    }

    /**
     * 通过contentUri获取文件路径
     */
    public static @Nullable String getFilePathFromContentUri(@NonNull Context context,@NonNull Uri contentUri){
        return queryResultByContentResolver(context,contentUri,MediaStore.Files.FileColumns.DATA);
    }

    /**
     * 通过contentUri获取文件大小，返回字符串型长度，单位字节
     */
    public static @Nullable String getFileLengthFromContentUri(@NonNull Context context,@NonNull Uri contentUri){
        return queryResultByContentResolver(context,contentUri,MediaStore.Files.FileColumns.SIZE);
    }

    private static @Nullable String queryResultByContentResolver(@NonNull Context context,@NonNull Uri contentUri,@NonNull String selection){
        try{
            String result=null;
            Cursor cursor = context.getContentResolver().query(contentUri,
                    new String[]{selection},
                    null, null, null);
            if (cursor == null) return null;
            else {
                if(cursor.moveToFirst()){
                    result = cursor.getString(cursor.getColumnIndex(selection));
                }
                cursor.close();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 传入的file须为主存储下的文件，且对file有完整的读写权限
     */
    public static Uri getUriForFileByFileProvider(@NonNull Context context,@NonNull File file){
        return FileProvider.getUriForFile(context,"com.github.ghmxr.apkextractor.FileProvider",file);
    }

    /**
     * 请求更新媒体数据库
     */
    public static void requestUpdatingMediaDatabase(@NonNull Context context){
        try{
            Bundle bundle=new Bundle();
            bundle.putString("volume","external");
            Intent intent=new Intent();
            intent.putExtras(bundle);
            intent.setComponent(new ComponentName("com.android.providers.media",
                    "com.android.providers.media.MediaScannerService"));
            context.startService(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /*public static int dp2px(@NonNull Context context,int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }*/

    /*public static String getBroadCastIpAddress(@NonNull Context context){
        try{
            if(isAPEnabled(context)){
                return getRouterIpAddress(context);
            }else return "255.255.255.255";
        }catch (Exception e){
            e.printStackTrace();
        }
        return "255.255.255.255";
    }*/

    /*
     * 当本机热点作为路由时发送广播包请求在线设备的ip地址
     */
    /*public static String getApHostBroadcastAddress(){
        try{
            String ip=getApConnectedDeviceIp();
            return ip.substring(0,ip.lastIndexOf("."))+".255";
        }catch (Exception e){e.printStackTrace();}
        return "";
    }*/

    /*
     * 获取连接本机热点设备的其中一个ip地址
     */
    /*private static String getApConnectedDeviceIp(){
        try{
            //String anIP="";
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            //读取第一行信息，就是IP address HW type Flags HW address Mask Device
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("[ ]+");
                if (tokens.length < 6) {
                    continue;
                }
                //String ip = tokens[0]; //ip
                return tokens[0];
                //    String mac = tokens[3];  //mac 地址
                //  String flag = tokens[2];//表示连接状态
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }*/

}
