package com.github.ghmxr.apkextractor.utils;

import java.util.Iterator;
import java.util.Map;

public class CommonUtil {

    /**
     * 将指定String的key从map中移除掉，不区分大小写
     */
    public static <T> void removeKeyFromMapIgnoreCase(Map<String, T> map, String key) {
        Iterator<Map.Entry<String, T>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, T> next = iterator.next();
            if (next != null && next.getKey() != null && next.getKey().equalsIgnoreCase(key)) {
                iterator.remove();
            }
        }
    }

}
