package com.github.ghmxr.apkextractor.ui;

import java.text.DecimalFormat;

import com.github.ghmxr.apkextractor.R;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadListDialog extends AlertDialog {
	
	ProgressBar pgbar;
	TextView textview_percent,textview_progress;
	int progress=0,max=0;

	public LoadListDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		View dialogview = layoutInflater.inflate(R.layout.layout_dialog_loadlist,null);		
		this.setView(dialogview);
		this.pgbar=(ProgressBar)dialogview.findViewById(R.id.dialog_loadlist_pgbar);
		this.textview_percent=(TextView)dialogview.findViewById(R.id.dialog_loadlist_textview_percent);
		this.textview_progress=(TextView)dialogview.findViewById(R.id.dialog_loadlist_textview_progress);		
	}
	
	public void setMax(int max){
		this.max=max;
		this.pgbar.setMax(max);
	}
	
	public void setProgress(int progress){
		this.progress=progress;
		refreshProgress();
	}
	
	private void refreshProgress(){
		DecimalFormat dm=new DecimalFormat("#.00");
		int percent=(int)(Double.valueOf(dm.format((double)this.progress/this.max))*100);			
		this.pgbar.setProgress(this.progress);
		this.textview_percent.setText(percent+"%");
		this.textview_progress.setText(this.progress+"/"+this.max);
	}
	
	
	

}
