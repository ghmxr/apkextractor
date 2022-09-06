package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ghmxr.apkextractor.R;

public class SignatureView extends LinearLayout {

    private final ViewGroup root;

    private TextView tv_sub_value;
    private TextView tv_iss_value;
    private TextView tv_serial_value;
    private TextView tv_start;
    private TextView tv_end;
    private TextView tv_md5;
    private TextView tv_sha1;
    private TextView tv_sha256;

    private LinearLayout linearLayout_sub;
    private LinearLayout linearLayout_iss;
    private LinearLayout linearLayout_serial;
    private LinearLayout linearLayout_start;
    private LinearLayout linearLayout_end;
    private LinearLayout linearLayout_md5;
    private LinearLayout linearLayout_sha1;
    private LinearLayout linearLayout_sha256;


    public SignatureView(Context context) {
        this(context, null);
    }

    public SignatureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.layout_card_signature, this);
        root = findViewById(R.id.detail_signature_root);
        tv_sub_value = findViewById(R.id.detail_signature_sub_value);
        tv_iss_value = findViewById(R.id.detail_signature_iss_value);
        tv_serial_value = findViewById(R.id.detail_signature_serial_value);
        tv_start = findViewById(R.id.detail_signature_start_value);
        tv_end = findViewById(R.id.detail_signature_end_value);
        tv_md5 = findViewById(R.id.detail_signature_md5_value);
        tv_sha1 = findViewById(R.id.detail_signature_sha1_value);
        tv_sha256 = findViewById(R.id.detail_signature_sha256_value);

        linearLayout_sub = findViewById(R.id.detail_signature_sub);
        linearLayout_iss = findViewById(R.id.detail_signature_iss);
        linearLayout_serial = findViewById(R.id.detail_signature_serial);
        linearLayout_start = findViewById(R.id.detail_signature_start);
        linearLayout_end = findViewById(R.id.detail_signature_end);
        linearLayout_md5 = findViewById(R.id.detail_signature_md5);
        linearLayout_sha1 = findViewById(R.id.detail_signature_sha1);
        linearLayout_sha256 = findViewById(R.id.detail_signature_sha256);
    }

    public TextView getTv_sub_value() {
        return tv_sub_value;
    }

    public TextView getTv_iss_value() {
        return tv_iss_value;
    }

    public TextView getTv_serial_value() {
        return tv_serial_value;
    }

    public TextView getTv_start() {
        return tv_start;
    }

    public TextView getTv_end() {
        return tv_end;
    }

    public TextView getTv_md5() {
        return tv_md5;
    }

    public TextView getTv_sha1() {
        return tv_sha1;
    }

    public TextView getTv_sha256() {
        return tv_sha256;
    }

    public LinearLayout getLinearLayout_sub() {
        return linearLayout_sub;
    }

    public LinearLayout getLinearLayout_iss() {
        return linearLayout_iss;
    }

    public LinearLayout getLinearLayout_serial() {
        return linearLayout_serial;
    }

    public LinearLayout getLinearLayout_start() {
        return linearLayout_start;
    }

    public LinearLayout getLinearLayout_end() {
        return linearLayout_end;
    }

    public LinearLayout getLinearLayout_md5() {
        return linearLayout_md5;
    }

    public LinearLayout getLinearLayout_sha1() {
        return linearLayout_sha1;
    }

    public LinearLayout getLinearLayout_sha256() {
        return linearLayout_sha256;
    }

    public ViewGroup getRoot() {
        return root;
    }
}
