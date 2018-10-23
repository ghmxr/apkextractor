package com.github.ghmxr.apkextractor.activities;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.adapters.FileListAdapter;
import com.github.ghmxr.apkextractor.data.FileItemInfo;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class FolderSelector extends BaseActivity implements Runnable{
	
	File path = new File(savepath);
	File path_ra;
	
	List <FileItemInfo> filelist=new ArrayList<FileItemInfo>();
	
	TextView currentpath;
	ListView listview;
	RelativeLayout rl_load,rl_face;
	SwipeRefreshLayout swl;
	
	FileListAdapter listadapter;
	
	Thread thread_refreshlist;
	
	private boolean isInterrupted=false;
	
	public static final int MESSAGE_REFRESH_FILELIST_COMPLETE = 0x0050;
	
	@Override
	public void onCreate(Bundle mybundle){
		super.onCreate(mybundle);		
		this.setContentView(R.layout.layout_folderselector);
		this.getActionBar().setDisplayShowHomeEnabled(false);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setTitle("选择路径");		
		this.currentpath=this.findViewById(R.id.folderselector_pathname);
		this.listview=this.findViewById(R.id.folderselector_filelist);
		this.rl_load=this.findViewById(R.id.folderselector_refresharea);
		this.rl_face=this.findViewById(R.id.folderselector_facearea);
		this.swl=this.findViewById(R.id.folderselector_swiperefreshlayout);
		
		this.swl.setColorSchemeColors(Color.parseColor(this.getResources().getString(R.color.color_actionbar)));
		this.swl.setSize(SwipeRefreshLayout.DEFAULT);
		this.swl.setDistanceToTriggerSync(100);
		this.swl.setProgressViewEndTarget(false, 200);
		
		this.listadapter=new FileListAdapter(this.filelist,this);
		this.path=new File (savepath);
		if(path.exists()&&!path.isDirectory()){
			path.delete();			
		}
		
		if(!path.exists()){
			path.mkdirs();
		}
		
		refreshList(true);
		
		this.swl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			
			@Override
			public void onRefresh() {
				// TODO Auto-generated method stub
				FolderSelector.this.refreshList(false);
			}
		});
		
		Toast.makeText(this, getResources().getString(R.string.activity_folder_selector_att), Toast.LENGTH_SHORT).show();
	}
	
	
	public void refreshList(boolean ifshowprogressbar){
		if(this.listview!=null){
			this.listview.setAdapter(null);
		}
		if(this.thread_refreshlist!=null){
			this.thread_refreshlist.interrupt();
			this.isInterrupted=true;
			this.thread_refreshlist=null;
		}
		
		this.thread_refreshlist=new Thread(this);
		this.isInterrupted=false;
		this.thread_refreshlist.start();
		if(this.rl_face!=null){
			this.rl_face.setVisibility(View.GONE);
		}
		if(this.rl_load!=null&&ifshowprogressbar){
			this.rl_load.setVisibility(View.VISIBLE);
		}
					
	}
	
	
	@Override
	public void processMessage(Message msg) {
		// TODO Auto-generated method stub
		switch(msg.what){
			default:break;
			case MESSAGE_REFRESH_FILELIST_COMPLETE:{				
				FolderSelector.this.setInfoAtt(this.path.getAbsolutePath());
				this.listadapter=new FileListAdapter(this.filelist,this);
				if(this.listview!=null){					
					this.listview.setAdapter(listadapter);
					this.thread_refreshlist=null;
					this.listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							// TODO Auto-generated method stub
							FolderSelector.this.path=FolderSelector.this.filelist.get(arg2).file;
							FolderSelector.this.setInfoAtt(FolderSelector.this.path.getAbsolutePath());
							FolderSelector.this.refreshList(true);
						}
					});
					
					this.listadapter.setOnRadioButtonClickListener(new FileListAdapter.onRadioButtonClickedListener() {
						
						@Override
						public void onClick(int position) {
							// TODO Auto-generated method stub
							FolderSelector.this.path_ra=FolderSelector.this.filelist.get(position).file;
							FolderSelector.this.setInfoAtt(FolderSelector.this.path_ra.getAbsolutePath());
							FolderSelector.this.listadapter.setSelected(position);
						}
					});
				}
				if(this.rl_load!=null){
					this.rl_load.setVisibility(View.GONE);
				}
				if(this.rl_face!=null){
					if(this.filelist.size()<=0){
						this.rl_face.setVisibility(View.VISIBLE);
					}
					else{
						this.rl_face.setVisibility(View.GONE);
					}
					
				}
				if(this.swl!=null){
					this.swl.setRefreshing(false);
				}
				
			}
			break;
		}
		
	}
	
	private void setInfoAtt(String att){
		if(att.length()>50){
			att="/..."+att.substring(att.length()-50);
		}
		if(this.currentpath!=null){
			this.currentpath.setText("当前路径： "+att);
		}
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(FolderSelector.this.path!=null&&FolderSelector.this.path.isDirectory()){
			
			File[] files=FolderSelector.this.path.listFiles();										
			FolderSelector.this.filelist=new ArrayList<FileItemInfo>();
			
			if(files!=null&&files.length>0){
				
				for(int i=0;i<files.length;i++){
					if(!this.isInterrupted){
						if(files[i].isDirectory()&&files[i].getName().indexOf(".")!=0){
							FileItemInfo fileitem=new FileItemInfo(files[i]);
							FolderSelector.this.filelist.add(fileitem);
							//Log.i("FolderSelector", "已添加文件夹"+fileitem.file.getName());
						}
					}
					else{
						break;
					}
				}
				
				Collections.sort(FolderSelector.this.filelist);
			}
			
			
		}
		if(!this.isInterrupted)
		sendEmptyMessage(MESSAGE_REFRESH_FILELIST_COMPLETE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.		
		getMenuInflater().inflate(R.menu.folderselector, menu);		
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
		switch(id){
			default:break;
			case R.id.folderselector_action_confirm:{								
				if(this.listadapter!=null){
					if(this.listadapter.getSelectedPosition()>=0&&this.path_ra!=null){
						savepath=this.path_ra.getAbsolutePath();
					}
					else{
						savepath=this.path.getAbsolutePath();
					}
				}
				else if(this.path!=null){
					savepath=this.path.getAbsolutePath();
				}					
				editor.putBoolean(PREFERENCE_IF_EDITED_SAVEPATH, true);
				editor.putString(PREFERENCE_APKPATH, savepath);
				editor.commit();
				Toast.makeText(this, "已更改APK导出路径至 "+savepath, Toast.LENGTH_SHORT).show();
				this.finish();								
			}
			break;
			case R.id.folderselector_action_cancel:{
				this.finish();
			}
			break;
			case R.id.folderselector_action_sort_ascending:{
				FileItemInfo.SortConfig=1;
				if(this.filelist!=null) Collections.sort(filelist);
				if(this.listadapter!=null){
					this.listadapter.setSelected(-1);
				}
			}
			break;
			case R.id.folderselector_action_sort_descending:{
				FileItemInfo.SortConfig=2;
				if(this.filelist!=null) Collections.sort(filelist);
				if(this.listadapter!=null){
					this.listadapter.setSelected(-1);
				}
			}
			break;
			case R.id.folderselector_action_newfolder:{
				LayoutInflater inflater = LayoutInflater.from(this);
				View dialogview=inflater.inflate(R.layout.layout_dialog_newfolder, null);
				final AlertDialog newfolder=new AlertDialog.Builder(this)
						.setTitle("新建文件夹")
						.setIcon(R.drawable.ic_newfolder)
						.setView(dialogview)
						.setPositiveButton("确定", null)
						.setNegativeButton("取消", null)
						.create();
				newfolder.show();
				
				final EditText edittext=(EditText)dialogview.findViewById(R.id.dialog_newfolder_edittext);								
				newfolder.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						String foldername=edittext.getText().toString().trim();
						File newfile = new File(FolderSelector.this.path.getAbsolutePath()+"/"+foldername);
						if(foldername.length()==0||foldername.equals("")){
							Toast.makeText(FolderSelector.this, "请输入有效的字符", Toast.LENGTH_SHORT).show();
							return;
						}else if(foldername.contains("?")||foldername.contains("\\")||foldername.contains("/")||foldername.contains(":")
								||foldername.contains("*")||foldername.contains("\"")||foldername.contains("<")||foldername.contains(">")
								||foldername.contains("|")){
							Toast.makeText(FolderSelector.this, "名称中包含非法字符（? \\ / : * \" < > |）", Toast.LENGTH_SHORT).show();							
							return;
						}else if(newfile.exists()){
							Toast.makeText(FolderSelector.this, "已存在重名文件夹 "+foldername, Toast.LENGTH_SHORT).show();
							return;
						}else{
							if(newfile.mkdirs()){
								FolderSelector.this.refreshList(true);
								newfolder.cancel();
							}
							else{
								Toast.makeText(FolderSelector.this, "Make Dirs error", Toast.LENGTH_SHORT).show();
								return;
							}
							
						}
						
					}
				});
				
				newfolder.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						newfolder.cancel();
					}
				});
						
						
			}
			
			break;
			case android.R.id.home:{
				backtoParent();
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void backtoParent(){
		if(this.path!=null){			
			if(this.path.getParentFile()==null){
				this.finish();
			}
			else{
				this.path=this.path.getParentFile();
				refreshList(true);
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			backtoParent();
			
			return true;
		}
		return false;
	}

}
