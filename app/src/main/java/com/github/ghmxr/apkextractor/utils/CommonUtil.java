package com.github.ghmxr.apkextractor.utils;

import java.util.Iterator;
import java.util.Map;

public class CommonUtil {

    /**
     * 将指定String的key从map中移除掉，不区分大小写
     *
     * @return true-存在并移除了key
     */
    public static <T> boolean removeKeyFromMapIgnoreCase(Map<String, T> map, String key) {
        boolean result = false;
        Iterator<Map.Entry<String, T>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, T> next = iterator.next();
            if (next != null && next.getKey() != null && next.getKey().equalsIgnoreCase(key)) {
                iterator.remove();
                result = true;
            }
        }
        return result;
    }

}
