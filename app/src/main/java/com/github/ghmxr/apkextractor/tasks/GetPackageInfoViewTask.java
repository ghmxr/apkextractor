package com.github.ghmxr.apkextractor.tasks;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.AssemblyView;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.SPUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GetPackageInfoViewTask extends Thread {
    private final Activity activity;
    private final PackageInfo packageInfo;
    private final Bundle static_receiver_bundle;
    private final CompletedCallback callback;
    private final AssemblyView assemblyView;

    public GetPackageInfoViewTask(@NonNull Activity activity, @NonNull PackageInfo packageInfo
            , @NonNull Bundle static_receiver_bundle
            , @NonNull AssemblyView assemblyView
            , @NonNull CompletedCallback callback) {
        this.activity = activity;
        this.packageInfo = packageInfo;
        this.static_receiver_bundle = static_receiver_bundle;
        this.assemblyView = assemblyView;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        final SharedPreferences settings = SPUtil.getGlobalSharedPreferences(activity);
        final String[] permissions = packageInfo.requestedPermissions;
        final ActivityInfo[] activities = packageInfo.activities;
        final ActivityInfo[] receivers = packageInfo.receivers;
        final ServiceInfo[] services = packageInfo.services;
        final ProviderInfo[] providers = packageInfo.providers;

        final boolean get_permissions = settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT);
        final boolean get_activities = settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT);
        final boolean get_receivers = settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT);
        final boolean get_static_loaders = settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT);
        final boolean get_services = settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT);
        final boolean get_providers = settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT);

        final ArrayList<View> permission_child_views = new ArrayList<>();
        final ArrayList<View> activity_child_views = new ArrayList<>();
        final ArrayList<View> receiver_child_views = new ArrayList<>();
        final ArrayList<View> loaders_child_views = new ArrayList<>();
        final ArrayList<View> service_child_views = new ArrayList<>();
        final ArrayList<View> provider_child_views = new ArrayList<>();

        if (permissions != null && get_permissions) {
            for (final String s : permissions) {
                if (s == null) continue;
                permission_child_views.add(getSingleItemView(assemblyView.getLinearLayout_permission(), s, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(s);
                    }
                }, null));
            }
        }
        if (activities != null && get_activities) {
            for (final ActivityInfo info : activities) {
                activity_child_views.add(getSingleItemView(assemblyView.getLinearLayout_activity(), info.name, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(info.name);
                    }
                }, new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        try {
                            Intent intent = new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setClassName(info.packageName, info.name);
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            ToastManager.showToast(activity, e.toString(), Toast.LENGTH_SHORT);
                        }
                        return true;
                    }
                }));
            }
        }
        if (receivers != null && get_receivers) {
            for (final ActivityInfo activityInfo : receivers) {
                receiver_child_views.add(getSingleItemView(assemblyView.getLinearLayout_receiver(), activityInfo.name, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(activityInfo.name);
                    }
                }, null));
            }
        }

        if (services != null && get_services) {
            for (final ServiceInfo serviceInfo : services) {
                service_child_views.add(getSingleItemView(assemblyView.getLinearLayout_service(), serviceInfo.name, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(serviceInfo.name);
                    }
                }, new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        try {
                            Intent intent = new Intent();
                            intent.setClassName(serviceInfo.packageName, serviceInfo.name);
                            activity.startService(intent);
                        } catch (Exception e) {
                            ToastManager.showToast(activity, e.toString(), Toast.LENGTH_SHORT);
                        }
                        return true;
                    }
                }));
            }
        }

        if (providers != null && get_providers) {
            for (final ProviderInfo providerInfo : providers) {
                provider_child_views.add(getSingleItemView(assemblyView.getLinearLayout_provider(), providerInfo.name, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(providerInfo.name);
                    }
                }, null));
            }
        }

        final Set<String> keys = static_receiver_bundle.keySet();
        if (get_static_loaders) {
            for (final String s : keys) {
                View static_loader_item_view = LayoutInflater.from(activity).inflate(R.layout.item_static_loader, assemblyView.getLinearLayout_loader(), false);
                ((TextView) static_loader_item_view.findViewById(R.id.static_loader_name)).setText(s);
                static_loader_item_view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(s);
                    }
                });
                ViewGroup filter_views = static_loader_item_view.findViewById(R.id.static_loader_intents);
                List<String> filters = static_receiver_bundle.getStringArrayList(s);
                if (filters == null) continue;
                for (final String filter : filters) {
                    View itemView = LayoutInflater.from(activity).inflate(R.layout.item_single_textview, filter_views, false);
                    ((TextView) itemView.findViewById(R.id.item_textview)).setText(filter);
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            clip2ClipboardAndShowSnackbar(filter);
                        }
                    });
                    filter_views.addView(itemView);
                }
                loaders_child_views.add(static_loader_item_view);
            }
        }

        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if (get_permissions) {
                    for (View view : permission_child_views)
                        assemblyView.getLinearLayout_permission().addView(view);
                    TextView att_permission = assemblyView.getTv_permission();
                    att_permission.setText(activity.getResources().getString(R.string.activity_detail_permissions)
                            + "(" + permission_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_permissions).setVisibility(View.VISIBLE);
                }
                if (get_activities) {
                    for (View view : activity_child_views)
                        assemblyView.getLinearLayout_activity().addView(view);
                    TextView att_activity = assemblyView.getTv_activity();
                    att_activity.setText(activity.getResources().getString(R.string.activity_detail_activities)
                            + "(" + activity_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_activities).setVisibility(View.VISIBLE);
                }
                if (get_receivers) {
                    for (View view : receiver_child_views)
                        assemblyView.getLinearLayout_receiver().addView(view);
                    TextView att_receiver = assemblyView.getTv_receiver();
                    att_receiver.setText(activity.getResources().getString(R.string.activity_detail_receivers) + "(" + receiver_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_receivers).setVisibility(View.VISIBLE);
                }
                if (get_static_loaders) {
                    for (View view : loaders_child_views)
                        assemblyView.getLinearLayout_loader().addView(view);
                    TextView att_static_loader = assemblyView.getTv_loader();
                    att_static_loader.setText(activity.getResources().getString(R.string.activity_detail_static_loaders) + "(" + keys.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_static_loaders).setVisibility(View.VISIBLE);
                }
                if (get_services) {
                    for (View view : service_child_views)
                        assemblyView.getLinearLayout_service().addView(view);
                    TextView att_service = assemblyView.getTv_service();
                    att_service.setText(activity.getResources().getString(R.string.activity_detail_services) + "(" + service_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_services).setVisibility(View.VISIBLE);
                }
                if (get_providers) {
                    for (View view : provider_child_views)
                        assemblyView.getLinearLayout_provider().addView(view);
                    TextView att_providers = assemblyView.getTv_provider();
                    att_providers.setText(activity.getResources().getString(R.string.activity_detail_providers) + "(" + provider_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_providers).setVisibility(View.VISIBLE);
                }
                callback.onViewsCreated();
            }
        });
    }

    private View getSingleItemView(ViewGroup group, String text, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        View view = LayoutInflater.from(activity).inflate(R.layout.item_single_textview, group, false);
        ((TextView) view.findViewById(R.id.item_textview)).setText(text);
        view.setOnClickListener(clickListener);
        view.setOnLongClickListener(longClickListener);
        return view;
    }

    private void clip2ClipboardAndShowSnackbar(String s) {
        try {
            ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("message", s));
            Snackbar.make(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.snack_bar_clipboard), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface CompletedCallback {
        void onViewsCreated();
    }
}
