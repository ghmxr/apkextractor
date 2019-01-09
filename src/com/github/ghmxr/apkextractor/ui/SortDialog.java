package com.github.ghmxr.apkextractor.ui;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.data.AppItemInfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

public class SortDialog extends AlertDialog{

	
	public RadioButton r_default,r_a_appname,r_d_appname,r_a_size,r_d_size,r_a_date,r_d_date;	
	
	public SortDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		View dialogview = layoutInflater.inflate(R.layout.layout_dialog_sort,null);		
		this.setView(dialogview);
		this.r_default=(RadioButton)dialogview.findViewById(R.id.sort_ra_default);
		this.r_a_appname=(RadioButton)dialogview.findViewById(R.id.sort_ra_ascending_appname);
		this.r_d_appname=(RadioButton)dialogview.findViewById(R.id.sort_ra_descending_appname);
		this.r_a_size=(RadioButton)dialogview.findViewById(R.id.sort_ra_ascending_appsize);	
		this.r_d_size=(RadioButton)dialogview.findViewById(R.id.sort_ra_descending_appsize);
		this.r_a_date=(RadioButton)dialogview.findViewById(R.id.sort_ra_ascending_date);
		this.r_d_date=(RadioButton)dialogview.findViewById(R.id.sort_ra_descending_date);
		
		this.setTitle("—°‘Ò≈≈–Ú");
		this.setIcon(android.R.drawable.ic_menu_sort_by_size);
		this.setCancelable(true);
		this.setCanceledOnTouchOutside(true);
		this.setButton(AlertDialog.BUTTON_NEGATIVE, "»°œ˚", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				SortDialog.this.cancel();
			}
		});
		
		this.r_default.setChecked(false);
		this.r_a_appname.setChecked(false);
		this.r_d_appname.setChecked(false);
		this.r_a_size.setChecked(false);
		this.r_d_size.setChecked(false);
		this.r_a_date.setChecked(false);
		this.r_d_date.setChecked(false);
		switch(AppItemInfo.SortConfig){
			default:break;
			case 0:this.r_default.setChecked(true);break;
			case 1:this.r_a_appname.setChecked(true);break;
			case 2:this.r_d_appname.setChecked(true);break;
			case 3:this.r_a_size.setChecked(true);break;
			case 4:this.r_d_size.setChecked(true);break;
			case 5:this.r_a_date.setChecked(true);break;
			case 6:this.r_d_date.setChecked(true);break;
		}
		
	}

}
