package com.github.ghmxr.apkextractor.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.github.ghmxr.apkextractor.R;

public class MyPagerAdapter extends FragmentPagerAdapter {

    private final Activity activity;
    private final Fragment[] fragments;

    public MyPagerAdapter(@NonNull Activity activity, @NonNull FragmentManager fm, Fragment... fragments) {
        super(fm);
        this.activity = activity;
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int i) {
        return fragments[i];
    }

    @Override
    public int getCount() {
        if (fragments == null) return 0;
        return fragments.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            default:
                break;
            case 0:
                return activity.getResources().getString(R.string.main_page_export);
            case 1:
                return activity.getResources().getString(R.string.main_page_import);
        }
        return "";
    }
}
