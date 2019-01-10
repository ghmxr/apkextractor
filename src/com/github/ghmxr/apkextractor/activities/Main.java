package com.github.ghmxr.apkextractor.activities;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.adapters.AppListAdapter;
import com.github.ghmxr.apkextractor.data.AppItemInfo;
import com.github.ghmxr.apkextractor.data.Constants;
import com.github.ghmxr.apkextractor.ui.AppDetailDialog;
import com.github.ghmxr.apkextractor.ui.FileCopyDialog;
import com.github.ghmxr.apkextractor.ui.LoadListDialog;
import com.github.ghmxr.apkextractor.ui.SortDialog;
import com.github.ghmxr.apkextractor.utils.CopyFilesTask;
import com.github.ghmxr.apkextractor.utils.FileSize;
import com.github.ghmxr.apkextractor.utils.SearchTask;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends BaseActivity {
	
	boolean showSystemApp=false;	
	private boolean isMultiSelectMode=false,isSearchMode=false;
	private boolean isExtractSuccess=true; 
	private String errorMessage="";
	
	private List<AppItemInfo > list_extract_multi=new ArrayList<AppItemInfo>();
		
	Thread thread_getappinfo,thread_search,thread_extractapp;
	CopyFilesTask runnable_extractapp;
	SearchTask runnable_search;
	
	InputMethodManager inputmgr;
	
	Menu menu;	
	
	AppDetailDialog dialog_appdetail;
	LoadListDialog  dialog_loadlist;
	FileCopyDialog  dialog_copyfile;
	SortDialog dialog_sort;
	AlertDialog dialog_wait;
	
	private boolean shareAfterExtract=false;
		
	public static final int MESSAGE_SET_NORMAL_TEXTATT				= 12;
	public static final int MESSAGE_SORT_LIST_COMPLETE				= 13;
	
	public static final int MESSAGE_EXTRACT_SINGLE_APP				= 20;
	public static final int MESSAGE_EXTRACT_MULTI_APP				= 21;
				
	public static final int MESSAGE_SHARE_SINGLE_APP				= 22;
	public static final int MESSAGE_SHARE_MULTI_APP					= 23;
	
	public static final int MESSAGE_SEARCH_COMPLETE					= 30;
	
	public static final int MESSAGE_PERMISSION_DENIED				= 40;
	public static final int MESSAGE_PERMISSION_GRANTED				= 41;
	
	private static final int MESSAGE_REFRESH_DATA_OBB_SIZE=50;
	
	private static final int MESSAGE_EXTRA_MULTI_SHOW_SELECTION_DIAG=60;
	
	AppListAdapter listadapter;
	
	AdapterView.OnItemClickListener listener_listview_onclick_normalmode=new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
			// TODO Auto-generated method stub						
			if(Main.this.listadapter!=null){
				//final List<AppItemInfo> list = Main.this.listadapter.getAppList();							
				final AppItemInfo item=listadapter.getAppList().get(position);
				Main.this.dialog_appdetail=new AppDetailDialog(Main.this);
				Main.this.dialog_appdetail.setTitle(item.getAppName()+" "+item.getVersion());		
				Main.this.dialog_appdetail.setIcon(item.getIcon());
				Main.this.dialog_appdetail.setAppInfo(item.getVersionCode(), item.getLastUpdateTime(), item.getPackageSize());
				if(Build.VERSION.SDK_INT>=24) {
					Main.this.dialog_appdetail.setAPPMinSDKVersion(item.getMinSDKVersion());
				}						
				Main.this.dialog_appdetail.show();
				final Thread thread=new Thread(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						long dataSize=FileSize.getFileOrFolderSize(new File(StorageUtil.getMainStoragePath()+"/android/data/"+item.packageName));
						long obbSize=FileSize.getFileOrFolderSize(new File(StorageUtil.getMainStoragePath()+"/android/obb/"+item.packageName));
						Message msg=new Message();
						msg.what=MESSAGE_REFRESH_DATA_OBB_SIZE;
						msg.obj=new Long[]{dataSize,obbSize};
						sendMessage(msg);
					}
					
				});
				thread.start();
				dialog_appdetail.setOnCancelListener(new DialogInterface.OnCancelListener(){

					@Override
					public void onCancel(DialogInterface dialog) {
						// TODO Auto-generated method stub
						thread.interrupt();						
					}
					
				});
				Main.this.dialog_appdetail.area_extract.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						//extract
						if(Main.this.dialog_appdetail!=null) Main.this.dialog_appdetail.cancel();
						final boolean data=((CheckBox)dialog_appdetail.findViewById(R.id.dialog_appdetail_extract_data_cb)).isChecked();
						final boolean obb=((CheckBox)dialog_appdetail.findViewById(R.id.dialog_appdetail_extract_obb_cb)).isChecked();
						List<AppItemInfo> selectedList=new ArrayList<AppItemInfo>();
						selectedList.add(item);
												
						List<AppItemInfo> list_item=new ArrayList<AppItemInfo>();
						list_item.add(listsum.get(position));
						String duplicate=getDulplicateFileInfo(list_item,(data||obb)?"zip":"apk");
						//apkchecker.startCheck();								
						//if(StorageUtil.getSDAvaliableSize()<(item.getPackageSize()+1024*1024)){
						//	showStorageNotEnoughDialog();
						//}
						//else 
							if(duplicate.length()>0){
							
							new AlertDialog.Builder(Main.this)
							.setIcon(R.drawable.ic_warn)
							.setTitle(getResources().getString(R.string.activity_main_duplicate_title))
							.setCancelable(true)									
							.setMessage(getResources().getString(R.string.activity_main_duplicate_message)+"\n\n"+duplicate)
							.setPositiveButton(getResources().getString(R.string.dialog_button_positive), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub	
									shareAfterExtract=false;
									Message msg_extract=new Message();
									msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
									msg_extract.obj=new Integer[]{Integer.valueOf(position),data?1:0,obb?1:0};
									Main.this.processExtractMsg(msg_extract);
								}
							})
							.setNegativeButton(getResources().getString(R.string.dialog_button_negative), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									
								}
							})
							.show();
							
						}								
						else{
							shareAfterExtract=false;
							Message msg_extract=new Message();
							msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
							//msg_extract.obj=Integer.valueOf(position);
							msg_extract.obj=new Integer[]{Integer.valueOf(position),data?1:0,obb?1:0};
							Main.this.processExtractMsg(msg_extract);
						}
					}										
				});
				
				Main.this.dialog_appdetail.area_share.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if(Main.this.dialog_appdetail!=null) Main.this.dialog_appdetail.cancel();
																		
						if(settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_DIRECT){
							shareAfterExtract=false;
							Message msg_share=new Message();
							msg_share.what=Main.MESSAGE_SHARE_SINGLE_APP;
							msg_share.obj=Integer.valueOf(position);
							Main.sendMessage(msg_share);
						}else if(settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_AFTER_EXTRACT){
							//shareAfterExtract=true;							
							List<AppItemInfo> list_single=new ArrayList<AppItemInfo>();
							list_single.add(listsum.get(position));
							extractMultiSelectedApps(list_single, true);
						}
												
					}
				});
				
				Main.this.dialog_appdetail.area_detail.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if(Main.this.dialog_appdetail!=null) Main.this.dialog_appdetail.cancel();
						Intent appdetail = new Intent();  
						appdetail.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);  
						appdetail.setData(Uri.fromParts("package", item.getPackageName(), null));   
						startActivity(appdetail); 
					}
				});
				
			}												
		}
	};
	
	AdapterView.OnItemClickListener listener_listview_onclick_multiselectmode=new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub															
			Main.this.updateMuliSelectMode(position);
		}
	};
		
	AdapterView.OnItemLongClickListener listener_listview_onlongclick=new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			Main.this.startMultiSelectMode(position);
			return true;
		}
	};
	
	
	View.OnClickListener listener_extract=new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(listadapter==null) return;
			List<AppItemInfo> list_extract=new ArrayList<AppItemInfo>();
			for(int i=0;i<listadapter.getAppList().size();i++){					
				if(listadapter.getIsSelected()[i]) list_extract.add(listadapter.getAppList().get(i));
			}
			extractMultiSelectedApps(list_extract,false);

		}
	};
	View.OnClickListener listener_share=new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_DIRECT) Main.sendEmptyMessage(MESSAGE_SHARE_MULTI_APP);
			else if (settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_AFTER_EXTRACT){
				if(listadapter==null) return;
				List<AppItemInfo> list_share=new ArrayList<AppItemInfo>();
				for(int i=0;i<listadapter.getAppList().size();i++){					
					if(listadapter.getIsSelected()[i]) list_share.add(listadapter.getAppList().get(i));
				}
				extractMultiSelectedApps(list_share,true);
			}
		}
	};
	
	DialogInterface.OnClickListener listener_stopbutton=new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if(Main.this.thread_extractapp!=null){
				if(Main.this.runnable_extractapp!=null){
					Main.this.runnable_extractapp.setInterrupted();
				}
				Main.this.thread_extractapp.interrupt();
				Main.this.thread_extractapp=null;
			}
			Main.this.dialog_copyfile.cancel();
		}
	};
	
	SwipeRefreshLayout.OnRefreshListener listener_swr=new SwipeRefreshLayout.OnRefreshListener() {
		
		@Override
		public void onRefresh() {
			// TODO Auto-generated method stub
			if(Main.this.isMultiSelectMode){
				Main.this.closeMultiSelectMode();
			}
			Main.this.refreshList(false);
		}
	};
	
	
	private void extractMultiSelectedApps(List<AppItemInfo> extract_list,boolean ifshare){
		if(PermissionChecker.checkSelfPermission(Main.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
			ActivityCompat.requestPermissions(Main.this,new String[]{"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"} , 1);
			Toast.makeText(Main.this, getResources().getString(R.string.toast_request_rw_permission), Toast.LENGTH_SHORT).show();
			return;
		}
		//if(Main.this.listadapter==null)return;
		shareAfterExtract=ifshare;
		final List<AppItemInfo> list=new ArrayList<AppItemInfo> ();//Main.this.listadapter.getAppList();
		for(int i=0;i<extract_list.size();i++){
			list.add(new AppItemInfo(extract_list.get(i)));
		}
		list_extract_multi=list;								
		
			dialog_wait=new AlertDialog.Builder(Main.this)
					.setTitle(getResources().getString(R.string.activity_main_wait))
					.setView(LayoutInflater.from(Main.this).inflate(R.layout.layout_extract_multi_extra,null))
					.setCancelable(false)
					.show();
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub					
					long data=0,obb=0;
					for(AppItemInfo item:list_extract_multi){
						long data_get=FileSize.getFileOrFolderSize(new File(StorageUtil.getMainStoragePath()+"/android/data/"+item.packageName));
						long obb_get=FileSize.getFileOrFolderSize(new File(StorageUtil.getMainStoragePath()+"/android/obb/"+item.packageName));
						if(data_get>0) item.exportData=true;
						if(obb_get>0) item.exportObb=true;
						data+=data_get;
						obb+=obb_get;
					}
					Message msg=new Message();
					msg.what=MESSAGE_EXTRA_MULTI_SHOW_SELECTION_DIAG;
					msg.obj=new Long[]{data,obb};
					sendMessage(msg);
				}
				
			}).start();
	}
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);
		getActionBar().setDisplayShowHomeEnabled(true);
		getActionBar().setDisplayShowTitleEnabled(true);
		
		inputmgr=(InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		View actionbarSearchView = LayoutInflater.from(this).inflate(R.layout.layout_main_actionbar_search, null); 
		SearchView searchview=(SearchView)actionbarSearchView.findViewById(R.id.actionbar_main_search);
		int id = searchview.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);		
		TextView tv_searchview=(TextView)searchview.findViewById(id);
		if(tv_searchview!=null){			
			tv_searchview.setHintTextColor(Color.parseColor(this.getResources().getString(R.color.color_searchview_hinttext)));
		}	
		searchview.setIconifiedByDefault(false);
		
		getActionBar().setCustomView(actionbarSearchView); 				
		
		SwipeRefreshLayout swrlayout=(SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout);
		swrlayout.setColorSchemeColors(Color.parseColor(this.getResources().getString(R.color.color_actionbar)));
		swrlayout.setOnRefreshListener(listener_swr);
		
		findViewById(R.id.area_multiselect).setVisibility(View.GONE);
		findViewById(R.id.topArea).setVisibility(View.VISIBLE);
				
		dialog_loadlist=new LoadListDialog(this);
		dialog_loadlist.setTitle(getResources().getString(R.string.activity_main_loading));
		dialog_loadlist.setCancelable(false);
		dialog_loadlist.setCanceledOnTouchOutside(false);
		dialog_loadlist.setMax(getPackageManager().getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT).size());
		AppItemInfo.SortConfig=settings.getInt(Constants.PREFERENCE_SORT_CONFIG, 0);
		showSystemApp=settings.getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, false);
		((CheckBox)findViewById(R.id.showSystemAPP)).setChecked(showSystemApp);
		refreshList(true);						
		((CheckBox)findViewById(R.id.showSystemAPP)).setOnCheckedChangeListener(new OnCheckedChangeListener(){
			public void onCheckedChanged(CompoundButton button , boolean isChecked){
				if(Main.this.isMultiSelectMode){
					Main.this.closeMultiSelectMode();
				}
				showSystemApp=isChecked;
				editor.putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, isChecked);
				editor.apply();
				refreshList(true);								
			}
		});	
		storage_path=settings.getString(Constants.PREFERENCE_STORAGE_PATH, Constants.PREFERENCE_STORAGE_PATH_DEFAULT);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(!this.isSearchMode)
		Main.sendEmptyMessage(Main.MESSAGE_SET_NORMAL_TEXTATT);
	}
					
	private void showSearchView(){
		this.isSearchMode=true;
		listsearch.clear();
		this.listadapter=new AppListAdapter(this,listsearch,false);								
		final SwipeRefreshLayout swrlayout=(SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout);
		ListView applist=(ListView)findViewById(R.id.applist);
		View topArea=findViewById(R.id.topArea);
		applist.setAdapter(listadapter);
		//applist.setOnItemClickListener(null);
		applist.setOnItemLongClickListener(null);
		
		swrlayout.setEnabled(false);
		swrlayout.setOnRefreshListener(null);
		topArea.setVisibility(View.GONE);
			
		this.setMenuVisible(false);
		this.getActionBar().setDisplayShowCustomEnabled(true);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowHomeEnabled(false);
		
		SearchView searchview=(SearchView)getActionBar().getCustomView().findViewById(R.id.actionbar_main_search);
		searchview.setFocusable(true);
		searchview.setFocusableInTouchMode(true);
		searchview.requestFocus();
		searchview.setSubmitButtonEnabled(false);
		
		searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				// TODO Auto-generated method stub								
				Main.this.updateSearchList(newText);												
				return true;
			}
		});
		
		
		this.inputmgr.showSoftInput(searchview.findFocus(), 0);
				
	}
	
	private void closeSearchView(){
		this.isSearchMode=false;
		if(this.runnable_search!=null){
			this.runnable_search.setInterrupted();
		}
		if(this.thread_search!=null){
			this.thread_search.interrupt();
			this.thread_search=null;
		}
		
		
		this.listadapter=new AppListAdapter(this,listsum,true);			
		ProgressBar pg_search=(ProgressBar)findViewById(R.id.progressbar_search);
		ListView listview=(ListView)findViewById(R.id.applist);
		SwipeRefreshLayout swrlayout=(SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout);
		pg_search.setVisibility(View.GONE);
		
		listview.setAdapter(listadapter);
		
		swrlayout.setEnabled(true);
		swrlayout.setOnRefreshListener(listener_swr);
		
		listview.setAdapter(listadapter);
		listview.setOnItemClickListener(listener_listview_onclick_normalmode);
		listview.setOnItemLongClickListener(listener_listview_onlongclick);
					
		this.getActionBar().setDisplayShowCustomEnabled(false);
		this.getActionBar().setDisplayHomeAsUpEnabled(false);
		this.getActionBar().setDisplayShowHomeEnabled(true);
		this.setMenuVisible(true);
				
		findViewById(R.id.topArea).setVisibility(View.VISIBLE);
		Main.sendEmptyMessage(Main.MESSAGE_SET_NORMAL_TEXTATT);
		((SearchView)getActionBar().getCustomView().findViewById(R.id.actionbar_main_search)).setFocusable(false);	
		
		View view=this.getCurrentFocus();
		if(view!=null){			
			this.inputmgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
						
	}
	
	private void startMultiSelectMode(int position){
		ListView listview=(ListView)findViewById(R.id.applist);
		this.isMultiSelectMode=true;
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		listadapter.setMultiSelectMode(position);	
		updateMuliSelectMode(position);
		
		TextView selectall=findViewById(R.id.text_selectall);
		TextView deselectall=findViewById(R.id.text_deselectall);
		//TextView extract=findViewById(R.id.text_extract);
		//TextView share=findViewById(R.id.text_share);
		
		selectall.setClickable(true);
		selectall.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		
		selectall.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				listadapter.selectAll();
				updateMuliSelectMode(-1);
			}
		});
		
		deselectall.setClickable(true);
		deselectall.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		deselectall.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				listadapter.deselectAll();
				updateMuliSelectMode(-1);
			}
		});
						
		listview.setOnItemClickListener(listener_listview_onclick_multiselectmode);
		listview.setOnItemLongClickListener(null);
				
		View multiselectarea=findViewById(R.id.area_multiselect);
		Animation anim=AnimationUtils.loadAnimation(this, R.anim.anim_multiselectarea_entry);
		multiselectarea.startAnimation(anim);
		multiselectarea.setVisibility(View.VISIBLE);											
	}
	
	
	private void updateMuliSelectMode(int position){
		TextView appinst=(TextView)findViewById(R.id.appinst);
		TextView extract=(TextView)findViewById(R.id.text_extract);
		TextView share=(TextView)findViewById(R.id.text_share);
		listadapter.onItemClicked(position);
		appinst.setText(getResources().getString(R.string.activity_main_multiselect_att_head)+Main.this.listadapter.getSelectedNum()+getResources().getString(R.string.activity_main_multiselect_att_item)
				+"\n"+getResources().getString(R.string.activity_main_multiselect_att_end)+Formatter.formatFileSize(Main.this, listadapter.getSelectedAppsSize()));			
		extract.setText(Main.this.getResources().getString(R.string.button_extract)+"("+this.listadapter.getSelectedNum()+")");
		share.setText(Main.this.getResources().getString(R.string.button_share)+"("+this.listadapter.getSelectedNum()+")");
		extract.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		extract.getPaint().setAntiAlias(true);
		share.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		share.getPaint().setAntiAlias(true);
		 
		if(Main.this.listadapter.getSelectedNum()>0){
			extract.setClickable(true);
			share.setClickable(true);
			extract.setOnClickListener(listener_extract);
			share.setOnClickListener(listener_share);
			extract.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_true)));
			share.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_true)));
		}
		else{			
			share.setOnClickListener(null);
			extract.setOnClickListener(null);				
			extract.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_false)));
			share.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_false)));
		}
	}

	
	private void closeMultiSelectMode(){
		TextView extract=(TextView)findViewById(R.id.text_extract);
		TextView share=(TextView)findViewById(R.id.text_share);
		TextView selectall=(TextView)findViewById(R.id.text_selectall);
		TextView deselectall=(TextView)findViewById(R.id.text_deselectall);
    	isMultiSelectMode=false; 
    	extract.setOnClickListener(null);
    	share.setOnClickListener(null);
    	selectall.setOnClickListener(null);
    	deselectall.setOnClickListener(null);
    	findViewById(R.id.topArea).setVisibility(View.VISIBLE);
    	Main.sendEmptyMessage(MESSAGE_SET_NORMAL_TEXTATT);
    	
    	listadapter.cancelMutiSelectMode();
    	
    	ListView applist=(ListView)findViewById(R.id.applist);
		applist.setOnItemClickListener(listener_listview_onclick_normalmode);
    	applist.setOnItemLongClickListener(listener_listview_onlongclick);
    	Animation anim=AnimationUtils.loadAnimation(this, R.anim.anim_multiselectarea_exit);
    	
    	View multiselectarea=findViewById(R.id.area_multiselect);
		multiselectarea.startAnimation(anim);
    	multiselectarea.setVisibility(View.GONE);
    	Main.this.getActionBar().setDisplayHomeAsUpEnabled(false);   
    	
	}
	
	private void refreshList(boolean ifshowProcessDialog){
		((ListView)findViewById(R.id.applist)).setAdapter(null);
		Main.sendEmptyMessage(MESSAGE_SET_NORMAL_TEXTATT);
		((CheckBox)findViewById(R.id.showSystemAPP)).setEnabled(false);
		listsum = new ArrayList<AppItemInfo>();
		//AppItemInfoSortConfig.SortConfig=0;
		if(this.dialog_loadlist!=null&&ifshowProcessDialog){
			this.dialog_loadlist.show();
		}
		this.thread_getappinfo=new Thread(new Runnable(){
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				PackageManager packagemanager=getPackageManager();
				List<PackageInfo> packagelist = packagemanager.getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
		        for (int i = 0; i < packagelist.size(); i++) {
		            PackageInfo pak = (PackageInfo) packagelist.get(i); 
		            AppItemInfo appitem=new AppItemInfo();			  
		            // if()里的值如果<=0则为自己装的程序，否则为系统工程自带  
		            if(!Main.this.showSystemApp){
		            	if ((pak.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {                      
				            appitem.setIcon(packagemanager.getApplicationIcon(pak.applicationInfo));  		            
				            appitem.setAppName(packagemanager.getApplicationLabel(pak.applicationInfo).toString());  		             
				            appitem.setPackageName(pak.applicationInfo.packageName);  
				            appitem.setPackageSize(FileSize.getFileSize(pak.applicationInfo.sourceDir));
				            appitem.setResourcePath(pak.applicationInfo.sourceDir);
				            appitem.setVersion(pak.versionName);
				            appitem.setVersionCode(pak.versionCode);
				            appitem.setLastUpdateTime(pak.lastUpdateTime);
				            if(Build.VERSION.SDK_INT>=24)
				            appitem.setMinSDKVersion(pak.applicationInfo.minSdkVersion);
				            listsum.add(appitem);                    
		                }  
		            }
		            else{
		            	appitem.setIcon(packagemanager.getApplicationIcon(pak.applicationInfo));  		            
			            appitem.setAppName(packagemanager.getApplicationLabel(pak.applicationInfo).toString());  		             
			            appitem.setPackageName(pak.applicationInfo.packageName);  
			            appitem.setPackageSize(FileSize.getFileSize(pak.applicationInfo.sourceDir));
			            appitem.setResourcePath(pak.applicationInfo.sourceDir);
			            appitem.setVersion(pak.versionName);
			            appitem.setVersionCode(pak.versionCode);
			            appitem.setLastUpdateTime(pak.lastUpdateTime);
			            if(Build.VERSION.SDK_INT>=24)
			            appitem.setMinSDKVersion(pak.applicationInfo.minSdkVersion);
			            if ((pak.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) appitem.isSystemApp=true;
			            listsum.add(appitem);  
		            }		            
		            Message msg_thisloop = new Message();
		            Integer progress=i+1;           		            
		            msg_thisloop.what=MESSAGE_LOADLIST_REFRESH_PROGRESS;
		            msg_thisloop.obj=progress;
		            Main.sendMessage(msg_thisloop);
		            
		        }		       
		        Main.sendEmptyMessage(MESSAGE_LOADLIST_COMPLETE);
			}
			
		});
		this.thread_getappinfo.start();	
	}
	
	private void updateSearchList(String text){
		final String searchinfo=text.trim().toLowerCase(Locale.ENGLISH);
		findViewById(R.id.progressbar_search).setVisibility(View.VISIBLE);
		((ListView)findViewById(R.id.applist)).setAdapter(null);			
		if(this.thread_search!=null){
			if(this.runnable_search!=null){
				this.runnable_search.setInterrupted();
			}
			this.thread_search.interrupt();		
			this.thread_search=null;			
		}
		this.runnable_search=new SearchTask(searchinfo);
		this.thread_search=new Thread(this.runnable_search);
		this.thread_search.start();				
				
	}
	
	private void showStorageNotEnoughDialog(){
		new AlertDialog.Builder(Main.this)
		.setTitle(Main.this.getResources().getString(R.string.dialog_storage_notenough_title))
		.setIcon(R.drawable.ic_warn)
		.setCancelable(true)									
		.setMessage(Main.this.getResources().getString(R.string.dialog_storage_notenough_message))
		.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		})									
		.show();
	}
	
	
	private void processExtractMsg(Message msg){
		this.isExtractSuccess=true;
		this.errorMessage="";		 		
		if(msg.what==Main.MESSAGE_EXTRACT_SINGLE_APP||msg.what==Main.MESSAGE_EXTRACT_MULTI_APP){
			if(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
				Toast.makeText(this, getResources().getString(R.string.toast_request_rw_permission), Toast.LENGTH_SHORT).show();
				ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_EXTERNAL_STORAGE",Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
			}else{			
				sendMessage(msg);
			}
		}						
	}
	
	
	public void processMessage(Message msg){		
		switch(msg.what){
			default:break;						
			case MESSAGE_SET_NORMAL_TEXTATT:{
				if(!this.isMultiSelectMode&&!this.isSearchMode){
					((TextView)findViewById(R.id.appinst)).setText(Main.this.getResources().getString(R.string.text_appinst)+"\n"+this.getResources().getString(R.string.text_avaliableroom)+Formatter.formatFileSize(this, StorageUtil.getAvaliableSizeOfPath(storage_path)));				
				}				
			}
			break;
			
			case MESSAGE_LOADLIST_COMPLETE:{							
				if(this.dialog_loadlist!=null) this.dialog_loadlist.cancel();
					
				ListView applist=(ListView)findViewById(R.id.applist);
				listadapter=new AppListAdapter(this,listsum,true);
				applist.setAdapter(listadapter);
				applist.setDivider(null);
				applist.setOnItemClickListener(listener_listview_onclick_normalmode);
				applist.setOnItemLongClickListener(listener_listview_onlongclick);
								
				if(AppItemInfo.SortConfig!=0)Main.this.sortList();
				else{
					((SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout)).setRefreshing(false);
					((CheckBox)findViewById(R.id.showSystemAPP)).setEnabled(true);	
				}
			}
			break;
			case MESSAGE_SEARCH_COMPLETE:{
				
				this.listadapter=new AppListAdapter(this,listsearch,false);
				((ListView)findViewById(R.id.applist)).setAdapter(listadapter);
							
				findViewById(R.id.progressbar_search).setVisibility(View.GONE);
								
			}
			break;
			case MESSAGE_LOADLIST_REFRESH_PROGRESS:{
				if(dialog_loadlist!=null){
					Integer progress=(Integer)msg.obj;
					
					dialog_loadlist.setProgress(progress);
				}
			}
			break;
			case MESSAGE_EXTRACT_SINGLE_APP:{
				List<AppItemInfo> list;
				//boolean isSelected[];
				Integer position[]=(Integer[])msg.obj;								
				if(this.listadapter!=null){
					list=this.listadapter.getAppList();
					if(list!=null){
						if(list.size()>0){
							//isSelected=new boolean[list.size()];
							//isSelected[position]=true;
							List<AppItemInfo> exportlist=new ArrayList<AppItemInfo>();
							AppItemInfo item=new AppItemInfo(list.get(position[0]));
							if(position[1]==1) item.exportData=true;
							if(position[2]==1) item.exportObb=true;
							exportlist.add(item);
							this.runnable_extractapp=new CopyFilesTask(exportlist,this);
							this.thread_extractapp=new Thread(this.runnable_extractapp);
							this.dialog_copyfile=new FileCopyDialog(this);
							this.dialog_copyfile.setCancelable(false);
							this.dialog_copyfile.setCanceledOnTouchOutside(false);
							this.dialog_copyfile.setMax(list.get(position[0]).getPackageSize());
							this.dialog_copyfile.setIcon(list.get(position[0]).getIcon());
							this.dialog_copyfile.setTitle(getResources().getString(R.string.activity_main_extracting_title));
							this.dialog_copyfile.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.activity_main_stop), this.listener_stopbutton);
							this.dialog_copyfile.show();
							this.thread_extractapp.start();
						}
					}
				}																				
			}			
			break;
			
			case MESSAGE_EXTRACT_MULTI_APP:{							
				/*if(this.listadapter!=null){
					List<AppItemInfo> exportlist=new ArrayList<AppItemInfo>();
					boolean isSelected[]=listadapter.getIsSelected();
					for(int i=0;i<listadapter.getAppList().size();i++){	
						if(i<isSelected.length&&isSelected[i]){
							AppItemInfo item=new AppItemInfo(listadapter.getAppList().get(i));
							exportlist.add(item);
						}						
					}
									
				}*/
				Main.this.closeMultiSelectMode();
				if(list_extract_multi==null) return;
				String msg_dulplicate="";
				boolean ifdulplicate=false;				
				for(AppItemInfo item:list_extract_multi){
					List<AppItemInfo> checklist=new ArrayList<AppItemInfo> ();
					checklist.add(item);
					if(item.exportData||item.exportObb){												
						String duplicate=getDulplicateFileInfo(checklist,"zip");
						if(duplicate.length()>0){
							ifdulplicate=true;
							msg_dulplicate+=duplicate;
						}
						
					}else{						
						String duplicate=getDulplicateFileInfo(checklist,"apk");
						if(duplicate.length()>0){
							ifdulplicate=true;
							msg_dulplicate+=duplicate;
						}
					}
				}
				if(ifdulplicate){
					new AlertDialog.Builder(Main.this)
					.setIcon(R.drawable.ic_warn)
					.setTitle(getResources().getString(R.string.activity_main_duplicate_title))
					.setCancelable(true)									
					.setMessage(getResources().getString(R.string.activity_main_duplicate_message)+"\n\n"+msg_dulplicate)
					.setPositiveButton(getResources().getString(R.string.dialog_button_positive), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub									
							runnable_extractapp=new CopyFilesTask(list_extract_multi,Main.this);
							Main.this.thread_extractapp=new Thread(Main.this.runnable_extractapp);
							Main.this.dialog_copyfile=new FileCopyDialog(Main.this);
							//Main.this.dialog_copyfile.setMax(100);
							Main.this.dialog_copyfile.setTitle(getResources().getString(R.string.activity_main_extracting_title));
							Main.this.dialog_copyfile.setIcon(R.drawable.ic_launcher);
							Main.this.dialog_copyfile.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.activity_main_stop), Main.this.listener_stopbutton);
							Main.this.dialog_copyfile.setCancelable(false);
							Main.this.dialog_copyfile.setCanceledOnTouchOutside(false);
							Main.this.dialog_copyfile.show();
							Main.this.thread_extractapp.start();
						}
					})
					.setNegativeButton(getResources().getString(R.string.dialog_button_negative), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					})
					.show();
				}else{
					runnable_extractapp=new CopyFilesTask(list_extract_multi,Main.this);
					Main.this.thread_extractapp=new Thread(Main.this.runnable_extractapp);
					Main.this.dialog_copyfile=new FileCopyDialog(Main.this);
					//Main.this.dialog_copyfile.setMax(100);
					Main.this.dialog_copyfile.setTitle(getResources().getString(R.string.activity_main_extracting_title));
					Main.this.dialog_copyfile.setIcon(R.drawable.ic_launcher);
					Main.this.dialog_copyfile.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.activity_main_stop), Main.this.listener_stopbutton);
					Main.this.dialog_copyfile.setCancelable(false);
					Main.this.dialog_copyfile.setCanceledOnTouchOutside(false);
					Main.this.dialog_copyfile.show();
					Main.this.thread_extractapp.start();
				}
				
					
			}			
			break;
			case MESSAGE_COPYFILE_CURRENTAPP:{
				Integer i=(Integer)msg.obj;
				if(dialog_copyfile==null) return;
				if(runnable_extractapp==null) return;
				try{
					dialog_copyfile.setIcon(runnable_extractapp.applist.get(i).icon);
					dialog_copyfile.setTitle(getResources().getString(R.string.activity_main_extracting_title)+(i+1)+"/"+runnable_extractapp.applist.size()+" "+runnable_extractapp.applist.get(i).appName);
				}catch (Exception e){
					e.printStackTrace();
				}
				
			}
			break;
			case MESSAGE_COPYFILE_CURRENTFILE:{				
				String currentFile=(String)msg.obj;			
				if(this.dialog_copyfile!=null){
					((TextView)dialog_copyfile.findViewById(R.id.currentfile)).setText(currentFile);
				}				
			}
			break;
			case MESSAGE_COPYFILE_REFRESH_PROGRESS:{
				Long progress[] = (Long[])msg.obj;
				if(this.dialog_copyfile!=null){
					dialog_copyfile.setMax(progress[1]);
					this.dialog_copyfile.setProgress(progress[0]);
				}
				
			}
			break;
			case MESSAGE_COPYFILE_REFRESH_SPEED:{
				Long speed = (Long)msg.obj;
				if(this.dialog_copyfile!=null){
					this.dialog_copyfile.setSpeed(speed);
				}
			}
			break;
			case MESSAGE_COPYFILE_COMPLETE:{
				if(this.dialog_copyfile!=null){
					this.dialog_copyfile.cancel();
				}
				if(isExtractSuccess) Toast.makeText(this, getResources().getString(R.string.activity_main_complete)+savepath,Toast.LENGTH_LONG).show();
				if(!this.isSearchMode) {
					Main.sendEmptyMessage(Main.MESSAGE_SET_NORMAL_TEXTATT);
				}
				
				if(!this.isExtractSuccess){
					new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.attention))
					.setIcon(R.drawable.ic_warn)
					.setMessage(getResources().getString(R.string.activity_main_exception_head)+this.errorMessage+getResources().getString(R.string.activity_main_exception_end))
					.setPositiveButton(getResources().getString(R.string.dialog_button_positive), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					})
					.show();
				}
				this.isExtractSuccess=true;
				this.errorMessage="";
				if(this.shareAfterExtract&&settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT)==Constants.SHARE_MODE_AFTER_EXTRACT){
					try{
						List<String> paths=(List<String>)msg.obj;
						Intent i=new Intent();
						//i.setType("application/vnd.android.package-archive");
						i.setType("application/x-zip-compressed");
						if(paths.size()==1){							
							i.setAction(Intent.ACTION_SEND);
							//Uri uri=FileProvider.getUriForFile(this,getPackageName()+".FileProvider", new File(paths.get(0)));	
							Uri uri=Uri.fromFile(new File(paths.get(0)));
							i.putExtra(Intent.EXTRA_STREAM, uri);														
						}else{
							i.setAction(Intent.ACTION_SEND_MULTIPLE);
							ArrayList<Uri> uris=new ArrayList<Uri>();
							for(int n=0;n<paths.size();n++){
								//uris.add(FileProvider.getUriForFile(this, getPackageName()+".FileProvider", new File(paths.get(n))));
								uris.add(Uri.fromFile(new File(paths.get(n))));
							}
							i.putExtra(Intent.EXTRA_STREAM, uris);
						}
						i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share)); 
						i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share));  
						//i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						//i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(i,getResources().getString(R.string.share) ));
					}catch(Exception e){
						e.printStackTrace();
						Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
					}
					
				}
			}
			break;
			
			case MESSAGE_SHARE_SINGLE_APP:{
				if(this.listadapter!=null){
					Intent intent=new Intent(Intent.ACTION_SEND);
					Integer position = (Integer)msg.obj;
					int pos = position.intValue();
					List<AppItemInfo> list=this.listadapter.getAppList();
					String apkpath=list.get(pos).getResourcePath();				
					File apk=new File(apkpath);												
					Uri uri = Uri.fromFile(apk);					
					intent.setType("application/vnd.android.package-archive");
					intent.putExtra(Intent.EXTRA_STREAM,uri);
					intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share)+list.get(pos).getAppName()); 
					intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share)+list.get(pos).getAppName());  
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(Intent.createChooser(intent,  getResources().getString(R.string.share)+list.get(pos).getAppName()   )  );
					
				}
			}
			break;
			
			case MESSAGE_SHARE_MULTI_APP:{
				if(this.listadapter!=null){
					List<AppItemInfo> list=this.listadapter.getAppList();
					boolean isSelected[] = this.listadapter.getIsSelected();
					Intent intent;
					if(this.listadapter.getSelectedNum()>1){
						intent=new Intent(Intent.ACTION_SEND_MULTIPLE);
						ArrayList<Uri> uris= new ArrayList<Uri>();
						for(int i=0;i<list.size();i++){
							if(isSelected[i]){																							
								File file = new File(list.get(i).getResourcePath());
								if(file.exists()&&!file.isDirectory()){
									uris.add(Uri.fromFile(file));
								}																									
							}
						}						
						intent.putExtra(Intent.EXTRA_STREAM, uris);
						intent.setType("application/vnd.android.package-archive");
						intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.activity_main_share_title)); 
						intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.activity_main_share_title));  
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(intent,  getResources().getString(R.string.activity_main_share_title)  )  );
						
					}
					else if(this.listadapter.getSelectedNum()==1){
						intent = new Intent(Intent.ACTION_SEND);
						String path="",appname="";
						for(int j=0;j<list.size();j++){
							if(isSelected[j]){
								path=list.get(j).getResourcePath();
								appname=list.get(j).getAppName();
								break;
							}
						}
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
						intent.setType("application/vnd.android.package-archive");
						intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share)+appname); 
						intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share)+appname);  
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(intent,  getResources().getString(R.string.share)+appname   )  );
					}
															
				}
			}
			break;
			
			case MESSAGE_COPYFILE_FILE_NOTFOUND_EXCEPTION:{
				this.isExtractSuccess=false;
				this.errorMessage+=(String)msg.obj;
				this.errorMessage+="\n\n";
			break;					
			}
			
			case MESSAGE_COPYFILE_IOEXCEPTION:{
				this.isExtractSuccess=false;
				//Toast.makeText(Main.this, "出现IO异常", Toast.LENGTH_SHORT).show();
			}
			break;
			
			/*case MESSAGE_PERMISSION_DENIED:{
				Main.this.isCheckPermissionReady=false;
				//Toast.makeText(Main.this, "未获取到读写权限，请检查本应用权限", Toast.LENGTH_LONG).show();
				new AlertDialog.Builder(Main.this)
				.setTitle("未获得读写权限")
				.setIcon(R.drawable.ic_warn)
				.setMessage("未获取到读写权限，请检查本应用权限然后重试。")
				.setCancelable(true)
				.setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent appdetail = new Intent();  
						appdetail.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);  
						appdetail.setData(Uri.fromParts("package", Main.this.getApplicationContext().getPackageName(), null));   
						startActivity(appdetail); 
						//Main.this.finish();
						
					}
				})			
				.show();
			}
			break;*/		
			case MESSAGE_REFRESH_DATA_OBB_SIZE:{
				if(this.dialog_appdetail==null) return;
				if(msg.obj==null) return;
				Long[] sizes=(Long[])msg.obj;
				CheckBox cb_data=(CheckBox)dialog_appdetail.findViewById(R.id.dialog_appdetail_extract_data_cb);
				CheckBox cb_obb=(CheckBox)dialog_appdetail.findViewById(R.id.dialog_appdetail_extract_obb_cb);
				cb_data.setText("Data("+Formatter.formatFileSize(this, sizes[0])+")");
				cb_obb.setText("Obb("+Formatter.formatFileSize(this, sizes[1])+")");
				cb_data.setVisibility(View.VISIBLE);
				cb_obb.setVisibility(View.VISIBLE);
				dialog_appdetail.findViewById(R.id.dialog_appdetail_extract_extra_pb).setVisibility(View.GONE);
				cb_data.setEnabled(sizes[0]>0);
				cb_obb.setEnabled(sizes[1]>0);
			}
			break;
			
			case MESSAGE_EXTRA_MULTI_SHOW_SELECTION_DIAG:{
				if(dialog_wait==null) return;
				if(msg.obj==null) return;
				dialog_wait.cancel();
				dialog_wait=new AlertDialog.Builder(this)
						.setTitle(getResources().getString(R.string.activity_main_extract_multi_additional_title))
						.setView(LayoutInflater.from(this).inflate(R.layout.layout_extract_multi_extra, null))
						.setPositiveButton(getResources().getString(R.string.dialog_button_continue), null)
						.setNegativeButton(getResources().getString(R.string.dialog_button_negative), null)
						.show();
				final CheckBox cb_data=(CheckBox)dialog_wait.findViewById(R.id.extract_multi_data_cb);
				final CheckBox cb_obb=(CheckBox)dialog_wait.findViewById(R.id.extract_multi_obb_cb);
				Long[]values=(Long[])msg.obj;
				cb_data.setEnabled(values[0]>0);
				cb_obb.setEnabled(values[1]>0);
				if(values[0]<=0&&values[1]<=0){
					dialog_wait.cancel();
					Message msg_extract_multi=new Message();
					msg_extract_multi.what=MESSAGE_EXTRACT_MULTI_APP;					
					Main.this.processExtractMsg(msg_extract_multi);
				}
				else{
					dialog_wait.findViewById(R.id.extract_multi_wait).setVisibility(View.GONE);
					dialog_wait.findViewById(R.id.extract_multi_selections).setVisibility(View.VISIBLE);
					cb_data.setText("Data("+Formatter.formatFileSize(Main.this, values[0])+")");
					cb_obb.setText("Obb("+Formatter.formatFileSize(Main.this, values[1])+")");
					dialog_wait.setCancelable(true);
					dialog_wait.setCanceledOnTouchOutside(true);
					dialog_wait.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							
							dialog_wait.cancel();
							// TODO Auto-generated method stub
							if(!cb_data.isChecked()){
								for(AppItemInfo item:list_extract_multi){
									item.exportData=false;
								}
							}
							if(!cb_obb.isChecked()){
								for(AppItemInfo item:list_extract_multi){
									item.exportObb=false;
								}
							}
							Message msg_extract_multi=new Message();
							msg_extract_multi.what=MESSAGE_EXTRACT_MULTI_APP;					
							Main.this.processExtractMsg(msg_extract_multi);
						}
					});
						
					dialog_wait.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							dialog_wait.cancel();
						}
					});											
				
				}
				
			}
			break;
			case MESSAGE_SORT_LIST_COMPLETE:{
				Main.this.listadapter=new AppListAdapter(this,listsum,true);
				
				((ListView)findViewById(R.id.applist)).setAdapter(this.listadapter);
				
				((SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout)).setRefreshing(false);
				
				((CheckBox)findViewById(R.id.showSystemAPP)).setEnabled(true);
			}
			break;
		}
	
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {    
            if (this.isMultiSelectMode){
            	closeMultiSelectMode();
            } else if(this.isSearchMode){
            	closeSearchView();
            }
            else{           	
            	this.finish();            	
            }
            return true;
        }
		else {    
           return super.onKeyDown(keyCode, event);    
       }  
	}
	
	private void setMenuVisible(boolean isVisible){
		if(this.menu!=null){
			for(int i=0;i<this.menu.size();i++){
				this.menu.getItem(i).setEnabled(isVisible);
				this.menu.getItem(i).setVisible(isVisible);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.		
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu=menu;
		return true;
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu)  {  
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {  
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {  
                try {  
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);  
                    m.setAccessible(true);  
                    m.invoke(menu, true);  
                } catch (Exception e) {  
                }  
            }  
        }  
        return super.onMenuOpened(featureId, menu);  
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_about) {
			View dialogview=LayoutInflater.from(this).inflate(R.layout.layout_dialog_about, null);
			dialogview.findViewById(R.id.layout_about_donate).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					try{
						startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://qr.alipay.com/FKX08041Y09ZGT6ZT91FA5")));
					}catch (Exception e){
						e.printStackTrace();
					}
					
				}
			});
			AlertDialog dialog_about=new AlertDialog.Builder(Main.this)
					.setTitle(this.getResources().getString(R.string.dialog_about_title))
					.setIcon(R.drawable.ic_apkext)
					.setCancelable(true)	
					.setView(dialogview)
					//.setMessage(this.getResources().getString(R.string.dialog_about_message))
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							//Do nothing
						}
					}).create();
			
			dialog_about.show();
			return true;
		}
		if(id==android.R.id.home){
			if(isMultiSelectMode){
				closeMultiSelectMode();
			}else if(isSearchMode){
				closeSearchView();
			}
		
			return true;
		}
		
		if(id==R.id.action_search){
			//this.isSearchMode=true;
			if(this.isMultiSelectMode){
				this.closeMultiSelectMode();
			}			
			showSearchView();
			
		}
		if(id==R.id.action_sort){
			this.dialog_sort=new SortDialog(this);
			this.dialog_sort.show();
			dialog_sort.r_default.setChecked(AppItemInfo.SortConfig==0);
			dialog_sort.r_a_appname.setChecked(AppItemInfo.SortConfig==1);
			dialog_sort.r_d_appname.setChecked(AppItemInfo.SortConfig==2);
			dialog_sort.r_a_size.setChecked(AppItemInfo.SortConfig==3);
			dialog_sort.r_d_size.setChecked(AppItemInfo.SortConfig==4);
			dialog_sort.r_a_date.setChecked(AppItemInfo.SortConfig==5);
			dialog_sort.r_d_date.setChecked(AppItemInfo.SortConfig==6);
			this.dialog_sort.r_default.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(Main.this.isMultiSelectMode){
						Main.this.closeMultiSelectMode();
					}					
					AppItemInfo.SortConfig=0;
					((ListView)findViewById(R.id.applist)).setAdapter(null);
					Main.this.refreshList(true);					
					Main.this.dialog_sort.cancel();
				}
			});
			
			
			this.dialog_sort.r_a_appname.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfo.SortConfig=1;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			
			this.dialog_sort.r_d_appname.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfo.SortConfig=2;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_a_size.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfo.SortConfig=3;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_d_size.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfo.SortConfig=4;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_a_date.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfo.SortConfig=5;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_d_date.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfo.SortConfig=6;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			dialog_sort.setOnCancelListener(new DialogInterface.OnCancelListener(){

				@Override
				public void onCancel(DialogInterface dialog) {
					// TODO Auto-generated method stub
					editor.putInt(Constants.PREFERENCE_SORT_CONFIG, AppItemInfo.SortConfig);
					editor.apply();
				}
				
			});
			
		}
		
		if(id==R.id.action_editpath){
			if(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
				Toast.makeText(this, getResources().getString(R.string.toast_request_rw_permission), Toast.LENGTH_SHORT).show();
			}
			else{
				Intent i = new Intent();
				i.setClass(this, FolderSelector.class);
				startActivity(i);
			}
		}
		
		if(id==R.id.action_filename){
			final View dialogView=LayoutInflater.from(this).inflate(R.layout.layout_dialog_filename,null);
			final EditText edit_apk=(EditText)dialogView.findViewById(R.id.filename_apk);
			final EditText edit_zip=(EditText)dialogView.findViewById(R.id.filename_zip);
			final TextView preview=((TextView)dialogView.findViewById(R.id.filename_preview));
			
			edit_apk.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT));
			edit_zip.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT));
			preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
			
			if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
					&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
				dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
			}else{
				dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
			}
			
			if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
					&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
				dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.VISIBLE);
			}else{
				dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.GONE);
			}
			
			final AlertDialog dialog=new AlertDialog.Builder(this)
			.setTitle(getResources().getString(R.string.dialog_filename_title))
			.setView(dialogView)
			.setPositiveButton(getResources().getString(R.string.dialog_button_positive), null)
			.setNegativeButton(getResources().getString(R.string.dialog_button_negative), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			})
			.show();
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.getText().toString().trim().equals("")||edit_zip.getText().toString().trim().equals("")){
						Toast.makeText(Main.this,getResources().getString(R.string.dialog_filename_toast_blank) , Toast.LENGTH_SHORT).show();
						return;
					}
					
					String apk_replaced_variables=edit_apk.getText().toString().replace(Constants.FONT_APP_NAME, "").replace(Constants.FONT_APP_PACKAGE_NAME, "").replace(Constants.FONT_APP_VERSIONCODE, "").replace(Constants.FONT_APP_VERSIONNAME, "");
					String zip_replaced_variables=edit_zip.getText().toString().replace(Constants.FONT_APP_NAME, "").replace(Constants.FONT_APP_PACKAGE_NAME, "").replace(Constants.FONT_APP_VERSIONCODE, "").replace(Constants.FONT_APP_VERSIONNAME, "");
					if(apk_replaced_variables.contains("?")||apk_replaced_variables.contains("\\")||apk_replaced_variables.contains("/")||apk_replaced_variables.contains(":")||apk_replaced_variables.contains("*")||apk_replaced_variables.contains("\"")
							||apk_replaced_variables.contains("<")||apk_replaced_variables.contains(">")||apk_replaced_variables.contains("|")
							||zip_replaced_variables.contains("?")||zip_replaced_variables.contains("\\")||zip_replaced_variables.contains("/")||zip_replaced_variables.contains(":")||zip_replaced_variables.contains("*")||zip_replaced_variables.contains("\"")
							||zip_replaced_variables.contains("<")||zip_replaced_variables.contains(">")||zip_replaced_variables.contains("|")){
						Toast.makeText(Main.this, getResources().getString(R.string.activity_folder_selector_invalid_foldername), Toast.LENGTH_SHORT).show();
						return;
					}
					editor.putString(Constants.PREFERENCE_FILENAME_FONT_APK, edit_apk.getText().toString());
					editor.putString(Constants.PREFERENCE_FILENAME_FONT_ZIP, edit_zip.getText().toString());
					editor.apply();
					dialog.cancel();
				}
			});
			edit_apk.addTextChangedListener(new TextWatcher(){

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void afterTextChanged(Editable s) {
					// TODO Auto-generated method stub
					preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
					if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
							&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
						dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
					}else{
						dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
					}
				}
				
			});
			edit_zip.addTextChangedListener(new TextWatcher(){

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void afterTextChanged(Editable s) {
					// TODO Auto-generated method stub
					preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
					if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
							&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
						dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.VISIBLE);
					}else{
						dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.GONE);
					}
				}
				
			});
			
			dialogView.findViewById(R.id.filename_appname).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.isFocused()){					
						edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_NAME);
					}
					if(edit_zip.isFocused()){						
						edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_NAME);
					}
				}
			});
			
			dialogView.findViewById(R.id.filename_packagename).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.isFocused()){					
						edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_PACKAGE_NAME);
					}
					if(edit_zip.isFocused()){						
						edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_PACKAGE_NAME);
					}
				}
			});
			
			dialogView.findViewById(R.id.filename_version).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.isFocused()){					
						edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_VERSIONNAME);
					}
					if(edit_zip.isFocused()){						
						edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_VERSIONNAME);
					}
				}
			});
			
			dialogView.findViewById(R.id.filename_versioncode).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.isFocused()){					
						edit_apk.getText().insert(edit_apk.getSelectionStart(), Constants.FONT_APP_VERSIONCODE);
					}
					if(edit_zip.isFocused()){						
						edit_zip.getText().insert(edit_zip.getSelectionStart(), Constants.FONT_APP_VERSIONCODE);
					}
				}
			});
			
			dialogView.findViewById(R.id.filename_connector).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.isFocused()){					
						edit_apk.getText().insert(edit_apk.getSelectionStart(), "-");
					}
					if(edit_zip.isFocused()){						
						edit_zip.getText().insert(edit_zip.getSelectionStart(), "-");
					}
				}
			});
			
			dialogView.findViewById(R.id.filename_upderline).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(edit_apk.isFocused()){					
						edit_apk.getText().insert(edit_apk.getSelectionStart(), "_");
					}
					if(edit_zip.isFocused()){						
						edit_zip.getText().insert(edit_zip.getSelectionStart(), "_");
					}
				}
			});
			
		}
		if(id==R.id.action_sharemode){
			int mode=settings.getInt(Constants.PREFERENCE_SHAREMODE, Constants.PREFERENCE_SHAREMODE_DEFAULT);
			View dialogView=LayoutInflater.from(this).inflate(R.layout.layout_dialog_sharemode,null,false);
			RadioButton ra_direct=((RadioButton)dialogView.findViewById(R.id.share_mode_direct_ra));
			RadioButton ra_after_extract=((RadioButton)dialogView.findViewById(R.id.share_mode_after_extract_ra));
			ra_direct.setChecked(mode==Constants.SHARE_MODE_DIRECT);
			ra_after_extract.setChecked(mode==Constants.SHARE_MODE_AFTER_EXTRACT);
			final AlertDialog dialog=new AlertDialog.Builder(this)
			.setTitle(getResources().getString(R.string.action_sharemode))
			.setView(dialogView)
			.show();
			dialogView.findViewById(R.id.share_mode_direct).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					editor.putInt(Constants.PREFERENCE_SHAREMODE, Constants.SHARE_MODE_DIRECT);
					editor.apply();
					dialog.cancel();
				}
			});
			dialogView.findViewById(R.id.share_mode_after_extract).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					editor.putInt(Constants.PREFERENCE_SHAREMODE, Constants.SHARE_MODE_AFTER_EXTRACT);
					editor.apply();
					dialog.cancel();
				}
			});
		}
		return super.onOptionsItemSelected(item);
	}
	
	private String getFormatedExportFileName(String apk,String zip){
		final String PREVIEW_APPNAME=getResources().getString(R.string.dialog_filename_preview_appname);
		final String PREVIEW_PACKAGENAME=getResources().getString(R.string.dialog_filename_preview_packagename);
		final String PREVIEW_VERSION=getResources().getString(R.string.dialog_filename_preview_version);
		final String PREVIEW_VERSIONCODE=getResources().getString(R.string.dialog_filename_preview_versioncode);
		return getResources().getString(R.string.preview)+":\n\nAPK:  "+apk.toString().replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME)
				.replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME).replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE).replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION)+".apk\n\n"
				+"ZIP:  "+zip.toString().replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME)
				.replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME).replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE).replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION)+".zip";
	}
	
	private void sortList(){
		if(Main.this.listadapter!=null&&!Main.this.isSearchMode){
			if(Main.this.isMultiSelectMode){
				Main.this.closeMultiSelectMode();
			}
			((CheckBox)findViewById(R.id.showSystemAPP)).setEnabled(false);
			((SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout)).setRefreshing(true);
			((ListView)findViewById(R.id.applist)).setAdapter(null);
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					synchronized(Main.class){
						Collections.sort(listsum);
						sendEmptyMessage(MESSAGE_SORT_LIST_COMPLETE);
					}					
				}
				
			}).start();
			
			
			
		}
	}
	
	public String getDulplicateFileInfo(List<AppItemInfo> items,String extension){
		try{
			String result="";
			for(AppItemInfo item: items){
				File file=new File (getAbsoluteWritePath(this,item,extension));
				if(file.exists()&&!file.isDirectory()){
					result+=file.getAbsolutePath();
					result+="\n\n";
				}
			}
			return result;
		}catch(Exception e){e.printStackTrace();}
		return "";
	}
	
	@Override
	public void finish(){
		super.finish();		
	}
	
}