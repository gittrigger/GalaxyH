package com.havenskys.galaxy.activity;

import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.havenskys.galaxy.DbAdapter;
import com.havenskys.galaxy.R;
import com.havenskys.galaxy.SyncProcessing;

public class PersonView extends ListActivity {

	private static final String TAG = "PersonView";
	private DbAdapter mDataStore;
	private TextView mPersonName, mSearchTitle;
	private EditText mSearch;
	private Bundle mIntentExtras;
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	
	public static String SHARED_HTTPSTATUS = "httpstatus_person";
	public static String SHARED_HTTPSPEED = "httpspeed_person";
	public static int TEXTVIEWFORPRINTOUT = R.id.view_searchtitle;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view);
        
        mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    	mPreferencesEditor = mSharedPreferences.edit();
    	//mPreferencesEditor.putString("search", "");
    	String webaddress = mSharedPreferences.contains("webaddress") ? mSharedPreferences.getString("webaddress", "") : "";
    	String login = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
    	String password = mSharedPreferences.contains("password") ? mSharedPreferences.getString("password", "") : "";
    	int runcount = mSharedPreferences.contains("runcount") ? mSharedPreferences.getInt("runcount", 0) : 0;
        
        //mPreferencesEditor.putString(SHARED_HTTPSTATUS, ""); mPreferencesEditor.commit();
		//String httpstatus = sharedPreferences.contains(SHARED_HTTPSTATUS) ? sharedPreferences.getString(SHARED_HTTPSTATUS, "") : "";

		mPreferencesEditor.putInt("runcount", runcount+1);
		mPreferencesEditor.commit();
        
        mPersonName = (TextView) findViewById(R.id.view_name);
        //mPersonCompany = (TextView) findViewById(R.id.view_title);
        //mPersonCompany = (TextView) findViewById(R.id.view_company);
        mSearchTitle = (TextView) findViewById(R.id.view_searchtitle);
        mSearch = (EditText) findViewById(R.id.view_search);
        
		mDataStore = new DbAdapter(this);
		mDataStore.loadDb(TAG + " onCreate() 1","contactStore");
		
		mIntentExtras = getIntent().getExtras();
		long contactid = mIntentExtras != null ? mIntentExtras.getLong("contactid") : 0;
		
		String name = mDataStore.getString("contactStore", contactid, "name");
		mDataStore.updateEntry("contactStore",contactid,"selectedlast",System.currentTimeMillis() + 1000);
		//startManagingCursor(dataRow);
    
		mPersonName.setText(name);
		

		startWebInteractor(TAG + " onCreate() 2");
		
		mSearch.requestFocus();
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
		
	private void startWebInteractor(final String who) {
		
		Thread webInteractorService = new Thread(){
			public void run() {
				
				SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
        		
		        String lastCommandProcessed = "none";
		        Thread bgquery = null;
		        
		        long lastTime = System.currentTimeMillis() - 900;
		        DefaultHttpClient httpClient = null;
		        SyncProcessing sp = new SyncProcessing(PersonView.this);
		        sp.setSharedPreferences(sharedPreferences);
		        long httpClientBirthday = System.currentTimeMillis() - (4*60*1000);
		        boolean deepState = false;
		        for(;;){
		        	SystemClock.sleep(50);
		        	
		        	String httpstatus = sharedPreferences.contains(SHARED_HTTPSTATUS) ? sharedPreferences.getString(SHARED_HTTPSTATUS, "") : "";
        			if( httpstatus == "" ){
        				httpClientBirthday = System.currentTimeMillis() - (4*60*1000);
        			}
        			
		        	if( httpClientBirthday < (System.currentTimeMillis() - (3*60*1000) )){ // && webaddress.length() > 0 && login.length() > 0 && password.length() > 0
		        		
		        		String webaddress = sharedPreferences.contains("webaddress") ? sharedPreferences.getString("webaddress", "") : "";
	    				String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
		            	String password = sharedPreferences.contains("password") ? sharedPreferences.getString("password", "") : "";
		            	if( login.length() == 0 || password.length() == 0 || webaddress.length() == 0 ){
		            		SystemClock.sleep(1880); // sleep for a sec, no reason to JUMP when the values are available
		            		continue;
		            	}

		        		Editor pe = sharedPreferences.edit(); pe.putString(SHARED_HTTPSTATUS, "Connecting"); pe.commit();
		        		PersonView.this.safeUpdateTitle(TAG + " webInteractorService 135 for " + who);
		        		//String httpstatus = sharedPreferences.contains(SHARED_HTTPSTATUS) ? sharedPreferences.getString(SHARED_HTTPSTATUS, "") : "";
		        		
		        		safeViewUpdate("Establishing a connection with webmail site.", TEXTVIEWFORPRINTOUT, "TextView", TAG + " webInteractorService 138 for " + who);		            	
		            	
		            	for( int attempt = 1; attempt <= 3; attempt ++ ){
		            		httpClient = sp.getOwaSession(TAG + " remoteSearchService 298 for " + who, webaddress, login, password);
		            		if( httpClient != null ){
		            			Log.w(TAG,"httpClient Replied");
		            			break;
		            		}
		            		httpClientBirthday = System.currentTimeMillis() - (2*60*1000);
		            		//Editor pe = sharedPreferences.edit(); 
		            		pe.putString(SHARED_HTTPSTATUS, ""); pe.commit();
		            		PersonView.this.safeUpdateTitle(TAG + " remoteSearchService 306");
		            		//pe.putString(SHARED_HTTPSTATUS, ""); pe.commit();
			        		//String httpstatus = sharedPreferences.contains(SHARED_HTTPSTATUS) ? sharedPreferences.getString(SHARED_HTTPSTATUS, "") : "";
		            	
		            		safeViewUpdate("Establishing a connection with webmail attempt "+attempt+"/3 failed.", TEXTVIEWFORPRINTOUT, "TextView", TAG + " webInteractorService 153 for " + who);		            	
			            	
		            		SystemClock.sleep(1880 * attempt);
		            	}
		            	
		            	if( httpClient == null ){
		            		Log.w(TAG,"remoteSearchService 327 Connection failed.  SLEEP and move on.");
		            		safeViewUpdate("Establishing a connection with webmail failed, sleeping for 30 seconds then retrying.", TEXTVIEWFORPRINTOUT, "TextView", TAG + " webInteractorService 160 for " + who);		            	

		            		String newsearch = "";
		            		for( int i = 0; i < 30; i++){
		            			newsearch = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		            			//if(newsearch != "" && newsearch != search ) { break; }
		            			SystemClock.sleep(1000);
		            		}
		            	}else{
		            		//Editor pe = sharedPreferences.edit(); 
		            		Log.w(TAG,"remoteSearchService 328 Connected for " + who);
		            		pe = sharedPreferences.edit(); pe.putString(SHARED_HTTPSPEED, ""); pe.putString(SHARED_HTTPSTATUS, "Connected"); pe.commit();
		            		PersonView.this.safeUpdateTitle(TAG + " remoteSearchService 330 for " + who);
			        		//String httpstatus = sharedPreferences.contains(SHARED_HTTPSTATUS) ? sharedPreferences.getString(SHARED_HTTPSTATUS, "") : "";
		            		safeViewUpdate("Connection established with webmail site.", TEXTVIEWFORPRINTOUT, "TextView", TAG + " webInteractorService 160 for " + who);		            	
		            	}
		        		httpClientBirthday = System.currentTimeMillis();
		        		
		        	}
		        	
		        	
		        	
		        	
		        	String command = sharedPreferences.contains("command") ? sharedPreferences.getString("command", "") : "";
			        //Editor pe = sharedPreferences.edit(); pe.putString("command", "Connecting"); pe.commit();
		        	
		        	if( command.length() == 0 ){
		        		SystemClock.sleep(500);
		        		continue;
		        	}
		        	if( command != lastCommandProcessed ){
		        		
		        	}
			        
		        }
		    	
			}
		};
		webInteractorService.start();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void safeUpdateTitle(String who) {
    	
    	SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    	boolean deepsearch = sharedPreferences.contains("deepsearch") ? sharedPreferences.getBoolean("deepsearch",false) : false;
    	String httpstatus = sharedPreferences.contains(SHARED_HTTPSTATUS) ? sharedPreferences.getString(SHARED_HTTPSTATUS, "") : "";
    	String search = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
    	//Editor pe = mSharedPreferences.edit(); pe.putInt("count", count); pe.commit();
		int count = sharedPreferences.contains("count") ? sharedPreferences.getInt("count", 0) : 0;
		String httpspeed = sharedPreferences.contains(SHARED_HTTPSPEED) ? sharedPreferences.getString(SHARED_HTTPSPEED, "") : "";
		try {
			Message msg = new Message();
			Bundle b = new Bundle();
				b.putBoolean("deepsearch", deepsearch);
				b.putInt("count", count);
				b.putString(SHARED_HTTPSTATUS, httpstatus);
				b.putString(SHARED_HTTPSPEED, httpspeed);
				b.putString("who", TAG + " safeUpdateTitle() 600 for " + who);
				
			msg.setData(b);
			mTitleHandler.sendMessage(msg);
		} catch (android.util.AndroidRuntimeException e) {
			Log.w(TAG,"safeUpdateTitle() 605 Message Already taken. for " + who);
			e.printStackTrace();
		}

	}
	
	
	
    
    private Handler mTitleHandler = new Handler() {
	    public void handleMessage(Message msg) {
	    	Bundle b = msg.getData();
	    	String httpstatus = b.containsKey(SHARED_HTTPSTATUS) ? b.getString(SHARED_HTTPSTATUS) : "";
	    	String httpspeed = b.containsKey(SHARED_HTTPSPEED) ? b.getString(SHARED_HTTPSPEED) : "";
	    	String who       = b.containsKey("who") ? b.getString("who") : "";
	    	//int count = sharedPreferences.contains("count") ? sharedPreferences.getInt("count", 0) : 0;
	    	//boolean deepsearch = b.containsKey("deepsearch") ? b.getBoolean("deepsearch") : false;
	    	
			String title = "Galaxy";// + mSearchSaved;
			
			//Editor pe = sharedPreferences.edit(); pe.putString(SHARED_HTTPSTATUS, "Connecting"); pe.commit();
			
			if( httpstatus != "" ){
				title += " " + httpstatus;
			}else{
				title += " Local";
			}
			
			if( httpspeed != "" ){
				title += " " + httpspeed;
			}else{
				title += "";
			}
			
			PersonView.this.setTitle(title);
			Log.w(TAG,"mTitleHandler 652 title("+title+") for " + who);
	    }
    };
    //Message msg = new Message();
	//Bundle b = new Bundle();
	//	b.putBoolean("deepsearch", deepsearch);
	//	b.putInt("count", count);
	//	b.putString(SHARED_HTTPSTATUS, httpstatus);
    //	b.putString("who",TAG + " () for " + who);
    //
	//msg.setData(b);
	//mTitleHandler.sendMessage(msg);

	
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private int safeViewUpdate(String message, int ref, String type, String who){
    	
		try {
    		Message msg = new Message();
			Bundle b = new Bundle();
				b.putString("message", message);
				b.putInt("ref", ref);
				b.putString("type", type); // EditView, etc
				b.putString("who", who);
				
			msg.setData(b);
			mViewUpdateHandler.sendMessage(msg);
		} catch (android.util.AndroidRuntimeException e) {
			Log.w(TAG,"safeViewUpdate 291 Message Already taken, skipping this update. for " + who);
			e.printStackTrace();
			return -1;
		}
		
		return 1;
		
    }
    
    private long mLastViewUpdateTime = 0;
	private int mLastViewUpdateId = 0;
	
	private Handler mViewUpdateHandler = new Handler() {
	    public void handleMessage(Message msg) {
	    	
	    	String type, message, who; int ref;
	    	type = ""; message = ""; who = ""; ref = 0;
	    	
	    	try {
		    	Bundle b = msg.getData();
		    	type = b.containsKey("type") ? b.getString("type") : "";
		    	message = b.containsKey("message") ? b.getString("message") : "";
		    	who = b.containsKey("who") ? b.getString("who") : "";
		    	ref = b.containsKey("ref") ? b.getInt("ref") : 0;
		    	
		    	Log.w(TAG,"mViewUpdateHandler 1047 message(" + message + ") type("+type+") ref("+ref+") for " + who);
		    	//Log.w(TAG,"mViewUpdateHandler 1049 for " + who);
	    	} catch (android.util.AndroidRuntimeException e) {
	    		Log.w(TAG,"mViewUpdateHandler 1051 AndroidRuntimeException "+e.getLocalizedMessage()+" for " + who);
	    		return;
	    	}
	    	if( ref == 0 || type == "" || message == ""){
	    		Log.w(TAG,"mViewUpdateHandler 1055 missing value for " + who);
	    		return;
	    	}
	    	
	    	if( type == "TextView" ){

	    		try {
	    			TextView v = (TextView) PersonView.this.findViewById(ref);
    				if( mLastViewUpdateId == ref && mLastViewUpdateTime > (System.currentTimeMillis() - 500) ){
    					String gettext = v.getText().toString();
    					if( gettext.length() > 0 ){ message = gettext + "\n" + message; }
    				}
	    			mLastViewUpdateTime = System.currentTimeMillis();
	    			mLastViewUpdateId = ref;
	    			v.setText(message);
	    		} catch ( android.util.AndroidRuntimeException e) {
	    			Log.w(TAG,"mViewUpdateHandler 1065 AndroidRuntimeException "+e.getLocalizedMessage()+" for " + who);
	    			try {
	    				Log.w(TAG,"mViewUpdateHandler 1067 this.finalize() for " + who);
						this.finalize();
					} catch (Throwable e1) {
						Log.w(TAG,"mViewUpdateHandler 1070 Thorable "+e.getLocalizedMessage()+" for " + who);
						e1.printStackTrace();
					}
	    			e.printStackTrace();
	    		}
	    	}
	    	else 
	    	if( type == "Button" ){
	    		Button v = (Button) PersonView.this.findViewById(ref);
	    		v.setText(message);
	    	}
	    	//int duration = b.containsKey("duration") ? b.getInt("duration") : Toast.LENGTH_SHORT;
	        //Toast.makeText(RSASMS.this, message, duration).show();
	    }
	    /*
		Message msg = new Message();
		Bundle b = new Bundle();
			b.putString("message", "");
			b.putInt("ref", "");
			b.putString("type", "TextView"); // EditView, etc
			b.putString("who", TAG + " () " + who);
		
		msg.setData(b);
		mHandler.sendMessage(msg);
		//*/
	    
	};

    
    
    
    
    
    
    
    
    
    
    
    
	
	
}
