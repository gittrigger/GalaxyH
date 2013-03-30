package com.havenskys.galaxy.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.havenskys.galaxy.Custom;
import com.havenskys.galaxy.R;

public class TabStart extends TabActivity {
	
	private static String TAG = "TabStart";
	
	private TabHost mTabHost;
	private Bundle mIntentExtras;
	private NotificationManager mNM;
	//private Handler mHandler;
	
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	private Custom mLog;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mLog = new Custom(this, TAG + " onCreate() 44 ");
        setContentView(R.layout.tabstart);
        
        mLog.w(TAG,"onCreate() ++++++++++++++++++++++++++++++++");

        mIntentExtras = getIntent().getExtras();
        long id = mIntentExtras != null ? mIntentExtras.getLong("id") : -1;
        
        mLog.w(TAG,"onCreate() 77");
	        
        mTabHost = getTabHost();
        //mTabHost.setForeground(drawable);
        
        mLog.w(TAG,"onCreate() 81");
        
        mLog.w(TAG,"onCreate() 84");
        {
        	Intent listView = new Intent(this, PersonSearch.class);
	        listView.putExtra("id", id);
	        
	        mLog.w(TAG,"onCreate() 86");
	        TabSpec t1 = mTabHost.newTabSpec("listview");
	        
	        mLog.w(TAG,"onCreate() 88");
	        t1.setIndicator(null, getResources().getDrawable(android.R.drawable.ic_menu_search));//Custom.TOPICON
	        mLog.w(TAG,"onCreate() 90");
	        t1.setContent(listView);
	        mLog.w(TAG,"onCreate() 92");
	        mTabHost.addTab(t1);
			mTabHost.setCurrentTab(1);
			
        }
        //mTabHost.setCurrentTab(tab);

        mLog.w(TAG,"onCreate() 97");
        
    	{	
		int topimage = R.drawable.microscope;
		//int topimage = R.drawable.microscope;
		//int topimage = android.R.drawable.ic_menu_compass;
		
        Intent browseView = new Intent(this, PersonView.class);
        	//browseView.putExtra("id", id);
			//listView.setClass(this, browseView.class);
        TabSpec t2 = mTabHost.newTabSpec("browse");
        	t2.setIndicator(null, getResources().getDrawable(topimage));
        	t2.setContent(browseView);
        mTabHost.addTab(t2);
    	}
        
        {
	        Intent listView2 = new Intent(this, ListPeople.class);
	        //listView2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        
        	//listView.putExtra("id", id);
        	//listView.setClass(this, listSave.class);
	        TabSpec t = mTabHost.newTabSpec("listpeople");
	        
	        //t.setIndicator(null, getResources().getDrawable(android.R.drawable.ic_menu_save));
	        //t.setIndicator(null, getResources().getDrawable(R.drawable.briefcase));
	        t.setIndicator(null, getResources().getDrawable(android.R.drawable.ic_menu_recent_history));
	        	t.setContent(listView2);
	        mTabHost.addTab(t);
        }
        
        {
	        Intent listView2 = new Intent(this, ConfigureLogin.class);
	        //listView2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        
        	//listView.putExtra("id", id);
        	//listView.setClass(this, listOut.class);
	        TabSpec t = mTabHost.newTabSpec("listout");
	        	t.setIndicator(null, getResources().getDrawable(R.drawable.outhouse));
	        	//t.setIndicator(null, getResources().getDrawable(R.drawable.tab_out));
	        	t.setContent(listView2);
	        mTabHost.addTab(t);
        }
        
        mLog.w(TAG,"onCreate() 124");
        
        
        
        
        //Thread getlatestThread = new Thread(){ public void run(){ getlatest(); } };
        //getlatestThread.start();
        
        
        mTabHost.setOnTabChangedListener(new OnTabChangeListener(){

			public void onTabChanged(String tabname) {
				int tab = mSharedPreferences.contains("tab") ? mSharedPreferences.getInt("tab",0) : 0;
				int cur = mTabHost.getCurrentTab();
				mLog.w(TAG, "onTabChanced() arg0("+tabname+") currenttab("+cur+") lasttab("+tab+")");
				mPreferencesEditor.putInt("lasttab", tab);
				mPreferencesEditor.putInt("tab", cur ).commit();
				
				if( cur == 0 ){
					
				}
				
			}
        	
        });
    	
        mLog.w(TAG,"onCreate() 149");
        
        Thread getloadRunner = new Thread(){
			public void run(){
				for(;;){
					double load = mLog.getload(TAG + " Custom(getloadRunner) 55");
					if( load < 4 ){
						mLog.i(TAG,"Current System Load " + load);
					} else if( load < 6 ){
						mLog.w(TAG,"Current System Load " + load);
					} else {
						mLog.e(TAG,"Current System Load " + load);
					}
					SystemClock.sleep(60 * 1000);
				}
				
				
			}
		};
		//getloadRunner.start();
        
		mLog.w(TAG,"onCreate() 156");
		
		
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mLog.setNotificationManager(mNM);
        
        mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
  	  	mPreferencesEditor = mSharedPreferences.edit();
  	  	mLog.setSharedPreferences(mSharedPreferences, mPreferencesEditor);
        
        //mHandler = new Handler();
        
        
        boolean stoprequest = mIntentExtras != null ? mIntentExtras.getBoolean("stoprequest") : false;
		int tab = mSharedPreferences.contains("tab") ? mSharedPreferences.getInt("tab",0) : 0;
		
		mLog.w(TAG,"onCreate() 173");
		
        if( id > -1 ){
			mPreferencesEditor.putLong("id", id).commit();
        }
    	//mPreferencesEditor.putLong("id", 0).commit();
		if( stoprequest ){
			mPreferencesEditor.putBoolean("stoprequest", false).commit();
		}else{
			//int total = mSharedPreferences.contains("total") ? mSharedPreferences.getInt("total",-1) : -1;
			//if( total == -1 ){
				//Toast.makeText(this, "Loading Service for the First Time", 5000).show();
				//serviceRestart("onCreate() 61");
			//}
			
			mLog.i(TAG,"onCreate() get AlarmManager");
			mAlM = (AlarmManager) getSystemService(ALARM_SERVICE);
			Intent resetservice = new Intent();
	        //com.havenskys.newsbite.SERVICE_RESET
			resetservice.setAction("com.havenskys.newsbite.SERVICE_START");
			PendingIntent service3 = PendingIntent.getBroadcast(this, 0, resetservice, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_CANCEL_CURRENT);
			mAlM.set(AlarmManager.RTC_WAKEUP,( System.currentTimeMillis() + (2 * 1000) ), service3);
			
		}
        
        mLog.w(TAG,"onCreate() 189");
        
        if( id > 0 ){
			mNM.cancel(Custom.NOTIFY_ID_ARTICLE);
			mNM.cancel((int) id);
			mTabHost.setCurrentTab(1);
		}else{
			mTabHost.setCurrentTab(tab);
		}
        
        
		
    }

    private AlarmManager mAlM;
    
	@Override
	protected void onStart() {
		mLog.w(TAG,"onStart() ++++++++++++++++++++++++++++++++");
		super.onStart();
		
		
		//Intent service = new Intent();
		//service.setClass(this, SyncService.class);
		//service.putExtra("com.havenskys.newsbite.who", TAG + " onStart() 214");
	    //startService(service);
	    
		/*/
		long syncstart = mSharedPreferences.getLong("syncstart", -1);
		long lastactive = mSharedPreferences.getLong("lastfeedactive", -1);
		long lowmemory = mSharedPreferences.getLong("lowmemory", -1);
		//mPreferencesEditor.putLong("lowmemory", System.currentTimeMillis()).commit();
		
		if( syncstart < (System.currentTimeMillis() - 60 * 1000) && lowmemory < (System.currentTimeMillis() - 2 * 60 * 1000 ) ){
		}
		//*/

	}

	
	@Override
	protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
		
		mLog.w(TAG,"onChildTitleChanged() tabCount("+mTabHost.getTabWidget().getChildCount()+") childActivity("+childActivity.getClass().getName()+") title("+title+") ++++++++++++++++++++++++++++++++");
		
		String name = childActivity.getClass().getName();
				
		//if( mTabHost.getTabWidget().getChildCount() == 1 ){
        	//mNM.cancel(Custom.NOTIFY_ID_ARTICLE);
		//}
		
		if( name.contains("browseView") ){
			if( title.length() == 0 ){
				//mTabHost.requestFocusFromTouch();
				int lasttab = mSharedPreferences.contains("lasttab") ? mSharedPreferences.getInt("lasttab",0) : 0;
				if( lasttab == 1 ){ lasttab = 0; }
				mLog.w(TAG,"onChildTitleChanged() lasttab("+lasttab+")");
				mTabHost.setCurrentTab(lasttab);
				return;
			}
			return;
		}
		
		
		if( name.contains("listView") || name.contains("listSave") || name.contains("listOut") ){
		
			if( title.length() == 0 ){
				mTabHost.requestFocusFromTouch();
				int tab = mSharedPreferences.contains("tab") ? mSharedPreferences.getInt("tab",0) : 0;
				mTabHost.setCurrentTab(tab);
				mPreferencesEditor.remove("id").commit();
				return;
			}
			
			long id = Long.parseLong(title.toString());
			mPreferencesEditor.putLong("id", id); mPreferencesEditor.commit();
			
			if( id > 0 ){
	        	//mNM.cancel(Custom.NOTIFY_ID_ARTICLE);
		        //mTabHost.getChildAt(1).setTag(id);
		        
		        //mTabHost.getChildAt(2).setTag(id);
		        
	        	/*
		        int topimage = android.R.drawable.ic_menu_compass;
		        int feedid = 0;
		        Cursor dataCursor = SqliteWrapper.query(this, this.getContentResolver(), DataProvider.CONTENT_URI 
		        		,new String[]{"type"}
		        		,"_id = " + id
		        		,null
		        		,null //orderby //"date desc"
		        		);
		        
		        if( dataCursor != null ){
		        	if( dataCursor.moveToFirst() ){
		        		feedid = dataCursor.getInt(0);
		    			topimage = Integer.parseInt(mLog.dataFeed[feedid][2]);
		        	}
		        }
		        
		        if( mTabHost.getChildCount() > 1 ){
			        TabWidget tw = mTabHost.getTabWidget();
			        tw.removeViewAt(1);
		        }
		        
		        Intent browseView = new Intent(this, browseView.class);
	        	//browseView.putExtra("id", id);
		        TabSpec t2 = mTabHost.newTabSpec("browse");
		        t2.setIndicator(null, getResources().getDrawable(topimage));
		        t2.setContent(browseView);
		        mTabHost.addTab(t2);
		        
		        //mTabHost.getChildAt(1).
		        //*/
		        
		        mTabHost.setCurrentTab(1);
		        
		        
			}
		}
		
		super.onChildTitleChanged(childActivity, title);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		mLog.w(TAG, "onContextItemSelected() ");
		return super.onContextItemSelected(item);
	}

	@Override
	public void onContextMenuClosed(Menu menu) {
		// TODO Auto-generated method stub
		mLog.w(TAG, "onContextMenuClosed() ");
		super.onContextMenuClosed(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		mLog.w(TAG, "onCreateContextMenu() ");
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		mLog.w(TAG, "onCreateOptionsMenu() ");
		
		//menu.add(Menu.NONE, 101, 0, "View Source Webpage").setIcon(android.R.drawable.ic_menu_view);
		
		//menu.add(Menu.NONE, 202, 1, "Email " + mLog.WHO).setIcon(Custom.LITTLEICON);
		
		menu.add(Menu.NONE, 1, 2, "Doc Chomps Software")
		.setIcon(R.drawable.docdot);
		
		menu.add(Menu.NONE, 2, 3, "Help")
			.setIcon(android.R.drawable.ic_menu_help);
		
		menu.add(Menu.NONE, 201, 4, "Email Support")
			.setIcon(android.R.drawable.ic_menu_send);
		
		menu.add(Menu.NONE, 202, 5, "Configure Login")
		.setIcon(android.R.drawable.ic_menu_preferences);
	
		{
			int groupNum = 31;
			SubMenu submenu = menu.addSubMenu(Menu.NONE, groupNum, 6, "Configure Sync");
			submenu.setIcon(android.R.drawable.ic_menu_preferences);
			submenu.add(groupNum,0,0,"No");
			submenu.add(groupNum,1,2,"Yes");
			boolean syncandroid = mSharedPreferences.getBoolean("syncandroid", false);
			submenu.setGroupCheckable(groupNum, true, true);
			submenu.setGroupEnabled(groupNum, true);
			MenuItem menuItem = null;
			if( syncandroid ){
				menuItem = submenu.findItem(1);
			}else{
				menuItem = submenu.findItem(0);
			}
			menuItem.setChecked(true);
		}
		

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		mLog.w(TAG, "onOptionsItemSelected() groupid(" + item.getGroupId() + ") itemid("+item.getItemId()+") title("+item.getTitle()+")");
		if( item.getGroupId() == 31 ){ // Sync Android
			Boolean syncandroid = false;
			if( item.getItemId() == 1 ){
				syncandroid = true;
				Toast.makeText(this, "Sync Android Activated", 1800).show();
			}else{
				Toast.makeText(this, "Sync Android De-activated", 1800).show();
			}
			mPreferencesEditor.putBoolean("syncandroid", syncandroid).commit();
			
		}
		
		switch(item.getItemId()){
		case 1: //About
			{
				//Intent jump = new Intent(this, About.class);
				//startActivity(jump);
			}
			break;
		case 2: //Help
			{
				//Intent jump = new Intent(this, Help.class);
				//startActivity(jump);
			}
			break;
		case 101:
			//{
				//Intent jump = new Intent(Intent.ACTION_VIEW, Uri.parse(Custom.BASEURL));
				//startActivity(jump);
			//}
			break;
		case 201:
			{
				Intent jump = new Intent(Intent.ACTION_SEND);
				jump.putExtra(Intent.EXTRA_TEXT, "This is a request for special help.  Contained in this request are the only details to help diagnose and understand an issue.\n\n\n\n");
				jump.putExtra(Intent.EXTRA_EMAIL, new String[] {"\""+ Custom.APP + " Support\" <havenskys@gmail.com>"} ); 
				jump.putExtra(Intent.EXTRA_SUBJECT, "Support Request: " + Custom.APP);
				jump.setType("message/rfc822"); 
				startActivity(Intent.createChooser(jump, "Email"));
			}
			break;
		case 202:
			{
				Intent goFish = new Intent(this, ConfigureLogin.class);
	        	startActivity(goFish);
	        	
				//Intent jump = new Intent(Intent.ACTION_SEND);
				//jump.putExtra(Intent.EXTRA_TEXT, "");
				//jump.putExtra(Intent.EXTRA_EMAIL, new String[] {"\""+Custom.WHO + "\" <"+Custom.EMAIL+">"});
				//jump.putExtra(Intent.EXTRA_SUBJECT, "Hello");
				//jump.setType("message/rfc822"); 
				//startActivity(Intent.createChooser(jump, "Email"));
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	
	
	private void serviceRestart(final String who) {
		
		final Context ctx = this;
		//mPreferencesEditor.putBoolean("servicerestart", true); mPreferencesEditor.commit();
		
		mLog.w(TAG, "serviceRestart() from " + who);
		Thread s = new Thread(){
			public void run(){
				SystemClock.sleep(1880);
				
			    //Intent service = new Intent();
				//service.setClass(ctx, SyncService.class);
			    //stopService(service);
			    //service.putExtra("com.havenskys.newsbite.who", TAG + " serviceRestart() from " + who);
			    //startService(service);
			}
		};
		//s.setPriority(Thread.MIN_PRIORITY);
		s.start();
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// TODO Auto-generated method stub
		mLog.w(TAG, "onOptionsMenuClosed()");
		super.onOptionsMenuClosed(menu);
	}



}










