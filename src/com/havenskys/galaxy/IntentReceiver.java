package com.havenskys.galaxy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.havenskys.galaxy.activity.NightWorker;

public class IntentReceiver extends BroadcastReceiver {

	private final static String TAG = "IntentReceiver";
	
	private Custom mLog;

	private static Object mStartingServiceSync = new Object();
	private static WakeLock mWakeService;
	private static Context mContext;

	public void onReceive(Context context, Intent intent) {
	//Log.i(TAG,"onReceive(Action Received:"+intent.getAction()+") ++++++++++++++++++++++++++++++++++++++++++++++++");
		onReceiveWithPrivilege(context, intent, false);
		return;
	}
		
	protected void onReceiveWithPrivilege(Context context, Intent intent, boolean privileged) {
		mLog = new Custom(context, TAG + " onReceiveWithPrivilege() 27");
		String action = intent.getAction();
		mLog.i(TAG,"onReceiveWithPrivilege(Action Received:"+action+") ++++++++++++++++++++++++++++++++++++++++++++++++");
		
		mContext = context;
		
		mLog.i(TAG,"onReceiveWithPrivilege() get access to Shared Preferences");
		SharedPreferences sharedPreferences = context.getSharedPreferences("Preferences", context.MODE_WORLD_WRITEABLE);

		mLog.i(TAG,"onReceiveWithPrivilege() get Preferences Editor");
		Editor preferencesEditor = sharedPreferences.edit();
		
		long lastfeedactive = sharedPreferences.contains("lastfeedactive") ? sharedPreferences.getLong("lastfeedactive",-1) : -1;
		long lastrequest = sharedPreferences.contains("lastrequest") ? sharedPreferences.getLong("lastrequest",-1) : -1;
		long laststart = sharedPreferences.contains("laststart") ? sharedPreferences.getLong("laststart",-1) : -1;
	
		
	
		if(lastfeedactive > System.currentTimeMillis() - 10000 || laststart > System.currentTimeMillis() - 60000 ){
			
			if( lastrequest < System.currentTimeMillis() - 60000 ){
				AlarmManager mAlM = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
				Intent resetservice = new Intent();
		        //com.havenskys.newsbite.SERVICE_RESET
				resetservice.setAction("com.havenskys.newsbite.SERVICE_RECOVER3");
				//PendingIntent service3 = PendingIntent.getActivity(mContext, 0, resetservice, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_CANCEL_CURRENT);
				PendingIntent service4 = PendingIntent.getBroadcast(mContext, 80, resetservice, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_CANCEL_CURRENT);
				//mAlM.set(AlarmManager.RTC_WAKEUP,( System.currentTimeMillis() + (60 * 1000) ), service3);
				mAlM.set(AlarmManager.RTC_WAKEUP, ( System.currentTimeMillis() + (30 * 1000) ), service4);
				preferencesEditor.putLong("lastrequest", System.currentTimeMillis()).commit();
				mLog.e(TAG, "onReceiveWithPrivilege restart already requested "+(int) ((System.currentTimeMillis()-laststart)/1000)+" seconds ago, scheduled retry.");
			}else{
				mLog.e(TAG, "onReceiveWithPrivilege restart already requested "+(int) ((System.currentTimeMillis()-laststart)/1000)+" seconds ago.");
			}
			return;
		}
		preferencesEditor.putLong("laststart", System.currentTimeMillis()).commit();
		
		
		//int ago = (int) ((System.currentTimeMillis() - lastfeedactive)/1000);
		//if( lastfeedactive > 0 && ago < 60){
			//mLog.w(TAG, "onReceiveWithPrivilege() started just a few seconds ago("+ago+" seconds) [cancel restart]");
			//return;
		//}

		/*
		boolean success = false;
		for( int t = 0; t < 10; t++ ){
			
			mLog.i(TAG, "onReceiveWithPrivilege() 37 getting Network Details. [positive error, hidden log?]");
			
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
			NetworkInfo[] ni = cm.getAllNetworkInfo();
			
			for( int i = 0; i < ni.length; i++){
				mLog.i(TAG, "beginHostingService() type("+ni[i].getTypeName()+") available(" + ni[i].isAvailable() + ") connected("+ni[i].isConnected()+") connected("+ni[i].isConnectedOrConnecting()+")");
				if( ni[i].isConnected() ){
					success = true;
					break;
				}
				if( ni[i].isConnectedOrConnecting() ){
					// Maybe we should sleep here instead and try again.
					success = true;
					break;
				}
				if( ni[i].isAvailable() ){
					success = true;
					break;
				}
			}
			if( success ){ break; }
			
			SystemClock.sleep(1000);
		}
		if( !success ){
			mLog.serviceState(TAG+" onReceiveWithPrivilege()","No connectivity after waiting 10 seconds and rechecking every second.");
			mLog.w(TAG, "onReceiveWithPrivilege() no connectivity available after waiting 10 seconds and rechecking each second.");
			//return;
		}//*/
		
		mLog.e(TAG,"onReceiveWithPrivilege(Action Received:"+action+")");
		
		intent.setClass(mContext, com.havenskys.galaxy.activity.NightWorker.class);
		intent.putExtra("result", getResultCode());
		//intent.putExtra("intentreceiver", true);
		intent.putExtra("com.havenskys.newsbite.who", TAG + " onReceiveWithPrivilege() " + action);
		
		beginHostingService(context,intent);
		
	}

	public static void beginHostingService(Context context, Intent intent) {
		Custom mLog = new Custom(context, TAG + " beginHostingService() 42");
		//android.intent.action.BOOT_COMPLETED
		
		mLog.i(TAG,"beginHostingService() ++++++++++++++++++++++++++++++++++++++++++++++++");
		
		mContext = context;
		synchronized (mStartingServiceSync){
			mLog.i(TAG,"beginHostingService() synchronized() ++++++++++++++++++++++++++++++++++++++++++++++++");
			if(mWakeService == null){
				mLog.i(TAG,"beginHostingService() PowerManager ++++++++++++++++++++++++++++++++++++++++++++++++");
				PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
				mWakeService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StartingSyncService");
				mWakeService.setReferenceCounted(false);
			}
			mLog.i(TAG,"beginHostingService() WakeService.acquire() ++++++++++++++++++++++++++++++++++++++++++++++++");
			mWakeService.acquire();

			mLog.i(TAG,"beginHostingService() startService() ++++++++++++++++++++++++++++++++++++++++++++++++");
			mContext.startService(intent);
		}
	}
	
	public static void finishHostingService(NightWorker service, int serviceId) {
		
		Custom mLog = new Custom(service.getApplicationContext(), TAG + " finishHostingService() 66");
		mLog.i(TAG,"finishHostingService() ++++++++++++++++++++++++++++++++++++++++++++++++");

		synchronized (mStartingServiceSync){
			if(mStartingServiceSync != null){
				mLog.i(TAG,"finishHostingService() stop Self Result");
				if( service.stopSelfResult(serviceId) ){
					mLog.i(TAG,"finishHostingService() release Wake Service");
					mWakeService.release();
				}
			}
		}
		
	}


}
