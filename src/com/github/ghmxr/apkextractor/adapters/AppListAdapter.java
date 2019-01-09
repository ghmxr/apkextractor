package com.github.ghmxr.apkextractor.adapters;

import java.util.List;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class AppListAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private Context context;
	private List <AppItemInfo> applist;
	private boolean isMultiSelectMode=false;
	private boolean[] isSelected;	
	private boolean[] ifshowedAnim;
	private boolean ifAnim;
	
	public AppListAdapter(Context context,List<AppItemInfo> applist,boolean ifAnim){
		 inflater = LayoutInflater.from(context);
		 this.context=context;	
		 this.applist=applist;
		 this.isSelected=new boolean [this.applist.size()];		
		 this.ifshowedAnim=new boolean[this.applist.size()];
		 this.ifAnim=ifAnim;
	}
	
    @Override    
    public int getCount() {  
        // TODO Auto-generated method stub  
        return this.applist.size();  
    } 
    
    @Override     
    public Object getItem(int position) {  
       // TODO Auto-generated method stub  
        return null;  
    }  
  
    @Override    
    public long getItemId(int position) {  
       // TODO Auto-generated method stub  
        return position;  
    }  
       
    public void setMultiSelectMode(int position){   	
    	this.isSelected=new boolean [this.applist.size()];
    	this.isMultiSelectMode=true;  
    }
    
    public void cancelMutiSelectMode(){
    	this.isMultiSelectMode=false;
    	this.notifyDataSetChanged();
    }
    
    public void selectAll(){
    	if(this.isSelected!=null){
    		for(int i=0;i<this.isSelected.length;i++){
        		this.isSelected[i]=true;
        	}
        	this.notifyDataSetChanged();
    	}
    	
    }
    
    public void deselectAll(){
    	if(this.isSelected!=null){
    		for(int i=0;i<this.isSelected.length;i++){
        		this.isSelected[i]=false;
        	}
        	this.notifyDataSetChanged();
    	}
    }
    
    public int getSelectedNum(){
    	if(this.isMultiSelectMode&&this.isSelected!=null){
    		int num=0;
    		for(int i=0;i<this.isSelected.length;i++){
    			if(this.isSelected[i]){
    				num++;
    			}
    		}
    		return num;
    	}
    	else{
    		return 0;
    	}
    }
    
    public long getSelectedAppsSize(){
    	if(this.isSelected!=null){
    		long size=0;
    		for(int i=0;i<this.isSelected.length;i++){
    			if(this.isSelected[i]){
    				size+=this.applist.get(i).getPackageSize();
    			}
    		}
    		return size;
    	}
    	else{
    		return 0;
    	}
    }
    
    public void onItemClicked(int position){
    	if(position<0||position>this.applist.size()) return;
    	this.isSelected[position]=!this.isSelected[position];
    	this.notifyDataSetChanged();
    }
    
    public boolean [] getIsSelected(){
    	return this.isSelected;
    }
    
    public List<AppItemInfo> getAppList(){
    	return this.applist;
    }
  
    @Override 
    public View getView(int position, View convertView, ViewGroup parent) {  
            ViewHolder holder;  
            if (convertView == null) {                
                convertView = inflater.inflate(R.layout.layout_item_applist, parent,false);  
                holder = new ViewHolder();  
                holder.icon = (ImageView) convertView  
                        .findViewById(R.id.appimg);  
                holder.label = (TextView) convertView  
                        .findViewById(R.id.appname); 
                holder.packagename=(TextView)convertView.findViewById(R.id.apppackagename);
                holder.appsize=(TextView)convertView.findViewById(R.id.appsize);
                holder.select=(CheckBox)convertView.findViewById(R.id.select);
                convertView.setTag(holder);  
            } else {  
                holder = (ViewHolder) convertView.getTag();  
            }     
            holder.icon.setImageDrawable(this.applist.get(position).getIcon());  
            holder.label.setText(this.applist.get(position).getAppName().toString()+"("+this.applist.get(position).getVersion()+")"); 
            holder.packagename.setText(this.applist.get(position).getPackageName().toString());
            holder.appsize.setText(Formatter.formatFileSize(context, this.applist.get(position).getPackageSize()));            
            if(this.isMultiSelectMode&&this.isSelected!=null){
            	if(position<this.isSelected.length){
            		holder.select.setChecked(this.isSelected[position]);
            	}            	
            	holder.select.setVisibility(View.VISIBLE);
            	holder.appsize.setVisibility(View.GONE);
            	
            }
            else{
            	holder.select.setVisibility(View.GONE);
            	holder.appsize.setVisibility(View.VISIBLE);
            }
            
            if(!this.ifshowedAnim[position]&&this.ifAnim){
            	this.ifshowedAnim[position]=true;
            	Animation animation=AnimationUtils.loadAnimation(context, R.anim.anim_listitem_firstshow);
            	convertView.startAnimation(animation);
            }
            
            return convertView;  
   }
   
    public void onDataSetChanged(List<AppItemInfo> list){
    	
    }
    
        public static final  class ViewHolder{  
            private ImageView icon;  
            private TextView label;
            private TextView packagename;
            private TextView appsize;
            private CheckBox select;
        }
		 
  
    }  
