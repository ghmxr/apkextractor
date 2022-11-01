package com.github.ghmxr.apkextractor.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.tasks.GetPackageInfoViewTask;

import java.util.List;

public class AssemblyView extends LinearLayout implements View.OnClickListener {

    private final LinearLayout linearLayout_permission;
    private final LinearLayout linearLayout_activity;
    private final LinearLayout linearLayout_receiver;
    private final LinearLayout linearLayout_loader;
    private final LinearLayout linearLayout_service;
    private final LinearLayout linearLayout_provider;

    private final RecyclerView rv_permission;
    private final RecyclerView rv_activity;
    private final RecyclerView rv_service;
    private final RecyclerView rv_provider;
    private final RecyclerView rv_receiver;
    private final RecyclerView rv_static_loader;

    private final ImageView permission_arrow;
    private final ImageView activity_arrow;
    private final ImageView receiver_arrow;
    private final ImageView loader_arrow;
    private final ImageView service_arrow;
    private final ImageView provider_arrow;

    private final TextView tv_permission;
    private final TextView tv_activity;
    private final TextView tv_receiver;
    private final TextView tv_loader;
    private final TextView tv_service;
    private final TextView tv_provider;

    public AssemblyView(Context context) {
        this(context, null);
    }

    public AssemblyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssemblyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.layout_card_assembly, this);

        linearLayout_permission = findViewById(R.id.detail_permission);
        linearLayout_activity = findViewById(R.id.detail_activity);
        linearLayout_receiver = findViewById(R.id.detail_receiver);
        linearLayout_loader = findViewById(R.id.detail_static_loader);
        linearLayout_service = findViewById(R.id.detail_service);
        linearLayout_provider = findViewById(R.id.detail_provider);

        rv_permission = findViewById(R.id.rv_permission);
        rv_activity = findViewById(R.id.rv_activity);
        rv_service = findViewById(R.id.rv_service);
        rv_provider = findViewById(R.id.rv_provider);
        rv_receiver = findViewById(R.id.rv_receiver);
        rv_static_loader = findViewById(R.id.rv_static_loader);

        tv_permission = findViewById(R.id.detail_permission_area_att);
        tv_activity = findViewById(R.id.detail_activity_area_att);
        tv_receiver = findViewById(R.id.detail_receiver_area_att);
        tv_loader = findViewById(R.id.detail_static_loader_area_att);
        tv_service = findViewById(R.id.detail_service_area_att);
        tv_provider = findViewById(R.id.detail_provider_area_att);
        permission_arrow = findViewById(R.id.detail_permission_area_arrow);
        activity_arrow = findViewById(R.id.detail_activity_area_arrow);
        receiver_arrow = findViewById(R.id.detail_receiver_area_arrow);
        loader_arrow = findViewById(R.id.detail_static_loader_area_arrow);
        service_arrow = findViewById(R.id.detail_service_area_arrow);
        provider_arrow = findViewById(R.id.detail_provider_area_arrow);

        rv_permission.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        rv_activity.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        rv_service.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        rv_provider.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        rv_static_loader.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        rv_receiver.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

        findViewById(R.id.detail_permission_area).setOnClickListener(this);
        findViewById(R.id.detail_activity_area).setOnClickListener(this);
        findViewById(R.id.detail_receiver_area).setOnClickListener(this);
        findViewById(R.id.detail_static_loader_area).setOnClickListener(this);
        findViewById(R.id.detail_services_area).setOnClickListener(this);
        findViewById(R.id.detail_provider_area).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detail_permission_area: {
                if (linearLayout_permission.getVisibility() == View.VISIBLE) {
                    permission_arrow.setRotation(0);
                    linearLayout_permission.setVisibility(View.GONE);
//                    rv_permission.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                } else {
                    findViewById(R.id.detail_permission_area_arrow).setRotation(90);
                    linearLayout_permission.setVisibility(View.VISIBLE);
//                    rv_permission.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_activity_area: {
                if (linearLayout_activity.getVisibility() == View.VISIBLE) {
                    activity_arrow.setRotation(0);
                    linearLayout_activity.setVisibility(View.GONE);
//                    rv_activity.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                } else {
                    activity_arrow.setRotation(90);
                    linearLayout_activity.setVisibility(View.VISIBLE);
//                    rv_activity.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_receiver_area: {
                if (linearLayout_receiver.getVisibility() == View.VISIBLE) {
                    receiver_arrow.setRotation(0);
                    linearLayout_receiver.setVisibility(View.GONE);
//                    rv_receiver.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                } else {
                    receiver_arrow.setRotation(90);
                    linearLayout_receiver.setVisibility(View.VISIBLE);
//                    rv_receiver.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_static_loader_area: {
                if (linearLayout_loader.getVisibility() == View.VISIBLE) {
                    loader_arrow.setRotation(0);
                    linearLayout_loader.setVisibility(View.GONE);
//                    rv_static_loader.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                } else {
                    loader_arrow.setRotation(90);
                    linearLayout_loader.setVisibility(View.VISIBLE);
//                    rv_static_loader.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_services_area: {
                if (linearLayout_service.getVisibility() == View.VISIBLE) {
                    service_arrow.setRotation(0);
                    linearLayout_service.setVisibility(View.GONE);
//                    rv_service.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                } else {
                    service_arrow.setRotation(90);
                    linearLayout_service.setVisibility(View.VISIBLE);
//                    rv_service.setVisibility(View.VISIBLE);
                    TransitionManager.beginDelayedTransition(this);
                }
            }
            break;
            case R.id.detail_provider_area: {
                if (linearLayout_provider.getVisibility() == View.VISIBLE) {
                    provider_arrow.setRotation(0);
                    linearLayout_provider.setVisibility(View.GONE);
//                    rv_provider.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(this);
                } else {
                    provider_arrow.setRotation(90);
                    linearLayout_provider.setVisibility(View.VISIBLE);
//                    rv_provider.setVisibility(View.VISIBLE);
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

    public LinearLayout getLinearLayout_service() {
        return linearLayout_service;
    }

    public LinearLayout getLinearLayout_provider() {
        return linearLayout_provider;
    }

    @SuppressLint("SetTextI18n")
    public <T extends ComponentInfo> void setPermissionInfo(@NonNull List<GetPackageInfoViewTask.AssembleItem<T>> data) {
        findViewById(R.id.detail_card_permissions).setVisibility(VISIBLE);
        tv_permission.setText(getContext().getResources().getString(R.string.activity_detail_permissions)
                + "(" + data.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        rv_permission.setAdapter(new AssembleListViewAdapter<>(data));
    }

    @SuppressLint("SetTextI18n")
    public void setPermissionInfoToAllViews(@NonNull List<View> permissionViews) {
        for (View view : permissionViews) {
            linearLayout_permission.addView(view);
        }
        tv_permission.setText(getContext().getResources().getString(R.string.activity_detail_permissions)
                + "(" + permissionViews.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        findViewById(R.id.detail_card_permissions).setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public <T extends ComponentInfo> void setActivityInfo(@NonNull List<GetPackageInfoViewTask.AssembleItem<T>> data) {
        findViewById(R.id.detail_card_activities).setVisibility(View.VISIBLE);
        tv_activity.setText(getContext().getResources().getString(R.string.activity_detail_activities)
                + "(" + data.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        rv_activity.setAdapter(new AssembleListViewAdapter<>(data));

    }

    @SuppressLint("SetTextI18n")
    public void setActivityInfoToAllViews(@NonNull List<View> activityViews) {
        for (View view : activityViews) {
            linearLayout_activity.addView(view);
        }
        tv_activity.setText(getContext().getResources().getString(R.string.activity_detail_activities)
                + "(" + activityViews.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        findViewById(R.id.detail_card_activities).setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public <T extends ComponentInfo> void setReceiverInfo(@NonNull List<GetPackageInfoViewTask.AssembleItem<T>> data) {
        findViewById(R.id.detail_card_receivers).setVisibility(View.VISIBLE);
        tv_receiver.setText(getContext().getResources().getString(R.string.activity_detail_receivers)
                + "(" + data.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        rv_receiver.setAdapter(new AssembleListViewAdapter<>(data));

    }

    @SuppressLint("SetTextI18n")
    public void setReceiverInfoToAllViews(@NonNull List<View> receiverViews) {
        for (View view : receiverViews) {
            linearLayout_receiver.addView(view);
        }
        tv_receiver.setText(getContext().getResources().getString(R.string.activity_detail_receivers)
                + "(" + receiverViews.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        findViewById(R.id.detail_card_receivers).setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public <T extends ComponentInfo> void setServiceInfo(@NonNull List<GetPackageInfoViewTask.AssembleItem<T>> data) {
        findViewById(R.id.detail_card_services).setVisibility(View.VISIBLE);
        tv_service.setText(getContext().getResources().getString(R.string.activity_detail_services)
                + "(" + data.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        rv_service.setAdapter(new AssembleListViewAdapter<>(data));

    }

    @SuppressLint("SetTextI18n")
    public void setServiceInfoToAllViews(@NonNull List<View> serviceViews) {
        for (View view : serviceViews) {
            linearLayout_service.addView(view);
        }
        tv_service.setText(getContext().getResources().getString(R.string.activity_detail_services)
                + "(" + serviceViews.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        findViewById(R.id.detail_card_services).setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public <T extends ComponentInfo> void setProviderInfo(@NonNull List<GetPackageInfoViewTask.AssembleItem<T>> data) {
        findViewById(R.id.detail_card_providers).setVisibility(View.VISIBLE);
        tv_provider.setText(getContext().getResources().getString(R.string.activity_detail_providers)
                + "(" + data.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        rv_provider.setAdapter(new AssembleListViewAdapter<>(data));

    }

    @SuppressLint("SetTextI18n")
    public void setProviderInfoToAllViews(@NonNull List<View> providerViews) {
        for (View view : providerViews) {
            linearLayout_provider.addView(view);
        }
        tv_provider.setText(getContext().getResources().getString(R.string.activity_detail_providers)
                + "(" + providerViews.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        findViewById(R.id.detail_card_providers).setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public void setStaticReceiverInfo(@NonNull List<GetPackageInfoViewTask.StaticLoaderItem> data) {
        findViewById(R.id.detail_card_static_loaders).setVisibility(VISIBLE);
        tv_loader.setText(getContext().getResources().getString(R.string.activity_detail_static_loaders)
                + "(" + data.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        rv_static_loader.setAdapter(new StaticLoaderListAdapter(data));
    }

    @SuppressLint("SetTextI18n")
    public void setStaticReceiverToAllViews(@NonNull List<View> staticReceiverViews) {
        for (View view : staticReceiverViews) {
            linearLayout_loader.addView(view);
        }
        tv_loader.setText(getContext().getResources().getString(R.string.activity_detail_static_loaders)
                + "(" + staticReceiverViews.size() + getContext().getResources().getString(R.string.unit_item) + ")");
        findViewById(R.id.detail_card_static_loaders).setVisibility(View.VISIBLE);
    }

    public boolean getIsExpanded() {
        return linearLayout_activity.getVisibility() == VISIBLE || linearLayout_permission.getVisibility() == VISIBLE
                || linearLayout_receiver.getVisibility() == VISIBLE || linearLayout_loader.getVisibility() == VISIBLE
                || linearLayout_service.getVisibility() == VISIBLE || linearLayout_provider.getVisibility() == VISIBLE;
    }

    private static class AssembleListViewAdapter<T extends ComponentInfo> extends RecyclerView.Adapter<AssembleViewHolder> {

        private final List<GetPackageInfoViewTask.AssembleItem<T>> data;

        public AssembleListViewAdapter(List<GetPackageInfoViewTask.AssembleItem<T>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public AssembleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new AssembleViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_single_textview, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull AssembleViewHolder assembleViewHolder, int i) {
            final GetPackageInfoViewTask.AssembleItem<T> info = data.get(i);
            assembleViewHolder.tv.setText(info.item.name);
            assembleViewHolder.root.setOnClickListener(info.clickAction);
            assembleViewHolder.root.setOnLongClickListener(info.longClickAction);
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }
    }

    private static class StaticLoaderListAdapter extends RecyclerView.Adapter<StaticLoaderViewHolder> {
        private final List<GetPackageInfoViewTask.StaticLoaderItem> data;

        public StaticLoaderListAdapter(List<GetPackageInfoViewTask.StaticLoaderItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public StaticLoaderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new StaticLoaderViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_static_loader, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull StaticLoaderViewHolder staticLoaderViewHolder, int i) {
            final GetPackageInfoViewTask.StaticLoaderItem item = data.get(i);
            staticLoaderViewHolder.tv_name.setText(item.name);
            staticLoaderViewHolder.tv_name.setOnClickListener(item.clickAction);
            staticLoaderViewHolder.linearLayout.removeAllViews();
            for (GetPackageInfoViewTask.StaticLoaderItem.IntentFilterItem intentFilterItem : item.intentFilterItems) {
                staticLoaderViewHolder.linearLayout.addView(intentFilterItem.intentFilterView);
            }
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }
    }


    private static class AssembleViewHolder extends RecyclerView.ViewHolder {
        View root;
        TextView tv;

        public AssembleViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.singleTextRoot);
            tv = itemView.findViewById(R.id.item_textview);
        }
    }

    private static class StaticLoaderViewHolder extends RecyclerView.ViewHolder {
        TextView tv_name;
        LinearLayout linearLayout;

        public StaticLoaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.static_loader_name);
            linearLayout = itemView.findViewById(R.id.static_loader_intents);
        }
    }
}
