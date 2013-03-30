package com.havenskys.galaxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.havenskys.galaxy.activity.ListPeople;

public class Custom {

	// CUSTOM
	public static String APP = "Galaxy";
	
	public static boolean PUBLISH = false;// Logs print if false
	public static boolean FREEVERSION = false;
	public static boolean DEVWORKING = false;
	public static int DEVFEED = 14; // if DEVWORKING is true this will be the only feed download
	public static int LIMITDOWNLOAD = 25;
	public static boolean QUITEFEED = true;
	public static int PARSESIZE_LIMIT = 50; //Kilobytes
	public static int DOWNLOAD_LIMIT = 64; //Kilobytes
	
	//public static String WHO = "SSCS";
	//public static String EMAIL = "info@seashepherd.org";
	
	public static int LITTLEICON = R.drawable.companygalaxy;
	public static int TOPICON = R.drawable.companygalaxy;
	
	private Context mContext;
    private ContentResolver mResolver;
	public Custom(Context ctx, String who){
		TAG = who.replaceFirst(".* for ", "").replaceFirst(" .*", "");
		
		long freememory = Runtime.getRuntime().freeMemory();
		
		//w(TAG,"Custom() ++++++++++++++++++++++++++++++++++ freememory("+(int)(freememory/1024)+" K) for " + who);
		
		
		mContext = ctx;
		mResolver = ctx.getContentResolver();

	}
	
	
	public static String MAINURI = "com.havenskys.galaxy";
	
	
	public static String TAG = "Custom";
	public static final String DATABASE_NAME = "data.db";
    //public static final String DATABASE_TABLE_NAME = "articles";
    public String SQL = "";
	public static int NOTIFY_ID = -2001;
	public static int NOTIFY_ID_ARTICLE = -2002;
	public int RESTARTMIN = 30;
	
    
	// CUSTOM
	//title, link, id, updated, summary, content
    public static final String ID           = "_id";
    //public static final String TYPE         = "type";
    //public static final String TITLE        = "title";
    //public static final String LINK      	= "link";
    //public static final String DATE         = "date";
    //public static final String SUMMARY      = "summary";
    //public static final String CONTENT      = "content";
    //public static final String CONTENTURL   = "contenturl";
    //public static final String DESCRIPTION  = "description";
    //public static final String AUTHOR       = "author";
    //public static final String GUID       = "guid";
    //public static final String SUBTITLE     = "subtitle";
    //public static final String CATEGORY     = "category";
    //public static final String MEDIA        = "media";
    //public static final String MEDIATYPE    = "mediatype";
    //public static final String MEDIADURATION = "mediaduration";
    //public static final String MEDIASIZE    = "mediasize";
    public static final String LAST_UPDATED = "lastupdated";
    public static final String CREATED      = "created";
    public static final String STATUS       = "status";
    public static final String READ         = "read";
    public static final String SEEN         = "seen";
    public static final String DEFAULT_SORT_ORDER = LAST_UPDATED + " DESC";
    
	// CUSTOM
	public String getContentSQL() {
		
		SQL = "";
		SQL += "create table "+mContext.getString(R.string.browserStore_table)+" (";
		SQL += "_id integer primary key autoincrement, ";
		SQL += mContext.getString(R.string.browserStore_columns);
		SQL += ", " + CREATED + " DATE";
		SQL += ", " + LAST_UPDATED + " DATE";
		SQL += ", " + STATUS + " INTEGER"; // < 0 deleted(value * -1), 1 active(created), ++ per update 
		SQL += ");\n";
		
		SQL += "create table contactStore (";
		SQL += "_id integer primary key autoincrement, ";
		SQL += mContext.getString(R.string.contactStore_columns);
		SQL += ", " + CREATED + " DATE";
		SQL += ", " + LAST_UPDATED + " DATE";
		SQL += ", " + STATUS + " INTEGER"; // < 0 deleted(value * -1), 1 active(created), ++ per update 
		SQL += ");\n";
		
		SQL += "create table "+mContext.getString(R.string.searchStore_table)+" (";
		SQL += "_id integer primary key autoincrement, ";
		SQL += mContext.getString(R.string.searchStore_columns);
		SQL += ", " + CREATED + " DATE";
		SQL += ", " + LAST_UPDATED + " DATE";
		SQL += ", " + STATUS + " INTEGER"; // < 0 deleted(value * -1), 1 active(created), ++ per update 
		SQL += ");\n";
        
		
		return SQL;

	}

	
	//private String memoryBox;
		

	private String getTagContent(String who, String lines, String tag) {
		String value = "";
		if( lines.contains("<"+tag+" ") ){
			long time = SystemClock.currentThreadTimeMillis();
			value = lines.replaceFirst(".*<"+tag+" ", "").replaceFirst(">", "SPLIT-"+time+">").replaceFirst(".*SPLIT-"+time+">", "").replaceFirst("</"+tag+">.*", "");
		}else{
			value = lines.replaceFirst(".*<"+tag+">", "").replaceFirst("</"+tag+">.*", "");
		}
		if( value.contains("CDATA[") ){
			value = value.replaceFirst("\\]\\].*", "").replaceFirst(".*CDATA\\[", "");
		}
		//.replaceAll("\"", "&#34;")
		value = value.replaceAll("&gt;", ">").replaceAll("&lt;", "<").replaceAll("&#39;", "'").replaceAll("&amp;", "&").replaceAll("&apos;", "'").replaceAll("&#34;", "'").replaceAll("\"", "'");
		return value.trim();
	}
	
	private String getTagValue(String who, String line, String tag, String key) {
		String value = "";
		value = line.replaceFirst(".*<"+tag+" .*"+key+"=\"", "").replaceFirst("\".*", "").trim();
		return value;
	}


	
    private NotificationManager mNM;
    public void setNotificationManager(NotificationManager notifmgr){
    	mNM = notifmgr;
    }
    
    private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	public void setSharedPreferences(SharedPreferences sharedPreferences, Editor preferencesEditor) {
		mSharedPreferences = sharedPreferences;
		mPreferencesEditor = preferencesEditor;
	}
	
	
	private String[] mLogLines = null;
	private int mLogLooper, mLogLen;
	public void i(String who, String data){
		if( PUBLISH ){ return; }
		//mLogLines = data.split("\n");
		//mLogLen = mLogLines.length;
		//for (mLogLooper = 0; mLogLooper < mLogLen; mLogLooper++){ Log.i(APP +" "+ who, mLogLines[mLogLooper] + " " + SystemClock.currentThreadTimeMillis()); }
		String[] logLines = data.split("\n");
		int logLen = logLines.length;
		for (int logLooper = 0; logLooper < logLen; logLooper++){ Log.i(APP +" "+ who, logLines[logLooper]+ " " + SystemClock.currentThreadTimeMillis()); }
	}
	
	public void w(String who, String data){
		if( PUBLISH ){ return; }
		String[] logLines = data.split("\n");
		int logLen = logLines.length;
		for (int logLooper = 0; logLooper < logLen; logLooper++){ Log.w(APP +" "+ who, logLines[logLooper]+ " " + SystemClock.currentThreadTimeMillis()); }
	}
	
	public void e(String who, String data){
		//mLogLines = data.split("\n");
		//mLogLen = mLogLines.length;
		//for (mLogLooper = 0; mLogLooper < mLogLen; mLogLooper++){ Log.e(APP +" "+ who, mLogLines[mLogLooper]+ " " + SystemClock.currentThreadTimeMillis()); }
		String[] logLines = data.split("\n");
		int logLen = logLines.length;
		for (int logLooper = 0; logLooper < logLen; logLooper++){ Log.e(APP +" "+ who, logLines[logLooper]+ " " + SystemClock.currentThreadTimeMillis()); }
	}

	
	private String fixDate(String origUpdated, String who) {
		
		String updated = origUpdated;
		// Mon, 29 Jun 2009 12:37:29 -0700
		// 0    1   2   3   4         5
		String[] dateparts = updated.split(" ");
		if( dateparts.length > 4 ){
			String[] month = new String("xxx Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec xxx").split(" ");
			int mon = 0;
			for(;mon < month.length; mon++){
				if( month[mon].equalsIgnoreCase(dateparts[2]) ){ break; } 
			}
			if( mon == 13 ){
				e(TAG,"Unable to determine month in fixDate("+updated+")");
				return updated;
			}
			int year = 0;
			int day = 0;
			try {
				year = Integer.parseInt(dateparts[3]);
				day = Integer.parseInt(dateparts[1]);
			} catch(NumberFormatException	e){
				e(TAG,"fixDate() 555 Number FormatException original("+updated+") policy(moving on)");
			}

			if( dateparts[5].contains("EST") ){ dateparts[5] = "-0400"; }
			if( dateparts[5].contains("CST") ){ dateparts[5] = "-0500"; }
			if( dateparts[5].contains("MST") ){ dateparts[5] = "-0600"; }
			if( dateparts[5].contains("PST") ){ dateparts[5] = "-0700"; }
			if( dateparts[5].contains("GMT") || dateparts[5].contains("0000") ){
				// All good in the GMT world
			}else if( dateparts[5].contains("+") || dateparts[5].contains("-") ){
				// we've got a modification here.
				String[] time = dateparts[4].split(":");
				char sign = dateparts[5].charAt(0);
				int hourMod = 0;
				int minMod = 0;
				
				//String hourS = dateparts[5].substring(1, 2);
				//String minS = dateparts[5].substring(3, 4);
				int hour = 0;
				int minute = 0;
				int second = 0;
				
				try {
					if( dateparts[5].charAt(1) == 0 ){
						hourMod = Integer.parseInt(""+dateparts[5].charAt(2));
					}else{
						hourMod = (Integer.parseInt(""+dateparts[5].charAt(1)) * 10) + Integer.parseInt(""+dateparts[5].charAt(2));
					}
					if( dateparts[5].charAt(3) == 0 ){
						minMod = 0;
					}else{
						minMod = (Integer.parseInt(""+dateparts[5].charAt(3)) * 10) + Integer.parseInt(""+dateparts[5].charAt(4));
					}
					//hourMod = Integer.parseInt(hourS);
					//minMod = Integer.parseInt(minS);
					hour = Integer.parseInt(time[0]);
					minute = Integer.parseInt(time[1]);
					second = Integer.parseInt(time[2]);
				} catch(NumberFormatException	e){
					e(TAG,"fixDate() 565 Number FormatException original("+updated+") time("+time[0]+":"+time[1]+":"+time[2]+") policy(moving on)");
				}
				
				int[] mondays = new int[] {0,31,28,31,30,31,30,31,31,30,31,30,31};
				if( sign == '-' ){
					hour += hourMod;
					minute += minMod;
					if( minute >= 60 ){
						minute -= 60;
						hour++;
					}
					if( hour >= 24 ){
						hour -= 24;
						day++;
						if( day > mondays[mon] ){
							day -= mondays[mon];
							mon++;
							if( mon > 12 ){
								year++;
								mon -= 12;
							}
						}
					}
				}else if(sign == '+'){
					hour -= hourMod;
					minute -= minMod;
					if( minute < 0 ){
						minute += 60;
						hour--;
					}
					if( hour < 0 ){
						hour += 24;
						day--;
						if( day < 1 ){
							mon--;
							if( mon < 1 ){
								mon += 12;
								year--;
							}
							day += mondays[mon];
						}
					}
				}else{
					e(TAG,"fixDate() 585 Sign(+|-) unfamiliar original("+updated+") policy(moving on)");
				}
				
				dateparts[4] = (hour > 9) ? ""+hour : "0"+hour;
				dateparts[4] += (minute > 9) ? ":"+minute : ":0"+minute;
				dateparts[4] += (second > 9) ? ":"+second : ":0"+second;
				w(TAG,"fixDate() 560 date("+updated+") time("+time[0]+":"+time[1]+":"+time[2]+") sign("+sign+") hourMod("+hourMod+") minMod("+minMod+") new("+dateparts[4]+")");
				
			}else{
				
			}
			
			updated = year + "";
			updated += (mon > 9) ? "-"+mon : "-0" + mon;
			updated += (day > 9) ? "-"+day : "-0" + day;
			updated += "T" + dateparts[4];
			
			//if( mon < 10 ){
				//updated = year + "-0" + mon + "-" + day + "T" + dateparts[4];
			//}else{
				//updated = year + "-" + mon + "-" + day + "T" + dateparts[4];
			//}
			i(TAG,"fixDate() 804 Updated date("+origUpdated+") to SQLite Format("+updated+") for " + who);
		}
		
		return updated;
	}

	private long mLastVib = 0;
	public void setEntryNotification(String who, int notificationID, long rowid, int icon, String title, String details, String topscroll){
		i(TAG,"setEntryNotification() for " + who);
		/*/
		
		
		Notification notif = new Notification(icon, topscroll, System.currentTimeMillis()); // This text scrolls across the top.
		Intent intentJump2 = new Intent(mContext, com.havenskys.newsbite.LoadArticle.class);
		intentJump2.putExtra("id", rowid);
		intentJump2.putExtra("tab", 1);
        //PendingIntent pi2 = PendingIntent.getActivity(this, 0, intentJump2, Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pi2 = PendingIntent.getActivity(mContext, 0, intentJump2, Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_MULTIPLE_TASK );
        
        //if( syncvib != 3 ){ // NOT OFF
        	//notif.defaults = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE;
        //}else{
        	//notif.defaults = Notification.DEFAULT_LIGHTS;
        //}
        
		notif.setLatestEventInfo(mContext, title, details, pi2); // This Text appears after the slide is open
		
		//Calendar c = new Calendar();
		Date d = new Date(); 
		i(TAG,"setEntryNotification() 432 getHours("+d.getHours()+") for " + who);
		
		int syncvib = mSharedPreferences.contains("syncvib") ? mSharedPreferences.getInt("syncvib",1) : 1;
		
		if( mLastVib > 0 && (System.currentTimeMillis() - mLastVib) > (25 * 60 * 1000) ){
			if( d.getHours() < 20 && d.getHours() > 8 ){
				switch (syncvib) {
				case 1: // ++_+
					notif.vibrate = new long[] { 100, 200, 100, 200, 500, 200 };
					break;
				case 3: // None _
					break;
				case 2: // ++
					notif.vibrate = new long[] { 100, 200, 100, 200 };
					break;
				case 4: // +_++
					notif.vibrate = new long[] { 100, 200, 500, 200, 100, 200 };
					break;
				}
			}
		}
        mNM.notify(notificationID, notif);
        //*/
	}
	
	public void setServiceNotification(String who, int notificationID, int icon, String title, String details, String topscroll){
		
		/*/
		//NotificationManager notifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notif = new Notification(icon, topscroll, System.currentTimeMillis());
		Intent intentJump = new Intent(mContext, com.havenskys.newsbite.Stop.class);
		intentJump.putExtra("stoprequest", true);
        PendingIntent pi = PendingIntent.getActivity(mContext, 0, intentJump, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NO_HISTORY);
        notif.setLatestEventInfo(mContext, title,details, pi);
        //notif.defaults = Notification.DEFAULT_LIGHTS;
        //notif.vibrate = new long[] { 100, 100, 100, 200, 100, 300 };
        mNM.notify(notificationID, notif);
        //*/
	}
	
	
	// This will block until load is low or time limit exceeded
    // loadLimit(TAG + " getlatest() 107", 1 , 5 * 1000, 3 * 60 * 1000);
	public void loadLimit(String who, int loadMin, int waitms, int waittimemax) {
		i(TAG,"loadLimit() loadMin("+loadMin+") waitms("+waitms+") waittimemax("+waittimemax+") for " + who);
		
		double load = 0;
		double lastload = 0;
        int waitloopmax = waittimemax / waitms;
        int sleepcounter = 0;
        //Thread sleeperThread = new Thread(){
        	//public void run(){ SystemClock.sleep(1000); }
        //};
        
        int spin = 0;
        for(int lc = 1; lc <= waitloopmax; lc++){
        	load = getload(TAG + " loadLimit() 822 for " + who);
        	
        	if( load > (loadMin + 0.99) ){
        		mPreferencesEditor.putLong("cpublock", System.currentTimeMillis()).commit();
        		w(TAG,"loadLimit() load("+load+") > loadMin("+loadMin+".99) for " + who);
        		//if( lc == 2 ){ // second loop, notify user app has paused.
        			//setServiceNotification(TAG + " loadLimit() 340", android.R.drawable.ic_media_pause, Custom.APP + " (Press to Stop)", "Waiting for device CPU load to decrease.", Custom.APP + " synchronizing service is paused, waiting for CPU load to decrease.");
        		//}
        		for(sleepcounter = 0; sleepcounter < (waitms/1000); sleepcounter++){ SystemClock.sleep(1000); }
        	}else{
        		mPreferencesEditor.putLong("cpublock", 0).commit();
        		i(TAG,"loadLimit() load("+load+") <= loadMin("+loadMin+".99) for " + who);
        		break;
        		//lc = waitloopmax;// setting to end so the following code is run (protects against raising load)
        		//spin++;
        		//if( spin > 5 ){
        			//e(TAG,"Spin Doctor Limit Reached moving on.");
        			//break; 
        			//}
        	}
        	if( lc == waitloopmax ){
        		if( load > lastload ){
        			w(TAG,"loadLimit() load("+load+") > lastload("+lastload+") for " + who);
        			// Load is going up, let's hold off till this isn't true.
        			// First available chance though, I'm on it.
        			lc--;
        			SystemClock.sleep( (long)(waitms/2) );
        		}else{
        			w(TAG,"Waited for maximum limit("+waittimemax+"ms), running anyway. for " + who);
        		}
        	}
        	lastload = load;
        }
        mPreferencesEditor.putLong("cpublock", 0).commit();
        i(TAG,"loadLimit() DONE");
        //setServiceNotification(TAG + " loadLimit() 350", android.R.drawable.stat_notify_sync, Custom.APP + " (Press to Stop)", "Synchronizing updates.", Custom.APP + " synchronizing updates.");
	}
	
	
	private java.lang.Process mLoadProcess;
	private InputStream mLoadStream;
	private byte[] mLoadBytes;
	private String[] mLoadParts;
	private long mLoadStart;
	private int mLoadReadSize;
	private double mLoadDouble;
	public double getload(String who){
		i(TAG,"getload() for " + who);
		
		if( mSharedPreferences == null || mPreferencesEditor == null ){
			e(TAG,"getload() started without SharedPreferences being available. Shouldn't happen.");
			return 0;
		}
		
		i(TAG,"getload() getting Editor");
		mPreferencesEditor = mSharedPreferences.edit();
		
		Thread loadT = new Thread(){
			public void run(){
				
				mPreferencesEditor = mSharedPreferences.edit();
				
				long mLoadStart = System.currentTimeMillis();
				double mLoadDouble = 1.1; // if something goes wrong, best to error on the side of shy
				
				try {
					//i(TAG,"getload() cat /proc/loadavg");
					
					java.lang.Process mLoadProcess = Runtime.getRuntime().exec("cat /proc/loadavg");
					//i(TAG,"getload() waitFor[it]");
					mLoadProcess.waitFor();
					//i(TAG,"getload() get Input Stream");
					InputStream mLoadStream = mLoadProcess.getInputStream();
					//i(TAG,"getload() create byte array[50]");
					byte[] mLoadBytes = new byte[50];
					//i(TAG,"getload() read bytes");
					int mLoadReadSize = mLoadStream.read(mLoadBytes, 0, 49);
					//i(TAG,"getload() split load parts(" + new String(mLoadBytes).replaceFirst("\n.*", "").trim() + ")");
					String[] mLoadParts = new String(mLoadBytes).trim().replaceFirst("\n.*", "").replaceAll("\\s+", " ").split(" ");
					//i(TAG,"getload() convert load to Double from string part [0](" + mLoadParts[0] + ")");
					
					if( mLoadParts[0].contains(".") && mLoadParts[0].length() > 0 ){
						//i(TAG,"getload() as we expected");
						mLoadDouble = new Double(mLoadParts[0].toString());
					}else if(mLoadParts[0].length() > 0){
						e(TAG,"getload() no decimal ");
						int newint = Integer.parseInt(mLoadParts[0]);
						mLoadDouble = (double) newint;
					}else{
						e(TAG,"getload() empty");
						mLoadDouble = 4;
					}
					//w(TAG,"Load size("+mLoadReadSize+") load("+mLoadDouble+") ms("+(System.currentTimeMillis() - mLoadStart)+") loadavg("+new String(mLoadBytes).trim()+")");
					//mPreferencesEditor.putFloat("loadFound", (float) mLoadDouble).commit();
					//i(TAG,"getload() setting preference");
					mPreferencesEditor.putInt("loadFound", (int) (mLoadDouble * 1000) ).commit();
					
					/*
					w(TAG,"Getting MEMINFO");
					Process top = Runtime.getRuntime().exec("cat /proc/meminfo");
					top.waitFor();
					InputStream topstream = top.getInputStream();
					mLoadBytes = new byte[1024];
					mLoadReadSize = topstream.read(mLoadBytes, 0, 1023);
					w(TAG,"MEMINFO " + new String(mLoadBytes).trim() );
					//*/
					
					/*
					w(TAG,"Getting PROC");
					Process top = Runtime.getRuntime().exec("ls /proc");
					top.waitFor();
					InputStream topstream = top.getInputStream();
					mLoadBytes = new byte[1024];
					mLoadReadSize = topstream.read(mLoadBytes, 0, 1023);
					String[] proclist = new String(mLoadBytes).trim().split("\n");
					for(int i = 0; i < proclist.length; i++){
						w(TAG,"PROC: " +  proclist[i].trim() );
						if( proclist[i].trim().contains("self") ){
							for(i++; i < proclist.length; i++){
								Process file = Runtime.getRuntime().exec("ls /proc/"+proclist[i].trim());
								file.waitFor();
								InputStream filestream = file.getInputStream();
								mLoadBytes = new byte[1024];
								mLoadReadSize = filestream.read(mLoadBytes, 0, 1023);
								String[] filelist = new String(mLoadBytes).trim().split("\n");
								for(int c = 0; c < filelist.length; i++){
									w(TAG,"FILE /proc/"+proclist[i].trim() + ": " +  filelist[c].trim() ); 
								}
							}
							
							break;
						}
					}
					//*/
					
					mLoadBytes = null;
		
				} catch (NumberFormatException e) {
					e(TAG,"Load NumberFormatException");
					e.printStackTrace();	
				} catch (InterruptedException e) {
					e(TAG,"Load InterruptedException");
					e.printStackTrace();
				} catch (IOException e) {
					e(TAG,"Load IOException");
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
			}
		};
		
		//loadT.setDaemon(true);
		//*/
		loadT.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){

			public void uncaughtException(Thread thread, Throwable ex) {
				e(TAG,"getload() 966 uncaughtException() [caught] "  + ex.getMessage());
			}
			
		});
		
		//*
		loadT.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){

			public void uncaughtException(Thread thread, Throwable ex) {
				e(TAG,"getload() 973 defaultUncaughtException() [caught] " + ex.getMessage());				
			}
		});//*/
		
		loadT.start();
		double loadFound = 0;
		
		i(TAG,"Waiting for reply");
		
		int maxwaitsec = 5;
		long waitstart = System.currentTimeMillis();
		int looplimit = 0;
		for(;;){
			if( (System.currentTimeMillis() - waitstart) > maxwaitsec * 1000 ){
				e(TAG,"getload() 1029 reached waitlimit("+maxwaitsec+") for " + who);
				break;
			}
			if( loadT.getState().name() == "BLOCKED" ){
				e(TAG,"getload() BLOCKED");
				loadT.interrupt();
				break;
			}else{
				i(TAG,"getload() state("+loadT.getState().name()+")");
			}
			try {
				loadT.sleep(250);
			} catch (InterruptedException e) {
				e(TAG,"getload() exception " + e.getLocalizedMessage());
				e.printStackTrace();
			}
			w(TAG,"getload() 1043");
			if( loadT.isAlive() ){
				i(TAG,"getload() 1006 thread is alive");
			}else{
				i(TAG,"getload() 1008 thread is nolonger active");
				loadFound = (double) mSharedPreferences.getInt("loadFound", 1000 * 1000);
				//loadFound = mSharedPreferences.contains("loadFound") ? (double) mSharedPreferences.getInt("loadFound", 1000 * 1000) : 1000 * 1000;
				loadFound = (loadFound/1000);
				if( loadFound != 1000 ){ break; }
			}
			
			SystemClock.sleep(250);
		}
		
		
		i(TAG,"getload() 574 found("+loadFound+") DONE");
		
		return loadFound;
	}
	
	
	public long getId(String path, String where){
		Object[][] data = getAndroidData(path,"_id",where,null);
		if( data == null ){
			return 0;
		}else{
			return new Long(data[0][0].toString());
		}
	}
	
	public Object[][] getAndroidData(String path, String columns, String where, String orderby){
		Object[][] reply = null;

		//mLog.w(TAG,"getAndroidData("+path+") columns("+columns+") where("+where+")");
		
		if( orderby == null ){
			orderby = "created desc";
		}
		
        Cursor dataCursor = SqliteWrapper.query(mContext, mResolver, Uri.parse(path) 
        		,columns.split(",")
        		,where
        		,null
        		,orderby //"date desc"
        		);
        
        
        if( dataCursor != null ){
        	if( dataCursor.moveToFirst() ){
        		int len = dataCursor.getCount();
        		int clen = dataCursor.getColumnCount();
        		reply = new Object[len][clen];
        		for(int r = 0; r < len ;r++){
        			dataCursor.moveToPosition(r);
	        		for(int c = 0; c < clen ;c++){
	        			reply[r][c] = dataCursor.getString(c);
	        		}
        		}
        	}else{
        		//mLog.w(TAG,"getAndroidData empty");
        	}

            dataCursor.close();
        }else{
        	//mLog.w(TAG,"getAndroidData null");
        }
		return reply;
	}

	
	public void serviceState(String who, String what){
		
		
		if( mSharedPreferences == null ){
			e(TAG,"serviceState() started without shared preferences for " + who);
			return;
		}

		//android.os.Process.
		Runtime.getRuntime().gc();
		String Oservicehistory = mSharedPreferences.getString("servicehistory", "");//DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | 
		String servicehistory = Process.myPid() + ": " + DateUtils.formatDateTime(mContext, System.currentTimeMillis(),  DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME );
		servicehistory += " " + what;
		if( Oservicehistory.length() > 1024 * 5 ){
			//Oservicehistory.regionMatches(false, 0, "\n", 1023, (Oservicehistory.length()-1023-1));
			//int next = Oservicehistory.indexOf("\n", (1024 * 5) - 1);
			//if( next > 1 ){ Oservicehistory = Oservicehistory.substring(0,next-1); }
			int start = Oservicehistory.length() - (1024 * 5);
			if(start < 0 ){start = 0;}
			int next = Oservicehistory.indexOf("\n", start);
			if( next > 1 ){
				Oservicehistory = Oservicehistory.substring(next,Oservicehistory.length()-1);
			}
		}
		e(TAG,"(positive) ServiceState: " + servicehistory);
		//servicehistory += "\n" + Oservicehistory;
		mPreferencesEditor.putString("servicehistory", Oservicehistory + "\n" + servicehistory ).commit();
		
	}

	private long mLastRestart = 0;
	private boolean mConsoleUnknownReset = false;
	private boolean mConsoleServiceReset = false;
	private boolean mConsoleTouchReset = true;
	private long mConsoleTouchLast = 0;
	public void refreshConsoleTouch(ImageView consoleTouch, Handler handler) {
		
		int syncInterval = mSharedPreferences.getInt("sync", 29);
		long syncstart = mSharedPreferences.getLong("syncstart", -1);
		long syncend = mSharedPreferences.getLong("syncend", -1);
		long lastactive = mSharedPreferences.getLong("lastfeedactive", -1);
		long lowmemory = mSharedPreferences.getLong("lowmemory", -1);
		long cpublock = mSharedPreferences.getLong("cpublock", -1);
		//mPreferencesEditor.putLong("lowmemory", System.currentTimeMillis()).commit();
		
		//
		
		i(TAG, "refreshConsoleTouch() mConsoleUnknownReset("+mConsoleUnknownReset+") mConsoleTouchReset("+mConsoleTouchReset+") mConsoleServiceReset("+mConsoleServiceReset+") syncInterval("+syncInterval+") syncstart("+syncstart+") syncend("+syncend+") lastactive("+lastactive+") lowmemory("+lowmemory+") cpublock("+cpublock+") +++++++++++++++++++++++++");
		
		
		boolean startservice = false;
		
		if( mConsoleServiceReset ){
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_active));
			handler.postDelayed((Runnable) mContext, 1880);
			mConsoleServiceReset = false;
			mConsoleTouchReset = false;
			return;
		}
		
		if( mConsoleUnknownReset ){
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_cpublock));
			handler.postDelayed((Runnable) mContext, 1880);
			mConsoleUnknownReset = false;
			mConsoleTouchReset = false;
			return;
		}
		
		if( mConsoleTouchReset ){
			mConsoleTouchReset = false;
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter));
			handler.postDelayed((Runnable) mContext, 1880);
			return;
		}
		mConsoleTouchReset = true;
		if( mConsoleTouchLast > System.currentTimeMillis() - 10 * 1000){
			e(TAG,"refreshConsoleTouch() last ran within 10 seconds, sounds like a duplicate.  leaving.");
			return;
		}
		mConsoleTouchLast = System.currentTimeMillis();
		if( Process.getElapsedCpuTime() > 300 * 1000 ){
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter));
			return;
		}
		
		//mConsoleTouch.post(new Runnable(){
			//public void run(){
		
			//}
		//});
		//mConsoleTouch.getHandler().sendEmptyMessage(1);
			
		//SystemClock.sleep(1000);

		
		/*
		if( syncstart > 0 && syncstart < (System.currentTimeMillis() - 60 * 1000) && lowmemory > 0 && lowmemory < (System.currentTimeMillis() - 2 * 60 * 1000 ) && cpublock > 0 && cpublock < (System.currentTimeMillis() - 2 * 60 * 1000 ) ){
			startservice = true;
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_unhealthy));
			
		} else//*/ 
		
		if( cpublock > 0 ){
			//cpublock > (System.currentTimeMillis() - 2 * 60 * 1000);
			int since = (int) ((System.currentTimeMillis() - cpublock)/1000);
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_cpublock));
			
			if( since >= 120 ){
				startservice = true;
				e(TAG, "refreshConsoleTouch() cpublock since("+since+" seconds) requesting restart");
			}else{
				e(TAG, "refreshConsoleTouch() cpublock since("+since+" seconds) recent");
			}
			
		} else if( lowmemory > 0 ){
			int since = (int) ((System.currentTimeMillis() - lowmemory)/1000);
			
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_memoryblock));
			if( since >= 120 ){
				startservice = true;
				e(TAG, "refreshConsoleTouch() lowmemory since("+since+" seconds) requesting restart");
			}else{
				e(TAG, "refreshConsoleTouch() lowmemory since("+since+" seconds) recent");
			}
		} else if( syncstart > (System.currentTimeMillis() - 60 * 1000) || lastactive > (System.currentTimeMillis() - 60 * 1000) ){
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_active));
			
		} else if( syncend > (System.currentTimeMillis() - syncInterval * 60 * 1000) ){
			boolean allverified = false;
			
			if( allverified ){
				consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_super));
			}else{
				startservice = true;
				consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_healthy));
			}
			
		} else if( syncstart == 0 ){
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_active));
			startservice = true;
			
		} else if( syncend > 0 && syncend < (System.currentTimeMillis() - (syncInterval+2) * 60 * 1000) ){ // wait a couple minutes before determining it's unhealthy
			//mConsoleUnknownReset = true;
			//mConsoleServiceReset = true;
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_unhealthy));
			startservice = true;
			
		/*/
		} else {
			mConsoleUnknownReset = true;
			w(TAG, "Uncaught condition setting to active syncstart("+syncstart+") syncInterval("+syncInterval+") syncend("+syncend+") lowmemory("+lowmemory+") cpublock("+cpublock+")");
			consoleTouch.setImageDrawable(mContext.getResources().getDrawable(R.drawable.listfooter_active));
			startservice = true;
			//*/
		}else{
			e(TAG, "Uncaught condition setting to active syncstart("+syncstart+") syncInterval("+syncInterval+") syncend("+syncend+") lowmemory("+lowmemory+") cpublock("+cpublock+")");
		}
		if( startservice ){
			w(TAG, "refreshConsoleTouch() Restarting service");
			if( mLastRestart < (System.currentTimeMillis() - 60 * 1000) ){
				mConsoleServiceReset = true;
				//i(TAG,"run() get AlarmManager");
				//AlarmManager alm = (AlarmManager) mContext.getSystemService(mContext.ALARM_SERVICE);
				//Intent resetservice = new Intent();
		        //com.havenskys.newsbite.IntentReceiver.SERVICE_RESET
				//resetservice.setAction("com.havenskys.newsbite.IntentReceiver.SERVICE_RESET");
				//PendingIntent service3 = PendingIntent.getBroadcast(mContext, 0, resetservice, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_CANCEL_CURRENT);
				//alm.set(AlarmManager.RTC_WAKEUP,( System.currentTimeMillis() + (2 * 1000) ), service3);
				//alm.setRepeating(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + 3 * 1000), AlarmManager.INTERVAL_FIFTEEN_MINUTES, service3);
				//alm.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + 3 * 1000), service3);
				
				//Intent service = new Intent();
				//service.setClass(mContext, SyncService.class);
			    //mContext.stopService(service);
			    //service.putExtra("com.havenskys.newsbite.who", TAG + " run()");
			    //mContext.startService(service);
				
				//Intent service = new Intent();
				//service.setClass(mContext, com.havenskys.newsbite.SyncService.class);
				//service.putExtra("com.havenskys.newsbite.who", TAG + " refreshConsoleTouch() Repeating Service Alarm minutes("+syncInterval+") at " + DateUtils.formatDateTime(mContext, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME ));
				//PendingIntent serviceP = PendingIntent.getService(mContext, 1000, service, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_CANCEL_CURRENT);
				//alm.setRepeating(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + 1000), AlarmManager.INTERVAL_FIFTEEN_MINUTES, serviceP);
			    
			    mLastRestart = System.currentTimeMillis();
			//} else if( startservice ){
				//mHandler.postDelayed(this, 1880);
				//return;
			}else{
				w(TAG,"refreshConsoleTouch() Already Restarted "+( (System.currentTimeMillis() - mLastRestart)/1000 )+" seconds ago.");
			}
		}
		
		//mHandler.postDelayed(this, 1000 * 10);
		//mHandler.sendEmptyMessageDelayed(3, 1000 * 5);
		handler.postDelayed((Runnable) mContext, 1000 * 10);
	}
	

	
	public String getPage(String gourl, String who){
		
		w(TAG,"getPage() get ConnectivityManager");
    	ConnectivityManager cnnm = (ConnectivityManager) mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);
    	w(TAG,"getPage() get NetworkInfo");
    	NetworkInfo ninfo = cnnm.getActiveNetworkInfo();
    	w(TAG,"getPage() got NetworkInfo state("+ninfo.getState().ordinal()+") name("+ninfo.getState().name()+")");
    	//android.os.Process.getElapsedCpuTime()
    	
		String httpPage = "";
		//String gourl = baseurl;
		Socket socket = null;
		SSLSocket sslsocket = null;
		BufferedReader br = null;
		BufferedWriter bw = null;
		int loopcnt = 0;
		try {
			while(gourl.length() > 0 ){
				
				//mPreferencesEditor.putLong("lastfeedactive", System.currentTimeMillis()).commit();
				loopcnt ++;
				if( loopcnt > 8 ){
					e(TAG,"getPage() Looped 8 times, really?! this many forwards?");
					break;
				}
				boolean secure = gourl.contains("https:") ? true : false;
				String hostname = gourl.replaceFirst(".*://", "").replaceFirst("/.*", "");
				int port = secure ? 443 : 80;
				if( hostname.contains(":") ){
					String[] p = hostname.split(":");
					hostname = p[0];
					port = Integer.parseInt(p[1]);
				}
				String docpath = gourl.replaceFirst(".*://", "").replaceFirst(".*?/", "/");
				w(TAG,"getPage() hostname("+hostname+") path("+docpath+") gourl("+gourl+")");
				gourl = "";
				
				if( !secure ){
					sslsocket = null;
					w(TAG,"getPage() Connecting to hostname("+hostname+") port("+port+")");
					socket = new Socket(hostname,port);
					
					//socket = new SecureSocket();
					//SecureSocket s = null;
					
					if( socket.isConnected() ){
						i(TAG,"getPage() Connecting to hostname("+hostname+") CONNECTED");
					}else{
						int loopcnt2 = 0;
						while( !socket.isConnected() ){
							e(TAG,"getPage() Not connected to hostname("+hostname+")");
							loopcnt2++;
							if( loopcnt2 > 10 ){
								e(TAG,"getPage() Not connected to hostname("+hostname+") TIMEOUT REACHED");
								break;
							}
							SystemClock.sleep(300);
						}
					}
					
					w(TAG,"getPage() Creating Writable to hostname("+hostname+") port("+port+")");
					bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					w(TAG,"getPage() Creating Readable to hostname("+hostname+") port("+port+")");
					br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				}else{
					socket = null;
					w(TAG,"getPage() Connecting Securely to hostname("+hostname+") port("+port+")");
					
					SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					sslsocket = (SSLSocket) factory.createSocket(hostname,443);
					SSLSession session = sslsocket.getSession();
					X509Certificate cert;
					try { cert = (X509Certificate) session.getPeerCertificates()[0]; }
					catch(SSLPeerUnverifiedException e){
						e(TAG,"getPage() Connecting to hostname("+hostname+") port(443) failed CERTIFICATE UNVERIFIED");
						break;
					}
					
					if( sslsocket.isConnected() ){
						i(TAG,"getPage() Connecting to hostname("+hostname+") CONNECTED");
					}else{
						int loopcnt2 = 0;
						while( !sslsocket.isConnected() ){
							e(TAG,"getPage() Not connected to hostname("+hostname+")");
							loopcnt2++;
							if( loopcnt2 > 20 ){
								e(TAG,"getPage() Not connected to hostname("+hostname+") TIMEOUT REACHED");
								break;
							}
							SystemClock.sleep(300);
						}
					}
											
					w(TAG,"getPage() Creating Writable to hostname("+hostname+") port("+port+")");
					bw = new BufferedWriter(new OutputStreamWriter(sslsocket.getOutputStream()));
					w(TAG,"getPage() Creating Readable to hostname("+hostname+") port("+port+")");
					br = new BufferedReader(new InputStreamReader(sslsocket.getInputStream()));
				}
				
				
				mPreferencesEditor.putLong("lastfeedactive", System.currentTimeMillis()).commit();
				w(TAG,"getPage() Requesting document hostname("+hostname+") port("+port+")");
				bw.write("GET " + docpath + " HTTP/1.0\r\n");
				bw.write("Host: " + hostname + "\r\n");
				bw.write("User-Agent: Android\r\n");
				bw.write("Range: bytes=0-"+(1024 * DOWNLOAD_LIMIT)+"\r\n");
				//bw.write("TE: deflate\r\n");
				bw.write("\r\n");
				bw.flush();
				//http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5
				String status = "";
				String line = "";
				try {
					if( !secure ){
						if( br.ready() ){
							w(TAG,"getPage() Ready to be read");
						}else{
							int loopcnt2 = 0;
							while( !br.ready() ){
								e(TAG,"getPage() NOT Ready to be read");
								loopcnt2++;
								if( loopcnt2 > 20 ){
									e(TAG,"getPage() NOT Ready to be read TIMEOUT REACHED WAITING");
									line = br.readLine();
									e(TAG,"getPage() NOT Ready to be read TIMEOUT REACHED WAITING line("+line+")");
									break;
								}
								SystemClock.sleep(300);
							}
						}
					}else{
						// br.ready() doesn't work from the sslsocket source
					}
					int linecnt = 0;
					for(line = br.readLine(); line != null; line = br.readLine()){
						if( line.length() == 0 ){
							w(TAG,"getPage() End of header Reached");
							break;
						}
						linecnt++;
						i(TAG,"getPage() received("+line+")");
						if( line.regionMatches(true, 0, "Location:", 0, 9) ){
							gourl = line.replaceFirst(".*?:", "").trim();
							w(TAG,"getPage() FOUND FORWARD URL("+gourl+") ");
						}
					}
					if( gourl.length() > 0 ){ continue; }
					if( line == null ){
						w(TAG,"getPage() End of read");
					}
					if( linecnt > 0 ){
						mPreferencesEditor.putLong("lastfeedactive", System.currentTimeMillis()).commit();
						mPreferencesEditor.putLong("lowmemory", 0).commit();
					}
					if( line != null ){
						int zerocnt = 0;
						for(line = br.readLine(); line != null; line = br.readLine()){
							if( line.length() == 0 ){
								zerocnt++;
								if( zerocnt > 50 ){
									e(TAG,"getPage() host("+hostname+") 50 empty lines received, moving on.");
									break;
								}
								continue;
							}
							zerocnt = 0;
							linecnt++;
							//i(TAG,"getPage() host("+hostname+") line("+line+")");
							httpPage += line;
							if( httpPage.length() > 1024 * DOWNLOAD_LIMIT ){
								w(TAG,"getPage() downloaded "+DOWNLOAD_LIMIT+"K from the site, moving on.");
								break;
							}
						}
					}
					w(TAG,"getPage() Downloaded("+httpPage.length()+" bytes)");
					
					
					/*/
					
					if( br.ready() ){
						
					}
					while(br.ready()){
						line = br.readLine();
						if( line == null ){
							w(TAG,"getPage() End of read Reached");
							break;
						} else if( line.length() == 0 ){
							w(TAG,"getPage() End of header Reached");
							break;
						}
						i(TAG,"getPage() feed("+longname+") received("+line+")");
						if( line.regionMatches(true, 0, "Location:", 0, 9) ){
							gourl = line.replaceFirst(".*?:", "").trim();
							w(TAG,"getPage() feed("+longname+") FOUND FORWARD URL("+gourl+") ");
						}
					}
					
					
					
					mPreferencesEditor.putLong("lastfeedactive", System.currentTimeMillis()).commit();
					while(br.ready()){
						line = br.readLine();
						if( line == null ){
							w(TAG,"getPage() End of read Reached");
							break;
						} else if( line.length() == 0 ){
							w(TAG,"getPage() End of header Reached");
							break;
						}
					}
					//*/
				}catch (IOException e1) {
					String msg = null;
					msg = e1.getLocalizedMessage() != null ? e1.getLocalizedMessage() : e1.getMessage();
					if( msg == null ){
						msg = e1.getCause().getLocalizedMessage();
						if( msg == null ){ msg = ""; }
					}
					e(TAG,"getPage() IOException while reading from web server " + msg);
					e1.printStackTrace();
				}
				
				if( !secure ){
					socket.close();
				}else{
					sslsocket.close();
				}
			}
		} catch (UnknownHostException e1) {
			e(TAG,"getPage() unknownHostException");
			e1.printStackTrace();
		} catch (IOException e1) {
			e(TAG,"getPage() IOException");
			e1.printStackTrace();
		}
		
		return httpPage;
	}


	public void loadlist(ListPeople view) {
		i(TAG,"loadlist() ++++++++++++++++++++++++++++++++++");
		
		Cursor lCursor = null;
		
		// CUSTOM
		//String[] columns = new String[] {"_id", "title", "datetime(date,'localtime') as date", "summary"};
		
		
		String[] columns = new String[] {"_id","name","alias","title","phone","office","company"};
		String[] from = new String[]{ "name", "title", "phone", "office", "company" };
		int[] to = new int[]{ R.id.contactrow_name, R.id.contactrow_title, R.id.contactrow_phone, R.id.contactrow_office, R.id.contactrow_company };
        
		lCursor = SqliteWrapper.query(view, view.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), 
        		columns,
        		"status > 0", // Future configurable time to expire seen and unread
        		null, 
        		"lastupdated desc limit 250");// + startrow + "," + numrows
		
		view.startManagingCursor(lCursor);
        SimpleCursorAdapter entries = new SimpleCursorAdapter(view, R.layout.contactrow, lCursor, from, to);
        //RelativeLayout tv = (RelativeLayout) view.findViewById(R.layout.listrow);
        //view.getListView().addHeaderView(tv);
        //RelativeLayout ll = new RelativeLayout(view); 
        //ll.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, 100));
        
        
        LinearLayout ll = new LinearLayout(view); 
        ll.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.FILL_PARENT, 65));
        //ll.setTag( new String("header") );
        //ll.setBackgroundColor(Color.RED); 
        //view.getListView().addHeaderView(ll);
        //view.getListView().addHeaderView(ll, null, false);
        ll.setTag( new String("footer") );
        //ll.setBackgroundColor(Color.BLACK);
        //RelativeLayout ll = (RelativeLayout) view.findViewById(R.layout.listfooter);
        view.getListView().addFooterView(ll, null, false);
        
        view.setListAdapter(entries);
        view.getListView().setTextFilterEnabled(true);
	}
	
}


