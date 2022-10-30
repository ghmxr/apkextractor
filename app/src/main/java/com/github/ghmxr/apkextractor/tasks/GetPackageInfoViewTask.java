package com.github.ghmxr.apkextractor.tasks;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.ghmxr.apkextractor.Constants;
import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.ui.AssemblyView;
import com.github.ghmxr.apkextractor.ui.ToastManager;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.SPUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GetPackageInfoViewTask extends Thread {
    private final Activity activity;
    private final PackageInfo packageInfo;
    private final CompletedCallback callback;
    private final AssemblyView assemblyView;

    private static final ConcurrentHashMap<String, PackageInfo> cache_wrapped_package_info = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Bundle> cache_static_receivers = new ConcurrentHashMap<>();

    public GetPackageInfoViewTask(@NonNull Activity activity, @NonNull PackageInfo packageInfo
            , @NonNull AssemblyView assemblyView
            , @NonNull CompletedCallback callback) {
        this.activity = activity;
        this.packageInfo = packageInfo;
        this.assemblyView = assemblyView;
        this.callback = callback;
    }

    public static class AssembleItem<T extends ComponentInfo> {
        public static class StringComponent extends ComponentInfo {
            public StringComponent(String name) {
                this.name = name;
            }
        }

        public T item;
        public View.OnClickListener clickAction;
        public View.OnLongClickListener longClickAction;

        public AssembleItem(T item, View.OnClickListener clickAction) {
            this.item = item;
            this.clickAction = clickAction;
        }

        public AssembleItem(T item, View.OnClickListener clickAction, View.OnLongClickListener longClickAction) {
            this.item = item;
            this.clickAction = clickAction;
            this.longClickAction = longClickAction;
        }
    }

    public static class StaticLoaderItem {
        public String name;
        public View.OnClickListener clickAction;
        public final ArrayList<IntentFilterItem> intentFilterItems = new ArrayList<>();

        public static class IntentFilterItem {
            public String actionName;
            public View.OnClickListener clickAction;
            public View intentFilterView;
        }
    }

    @Override
    public void run() {
        super.run();
        final SharedPreferences settings = SPUtil.getGlobalSharedPreferences(activity);
        String[] permissions = null;
        ActivityInfo[] activities = null;
        ActivityInfo[] receivers = null;
        ServiceInfo[] services = null;
        ProviderInfo[] providers = null;

        final boolean get_permissions = settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT);
        final boolean get_activities = settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT);
        final boolean get_receivers = settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT);
        final boolean get_static_loaders = settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT);
        final boolean get_services = settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT);
        final boolean get_providers = settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT);

        try {
            int flag = 0;
            if (get_permissions) flag |= PackageManager.GET_PERMISSIONS;
            if (get_activities) flag |= PackageManager.GET_ACTIVITIES;
            if (get_receivers) flag |= PackageManager.GET_RECEIVERS;
            if (get_services) flag |= PackageManager.GET_SERVICES;
            if (get_providers) flag |= PackageManager.GET_PROVIDERS;

            PackageInfo wrapped_package_info = cache_wrapped_package_info.get(packageInfo.applicationInfo.sourceDir);
            if (wrapped_package_info == null) {
                wrapped_package_info = activity.getPackageManager().getPackageArchiveInfo(packageInfo.applicationInfo.sourceDir, flag);
                cache_wrapped_package_info.put(packageInfo.applicationInfo.sourceDir, wrapped_package_info);
            }

            permissions = wrapped_package_info.requestedPermissions;
            activities = wrapped_package_info.activities;
            receivers = wrapped_package_info.receivers;
            services = wrapped_package_info.services;
            providers = wrapped_package_info.providers;
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle static_receiver_bundle = cache_static_receivers.get(packageInfo.applicationInfo.sourceDir);
        if (static_receiver_bundle == null) {
            static_receiver_bundle = EnvironmentUtil.getStaticRegisteredReceiversOfBundleTypeForPackageName(activity, packageInfo.packageName);
            cache_static_receivers.put(packageInfo.applicationInfo.sourceDir, static_receiver_bundle);
        }

        final ArrayList<AssembleItem<AssembleItem.StringComponent>> permissionData = new ArrayList<>();
        final ArrayList<AssembleItem<ActivityInfo>> activityData = new ArrayList<>();
        final ArrayList<AssembleItem<ActivityInfo>> receiverData = new ArrayList<>();
        final ArrayList<AssembleItem<ServiceInfo>> serviceData = new ArrayList<>();
        final ArrayList<AssembleItem<ProviderInfo>> providerData = new ArrayList<>();
        final ArrayList<StaticLoaderItem> staticLoaderItems = new ArrayList<>();


        /*final ArrayList<View> permission_child_views = new ArrayList<>();
        final ArrayList<View> activity_child_views = new ArrayList<>();
        final ArrayList<View> receiver_child_views = new ArrayList<>();
        final ArrayList<View> loaders_child_views = new ArrayList<>();
        final ArrayList<View> service_child_views = new ArrayList<>();
        final ArrayList<View> provider_child_views = new ArrayList<>();*/

        if (permissions != null && get_permissions) {
            for (final String s : permissions) {
                if (s == null) continue;
                permissionData.add(new AssembleItem<>(new AssembleItem.StringComponent(s), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(s);
                    }
                }));
               /* permission_child_views.add(getSingleItemView(assemblyView.getLinearLayout_permission(), s, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(s);
                    }
                }, null));*/
            }
        }
        if (activities != null && get_activities) {
            for (final ActivityInfo info : activities) {
                if (info == null) continue;
                activityData.add(new AssembleItem<>(info, new View.OnClickListener() {
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
               /* activity_child_views.add(getSingleItemView(assemblyView.getLinearLayout_activity(), info.name, new View.OnClickListener() {
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
                }));*/
            }
        }
        if (receivers != null && get_receivers) {
            for (final ActivityInfo activityInfo : receivers) {
                if (activityInfo == null) continue;
                receiverData.add(new AssembleItem<>(activityInfo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(activityInfo.name);
                    }
                }));
                /*receiver_child_views.add(getSingleItemView(assemblyView.getLinearLayout_receiver(), activityInfo.name, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(activityInfo.name);
                    }
                }, null));*/
            }
        }

        if (services != null && get_services) {
            for (final ServiceInfo serviceInfo : services) {
                if (serviceInfo == null) continue;
                serviceData.add(new AssembleItem<>(serviceInfo, new View.OnClickListener() {
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
                /*service_child_views.add(getSingleItemView(assemblyView.getLinearLayout_service(), serviceInfo.name, new View.OnClickListener() {
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
                }));*/
            }
        }

        if (providers != null && get_providers) {
            for (final ProviderInfo providerInfo : providers) {
                if (providerInfo == null) continue;
                providerData.add(new AssembleItem<>(providerInfo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(providerInfo.name);
                    }
                }));
                /*provider_child_views.add(getSingleItemView(assemblyView.getLinearLayout_provider(), providerInfo.name, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(providerInfo.name);
                    }
                }, null));*/
            }
        }

        final Set<String> keys = static_receiver_bundle.keySet();
        if (get_static_loaders) {
            for (final String s : keys) {
                StaticLoaderItem staticLoaderItem = new StaticLoaderItem();
                staticLoaderItem.name = s;
                staticLoaderItem.clickAction = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(s);
                    }
                };
                //////////////
                /*View static_loader_item_view = LayoutInflater.from(activity).inflate(R.layout.item_static_loader, assemblyView.getLinearLayout_loader(), false);
                ((TextView) static_loader_item_view.findViewById(R.id.static_loader_name)).setText(s);
                static_loader_item_view.findViewById(R.id.static_loader_name).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clip2ClipboardAndShowSnackbar(s);
                    }
                });
                ViewGroup filter_views = static_loader_item_view.findViewById(R.id.static_loader_intents);*/
                ////////////
                List<String> filters = static_receiver_bundle.getStringArrayList(s);
                if (filters == null) continue;
                for (final String filter : filters) {
                    if (filter == null) continue;
                    StaticLoaderItem.IntentFilterItem intentFilterItem = new StaticLoaderItem.IntentFilterItem();
                    intentFilterItem.actionName = filter;
                    intentFilterItem.clickAction = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            clip2ClipboardAndShowSnackbar(filter);
                        }
                    };
                    ////////////////
                    View itemView = LayoutInflater.from(activity).inflate(R.layout.item_single_textview, null, false);
                    ((TextView) itemView.findViewById(R.id.item_textview)).setText(filter);
                    itemView.setOnClickListener(intentFilterItem.clickAction);
                    intentFilterItem.intentFilterView = itemView;
//                    filter_views.addView(itemView);
                    staticLoaderItem.intentFilterItems.add(intentFilterItem);
                }
                staticLoaderItems.add(staticLoaderItem);
//                loaders_child_views.add(static_loader_item_view);
            }
        }

        Global.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (get_permissions) {
                    /*for (View view : permission_child_views)
                        assemblyView.getLinearLayout_permission().addView(view);
                    TextView att_permission = assemblyView.getTv_permission();
                    att_permission.setText(activity.getResources().getString(R.string.activity_detail_permissions)
                            + "(" + permission_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_permissions).setVisibility(View.VISIBLE);*/
                    assemblyView.setPermissionInfo(permissionData);
                }
                if (get_activities) {
                    /*for (View view : activity_child_views)
                        assemblyView.getLinearLayout_activity().addView(view);
                    TextView att_activity = assemblyView.getTv_activity();
                    att_activity.setText(activity.getResources().getString(R.string.activity_detail_activities)
                            + "(" + activity_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_activities).setVisibility(View.VISIBLE);*/
                    assemblyView.setActivityInfo(activityData);
                }
                if (get_receivers) {
                    /*for (View view : receiver_child_views)
                        assemblyView.getLinearLayout_receiver().addView(view);
                    TextView att_receiver = assemblyView.getTv_receiver();
                    att_receiver.setText(activity.getResources().getString(R.string.activity_detail_receivers) + "(" + receiver_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_receivers).setVisibility(View.VISIBLE);*/
                    assemblyView.setReceiverInfo(receiverData);
                }
                if (get_static_loaders) {
                    /*for (View view : loaders_child_views)
                        assemblyView.getLinearLayout_loader().addView(view);
                    TextView att_static_loader = assemblyView.getTv_loader();
                    att_static_loader.setText(activity.getResources().getString(R.string.activity_detail_static_loaders) + "(" + keys.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_static_loaders).setVisibility(View.VISIBLE);*/
                    assemblyView.setStaticReceiverInfo(staticLoaderItems);
                }
                if (get_services) {
                    /*for (View view : service_child_views)
                        assemblyView.getLinearLayout_service().addView(view);
                    TextView att_service = assemblyView.getTv_service();
                    att_service.setText(activity.getResources().getString(R.string.activity_detail_services) + "(" + service_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_services).setVisibility(View.VISIBLE);*/
                    assemblyView.setServiceInfo(serviceData);
                }
                if (get_providers) {
                    /*for (View view : provider_child_views)
                        assemblyView.getLinearLayout_provider().addView(view);
                    TextView att_providers = assemblyView.getTv_provider();
                    att_providers.setText(activity.getResources().getString(R.string.activity_detail_providers) + "(" + provider_child_views.size() + activity.getResources().getString(R.string.unit_item) + ")");
                    assemblyView.findViewById(R.id.detail_card_providers).setVisibility(View.VISIBLE);*/
                    assemblyView.setProviderInfo(providerData);
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
