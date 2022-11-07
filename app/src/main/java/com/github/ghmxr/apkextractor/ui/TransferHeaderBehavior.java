package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class TransferHeaderBehavior extends CoordinatorLayout.Behavior<ImageView> {

    /**
     * 处于中心时候原始X轴
     */
    private int mOriginalHeaderX = 0;
    /**
     * 处于中心时候原始Y轴
     */
    private int mOriginalHeaderY = 0;


    public TransferHeaderBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull ImageView child, @NonNull View dependency) {
        return dependency instanceof Toolbar;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull ImageView child, @NonNull View dependency) {
        // 计算X轴坐标
        if (mOriginalHeaderX == 0) {
            this.mOriginalHeaderX = dependency.getWidth() / 2 - child.getWidth() / 2;
        }
        // 计算Y轴坐标
        if (mOriginalHeaderY == 0) {
            mOriginalHeaderY = dependency.getHeight() - child.getHeight();
        }
        //X轴百分比
        float mPercentX = dependency.getY() / mOriginalHeaderX;
        if (mPercentX >= 1) {
            mPercentX = 1;
        }
        //Y轴百分比
        float mPercentY = dependency.getY() / mOriginalHeaderY;
        if (mPercentY >= 1) {
            mPercentY = 1;
        }

        float x = mOriginalHeaderX - mOriginalHeaderX * mPercentX;
        if (x <= child.getWidth()) {
            x = child.getWidth();
        }
        // TODO 头像的放大和缩小没做

        child.setX(x);
        child.setY(mOriginalHeaderY - mOriginalHeaderY * mPercentY);
        return true;
    }
}