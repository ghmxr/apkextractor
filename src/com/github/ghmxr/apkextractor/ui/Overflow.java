package com.github.ghmxr.apkextractor.ui;

import java.lang.reflect.Field;

import android.app.Application;
import android.view.ViewConfiguration;

public class Overflow extends Application {
	 
	public void onCreate() {        
		try {  
			ViewConfiguration config = ViewConfiguration.get(this);  
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");  
			if (menuKeyField != null) {  
				menuKeyField.setAccessible(true);  
				menuKeyField.setBoolean(config, false);  
			}  
		}  
		catch (Exception ex) {  
			ex.printStackTrace();
		}  
     super.onCreate();  
	}  
	
	
}