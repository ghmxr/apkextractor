package com.github.ghmxr.apkextractor.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.DocumentFileUtil;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;
import com.github.ghmxr.apkextractor.utils.ZipFileUtil;

import java.util.List;

public class PackageImportingPermissionDialog extends AlertDialog {

    private RecyclerView recyclerView;
    private final AppCompatActivity activity;
    private final ZipFileUtil.ZipFileInfo zipFileInfo;
    private ListAdapter listAdapter;

    public PackageImportingPermissionDialog(@NonNull AppCompatActivity activity, ZipFileUtil.ZipFileInfo zipFileInfo) {
        super(activity);
        this.activity = activity;
        this.zipFileInfo = zipFileInfo;
        setTitle("包路径授权");
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_package_permission, null);
        setView(dialogView);
        recyclerView = dialogView.findViewById(R.id.pnrv);
    }

    /*@Override
    protected int getContentLayoutId() {
        return R.layout.dialog_package_permission;
    }*/

    @Override
    public void show() {
        super.show();

        listAdapter = new ListAdapter(zipFileInfo);
        recyclerView.setAdapter(listAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePermissionDisplays() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        } else {
            listAdapter = new ListAdapter(zipFileInfo);
            recyclerView.setAdapter(listAdapter);
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final List<String> packageNames;

        public ListAdapter(ZipFileUtil.ZipFileInfo zipFileInfo) {
            this.packageNames = zipFileInfo.getResolvedPackageNames();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_packagename_permission, viewGroup, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            final String packageName = packageNames.get(i);
            final boolean data = DocumentFileUtil.canRWDataDocumentFileOf(packageName);
            final boolean obb = DocumentFileUtil.canRWObbDocumentFileOf(packageName);
            viewHolder.tv_package_name.setText(packageName);
            viewHolder.ivData.setImageResource(data ? R.drawable.shape_green_dot : R.drawable.shape_red_dot);
            viewHolder.ivObb.setImageResource(obb ? R.drawable.shape_green_dot : R.drawable.shape_red_dot);
            viewHolder.tv_data.setText("Data(" + (getContext().getResources().getString(data ? R.string.permission_granted : R.string.permission_denied)) + ")");
            viewHolder.tv_obb.setText("Obb(" + (getContext().getResources().getString(obb ? R.string.permission_granted : R.string.permission_denied)) + ")");
            viewHolder.tv_data_grant.setVisibility(data ? View.GONE : View.VISIBLE);
            viewHolder.tv_obb_grant.setVisibility(obb ? View.GONE : View.VISIBLE);
            viewHolder.tv_copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EnvironmentUtil.clip2Clipboard(packageName);
                }
            });
            viewHolder.tv_data_grant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EnvironmentUtil.jump2DataPathOfPackageName(activity, 0, packageName);
                }
            });
            viewHolder.tv_obb_grant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EnvironmentUtil.jump2ObbPathOfPackageName(activity, 0, packageName);
                }
            });
        }

        @Override
        public int getItemCount() {
            return packageNames.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_package_name, tv_data, tv_obb;
        ImageView ivData, ivObb;
        TextView tv_data_grant;
        TextView tv_obb_grant;
        TextView tv_copy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_package_name = itemView.findViewById(R.id.pn);
            ivData = itemView.findViewById(R.id.pDataDot);
            ivObb = itemView.findViewById(R.id.pObbDot);
            tv_data = itemView.findViewById(R.id.pDataTv);
            tv_copy = itemView.findViewById(R.id.pCopy);
            tv_data_grant = itemView.findViewById(R.id.pDataGrant);
            tv_obb = itemView.findViewById(R.id.pObbTv);
            tv_obb_grant = itemView.findViewById(R.id.pObbGrant);
        }
    }
}
