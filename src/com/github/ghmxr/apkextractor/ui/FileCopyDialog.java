package com.github.ghmxr.apkextractor.ui;

import java.text.DecimalFormat;

import com.github.ghmxr.apkextractor.R;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


public class FileCopyDialog extends AlertDialog{
	
	ProgressBar pgbar;
	TextView text_speed,text_progress,text_curreninfo;
	private long progress=0,total=1024*100,speed=0;
	private int  percent;
	private Context context;

	public FileCopyDialog(Context context) {
		super(context);
		this.context=context;			
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		View dialogview = layoutInflater.inflate(R.layout.layout_dialog_filecopy,null);		
		this.setView(dialogview);
		this.pgbar=(ProgressBar)dialogview.findViewById(R.id.progressBar);
		this.text_curreninfo=(TextView)dialogview.findViewById(R.id.currentfile);
		this.text_progress=(TextView)dialogview.findViewById(R.id.copyprogress);
		this.text_speed=(TextView)dialogview.findViewById(R.id.copyspeed);
		setMax((int)total/1024);
	}
	
	public void setTextAtt(String title){
		this.text_curreninfo.setText(title);
		//this.setTitle(title);
	}
	
	public void setProgress(long bytes){
		this.progress=bytes;	
		refreshProgress(this.progress);
	}
	
	public void setMax(long Bytes){
		this.total=Bytes;
		this.pgbar.setMax((int)(this.total/1024));
	}
	
	public void setSpeed(long speedofBytes){
		this.speed=speedofBytes;
		refreshSpeed();
	}
	
	private void refreshProgress(long progressofBytes){	
		DecimalFormat dm=new DecimalFormat("#.00");			
		int percent=(int)(Double.valueOf(dm.format((double)this.progress/this.total))*100);		
		this.pgbar.setProgress((int)(progressofBytes/1024));	
		this.percent=percent;					
		this.text_progress.setText(Formatter.formatFileSize(this.context, this.progress)+"/"+Formatter.formatFileSize(this.context, this.total)+"("+this.percent+"%)");
		
	}
	
	private void refreshSpeed(){
		this.text_speed.setText(Formatter.formatFileSize(this.context, this.speed)+"/s");
			
	}

}
