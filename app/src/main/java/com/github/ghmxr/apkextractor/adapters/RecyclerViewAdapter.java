package com.github.ghmxr.apkextractor.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ghmxr.apkextractor.DisplayItem;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecyclerViewAdapter<T extends DisplayItem> extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private final Activity activity;
    private final RecyclerView recyclerView;
    private final ArrayList<T>data=new ArrayList<>();
    private final ListAdapterOperationListener<T> listener;
    private boolean[] isSelected;
    private boolean isMultiSelectMode=false;
    private int mode;
    private String highlightKeyword =null;

    public RecyclerViewAdapter(@NonNull Activity activity,@NonNull RecyclerView recyclerView,@Nullable List<T>data,int viewMode,
                               @NonNull ListAdapterOperationListener<T> listener){
        this.activity=activity;
        this.recyclerView=recyclerView;
        if(data!=null)this.data.addAll(data);
        this.listener=listener;
        this.mode=viewMode;
        setLayoutManagerAndView(mode);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(activity).inflate(i==0?R.layout.item_app_info_linear
                :R.layout.item_app_info_grid,viewGroup,false),i);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        final T item=data.get(viewHolder.getAdapterPosition());
        viewHolder.title.setTextColor(activity.getResources().getColor((item.isRedMarked()?
                R.color.colorSystemAppTitleColor:R.color.colorHighLightText)));
        try {
            viewHolder.title.setText(EnvironmentUtil.getSpannableString(String.valueOf(item.getTitle()),highlightKeyword,Color.parseColor("#3498db")));
        } catch (Exception e) {
            e.printStackTrace();
            viewHolder.title.setText(String.valueOf(item.getTitle()));
        }
        viewHolder.icon.setImageDrawable(item.getIconDrawable());
        if(viewHolder.getViewType()==0){
            try {
                viewHolder.description.setText(EnvironmentUtil.getSpannableString(item.getDescription(),highlightKeyword,Color.parseColor("#3498db")));
            } catch (Exception e) {
                e.printStackTrace();
                viewHolder.description.setText(String.valueOf(item.getDescription()));
            }
            viewHolder.right.setText(Formatter.formatFileSize(activity,item.getSize()));
            viewHolder.right.setVisibility(isMultiSelectMode?View.GONE:View.VISIBLE);
            viewHolder.cb.setVisibility(isMultiSelectMode?View.VISIBLE:View.GONE);
            if(isMultiSelectMode)viewHolder.cb.setChecked(isSelected[viewHolder.getAdapterPosition()]);
        }else if(viewHolder.getViewType()==1){
            if(isMultiSelectMode)viewHolder.root.setBackgroundColor(activity.getResources().getColor(isSelected[viewHolder.getAdapterPosition()]
                    ?R.color.colorSelectedBackground
                    :R.color.colorCardArea));
            else viewHolder.root.setBackgroundColor(activity.getResources().getColor(R.color.colorCardArea));
        }

        viewHolder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMultiSelectMode){
                    isSelected[viewHolder.getAdapterPosition()]=!isSelected[viewHolder.getAdapterPosition()];
                    if(listener!=null)listener.onMultiSelectItemChanged(getSelectedItems(),getSelectedFileLength());
                    notifyItemChanged(viewHolder.getAdapterPosition());
                }else{
                    if(listener!=null)listener.onItemClicked(item,viewHolder,viewHolder.getAdapterPosition());
                }

            }
        });
        viewHolder.root.setOnLongClickListener(isMultiSelectMode?null:new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                isSelected=new boolean[data.size()];
                isSelected[viewHolder.getAdapterPosition()]=true;
                isMultiSelectMode=true;
                notifyDataSetChanged();
                if(listener!=null){
                    listener.onMultiSelectModeOpened();
                    listener.onMultiSelectItemChanged(getSelectedItems(),getSelectedFileLength());
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    public void setData(@Nullable List<T>data){
        this.data.clear();
        isSelected=null;
        if(data!=null)this.data.addAll(data);
        notifyDataSetChanged();
    }

    public void setHighlightKeyword(String keyword){
        this.highlightKeyword =keyword;
        notifyDataSetChanged();
    }

    public void setMultiSelectMode(boolean b){
        this.isMultiSelectMode=b;
        notifyDataSetChanged();
        if(listener!=null){
            if(b)listener.onMultiSelectModeOpened();
            //else listener.onMultiSelectModeClosed();
        }
    }

    public boolean getIsMultiSelectMode(){
        return isMultiSelectMode;
    }

    public void setSelectAll(boolean b){
        if(!isMultiSelectMode||isSelected==null)return;
        Arrays.fill(isSelected,b);
        notifyDataSetChanged();
        if(listener!=null)listener.onMultiSelectItemChanged(getSelectedItems(),getSelectedFileLength());
    }

    public void setToggleSelectAll(){
        if(!isMultiSelectMode||isSelected==null)return;
        for (boolean b : isSelected) {
            if (!b) {
                setSelectAll(true);
                return;
            }
        }
        setSelectAll(false);
    }

    /**
     * 获取已选择的项目
     */
    public @NonNull List<T> getSelectedItems(){
        ArrayList<T> list_selected=new ArrayList<>();
        if(isSelected==null||isSelected.length!=data.size())return list_selected;
        for(int i=0;i<data.size();i++){
            if(isSelected[i])list_selected.add(data.get(i));
        }
        return list_selected;
    }

    private long getSelectedFileLength(){
        long length=0;
        if(isSelected==null||isSelected.length!=data.size())return 0;
        for(int i=0;i<isSelected.length;i++){
            if(isSelected[i])length+=data.get(i).getSize();
        }
        return length;
    }

    public void setLayoutManagerAndView(int mode){
        if(mode==1){
            GridLayoutManager gridLayoutManager=new GridLayoutManager(activity,4);
            recyclerView.setLayoutManager(gridLayoutManager);
            this.mode=1;
        }else{
            LinearLayoutManager linearLayoutManager=new LinearLayoutManager(activity);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            this.mode=0;
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        final int viewType;
        public ImageView icon;
        TextView title;
        TextView description;
        TextView right;
        CheckBox cb;
        View root;
        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType =viewType;
            root=itemView.findViewById(R.id.item_app_root);
            icon=itemView.findViewById(R.id.item_app_icon);
            title=itemView.findViewById(R.id.item_app_title);
            if(viewType==0){
                description=itemView.findViewById(R.id.item_app_description);
                right=itemView.findViewById(R.id.item_app_right);
                cb=itemView.findViewById(R.id.item_app_cb);
            }
        }

        int getViewType() {
            return viewType;
        }
    }

    public interface ListAdapterOperationListener<T>{
        void onItemClicked(T item, ViewHolder viewHolder, int position);
        void onMultiSelectItemChanged(List<T> selected_items, long length);
        void onMultiSelectModeOpened();
    }
}
