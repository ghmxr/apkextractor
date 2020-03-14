package com.github.ghmxr.apkextractor.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ghmxr.apkextractor.R;

public class AssemblyView extends LinearLayout implements View.OnClickListener{

    private LinearLayout linearLayout_permission;
    private LinearLayout linearLayout_activity;
    private LinearLayout linearLayout_receiver;
    private LinearLayout linearLayout_loader;

    private ImageView permission_arrow;
    private ImageView activity_arrow;
    private ImageView receiver_arrow;
    private ImageView loader_arrow;

    private TextView tv_permission;
    private TextView tv_activity;
    private TextView tv_receiver;
    private TextView tv_loader;

    public AssemblyView(Context context) {
        this(context,null);
    }

    public AssemblyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AssemblyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.layout_card_assembly,this);
        linearLayout_permission=findViewById(R.id.detail_permission);
        linearLayout_activity=findViewById(R.id.detail_activity);
        linearLayout_receiver=findViewById(R.id.detail_receiver);
        linearLayout_loader=findViewById(R.id.detail_static_loader);
        tv_permission=findViewById(R.id.detail_permission_area_att);
        tv_activity=findViewById(R.id.detail_activity_area_att);
        tv_receiver=findViewById(R.id.detail_receiver_area_att);
        tv_loader=findViewById(R.id.detail_static_loader_area_att);
        permission_arrow=findViewById(R.id.detail_permission_area_arrow);
        activity_arrow=findViewById(R.id.detail_activity_area_arrow);
        receiver_arrow=findViewById(R.id.detail_receiver_area_arrow);
        loader_arrow=findViewById(R.id.detail_static_loader_area_arrow);

        findViewById(R.id.detail_permission_area).setOnClickListener(this);
        findViewById(R.id.detail_activity_area).setOnClickListener(this);
        findViewById(R.id.detail_receiver_area).setOnClickListener(this);
        findViewById(R.id.detail_static_loader_area).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.detail_permission_area:{
                if(linearLayout_permission.getVisibility()==View.VISIBLE){
                    permission_arrow.setRotation(0);
                    linearLayout_permission.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                }else {
                    findViewById(R.id.detail_permission_area_arrow).setRotation(90);
                    linearLayout_permission.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_activity_area:{
                if(linearLayout_activity.getVisibility()==View.VISIBLE){
                    activity_arrow.setRotation(0);
                    linearLayout_activity.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                }else{
                    activity_arrow.setRotation(90);
                    linearLayout_activity.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_receiver_area:{
                if(linearLayout_receiver.getVisibility()==View.VISIBLE){
                    receiver_arrow.setRotation(0);
                    linearLayout_receiver.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                }else{
                    receiver_arrow.setRotation(90);
                    linearLayout_receiver.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_static_loader_area:{
                if(linearLayout_loader.getVisibility()==View.VISIBLE){
                    loader_arrow.setRotation(0);
                    linearLayout_loader.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                }else{
                    loader_arrow.setRotation(90);
                    linearLayout_loader.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
        }
    }

    public LinearLayout getLinearLayout_permission() {
        return linearLayout_permission;
    }

    public LinearLayout getLinearLayout_activity() {
        return linearLayout_activity;
    }

    public LinearLayout getLinearLayout_receiver() {
        return linearLayout_receiver;
    }

    public LinearLayout getLinearLayout_loader() {
        return linearLayout_loader;
    }

    public TextView getTv_permission() {
        return tv_permission;
    }

    public TextView getTv_activity() {
        return tv_activity;
    }

    public TextView getTv_receiver() {
        return tv_receiver;
    }

    public TextView getTv_loader() {
        return tv_loader;
    }

    public boolean getIsExpanded(){
        return linearLayout_activity.getVisibility()==VISIBLE||linearLayout_permission.getVisibility()==VISIBLE
                ||linearLayout_receiver.getVisibility()==VISIBLE||linearLayout_loader.getVisibility()==VISIBLE;
    }
}
