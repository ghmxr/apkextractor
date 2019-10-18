package com.github.ghmxr.apkextractor;

import android.graphics.drawable.Drawable;

public interface DisplayItem {
    /**
     * @return 项目图标
     */
    Drawable getIconDrawable();
    /**
     * @return 项目标题
     */
    String getTitle();
    /**
     * @return 项目描述
     */
    String getDescription();
    /**
     * @return 项目大小，单位字节
     */
    long getSize();
    /**
     * @return 是否需要红色高亮标注
     */
    boolean isRedMarked();
}
