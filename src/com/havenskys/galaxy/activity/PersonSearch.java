package com.havenskys.galaxy.activity;

import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.havenskys.galaxy.Custom;
import com.havenskys.galaxy.DataProvider;
import com.havenskys.galaxy.R;
import com.havenskys.galaxy.SqliteWrapper;
import com.havenskys.galaxy.SyncProcessing;

public class PersonSearch extends ListActivity implements OnClickListener {

	private final static String TAG = "Galaxy";
	private EditText mSearch;
	private Button mSearchButton;
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	private Context mContext;
	
	private final static int ACTIVITYCODE_PERSONVIEW = 100;
	private final static int ACTIVITYCODE_CONFLOGIN = 99;
	private final static int ACTIVITYCODE_CONFDATA = 101;
	private final static int ACTIVITYCODE_CONFSYNC = 102;
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i(TAG,"onSaveInstanceState() ++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		//outState.putString("example", mExampleVarSource );
	}
    
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		//mExampleVarSource = savedInstanceState != null ? savedInstanceState.getString("example") : "";
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.main);
        
        registerForContextMenu(getListView());
        
        //mDataStore = new DbAdapter(this); 
		//mDataStore.loadDb(TAG + " onCreate() 1","contactStore");
		
		mSearchButton = (Button) findViewById(R.id.main_searchbutton);
		mSearchButton.setOnClickListener(this);
		
        mSearch = (EditText) findViewById(R.id.main_search);
        
    	mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    	mPreferencesEditor = mSharedPreferences.edit();
    	
    	int runcount = mSharedPreferences.contains("runcount") ? mSharedPreferences.getInt("runcount", 0) : 0;
    	String search = mSharedPreferences.contains("search") ? mSharedPreferences.getString("search", "") : "";
    	
    	mContext = this.getApplicationContext();
        
        mPreferencesEditor.putString("httpstatus", "");
		mPreferencesEditor.putInt("runcount", runcount+1);
		mPreferencesEditor.commit();
        
        searchFieldService(TAG + " onCreate() 2");
        localSearchService(TAG + " onCreate() 3");
		remoteSearchService(TAG + " onCreate() 4");
		
		mSearch.requestFocus();
		mSearch.setText(search);
    
    	buildBackgroundCounter(TAG + " onCreate() 5");
    	
        Intent nightWorker = new Intent();
        	nightWorker.setClass(this, NightWorker.class);
        	
    	startService(nightWorker);
    	
    	// Configuration Verification
    	String webaddress = mSharedPreferences.contains("webaddress") ? mSharedPreferences.getString("webaddress", "") : "";
    	String login = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
    	//boolean validlogin = mSharedPreferences.contains("validlogin") ? mSharedPreferences.getBoolean("validlogin", false) : false;
    	String password = mSharedPreferences.contains("password") ? mSharedPreferences.getString("password", "") : "";
    	
    	if( webaddress == "" || login == "" || password == "" ){
        	Intent configureLogin = new Intent();
        		configureLogin.setClass(this, ConfigureLogin.class);
        		
        	startActivityForResult(configureLogin,ACTIVITYCODE_CONFLOGIN);
		}
    	
    }
    
    

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
        menu.add(0, 101, 0, "Add to Contacts");
        menu.add(0, 102, 0, "Call Mobile/Office");

    }

	public boolean onContextItemSelected(final MenuItem item) {
		
		
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final String login = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
    	final String username = login.replaceFirst(".*?/", "").replaceFirst(".*?\\\\", "");
    	
    	Thread t = new Thread() {
    	
    		public void run() {
				switch(item.getItemId()){
				case 101:
					SyncProcessing sp = new SyncProcessing(PersonSearch.this);
					sp.updateAddAndroidContact(TAG + " onContextItemSelected("+info.id+")", "Galaxy: " + username, (int) info.id);
					break;
				case 102:
					long rowid = info.id;
					Cursor mobileCursor = null;
					String mobile = "";
					String phone = "";
					mobileCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"),""+rowid), new String[] {"mobile","phone"}, null, null, null);
					if( mobileCursor != null){if( mobileCursor.moveToFirst() ){ mobile = mobileCursor.getString(0); phone = mobileCursor.getString(1); } mobileCursor.close();}
					//String mobile = mDataStore.getString("contactStore", info.id, "mobile");
					if( mobile.length() > 0 ){
						Intent mobilecall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+mobile));
						startActivity(mobilecall);
					}else if( phone.length() > 0 ){
						Intent mobilecall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phone));
						startActivity(mobilecall);
					}
					
					break;
				}
    		}
    	};
    	t.start();
    	

		return super.onContextItemSelected(item);
    	//return true;
		
	}



    
    
    private void searchFieldService(final String who){
    	Thread searchFieldThread = new Thread(){
    		
    		private EditText tSearch;
    		private String tSearchSaved;
    		private SharedPreferences tSharedPreferences;
    		private Editor tPreferencesEditor;
    		private int tLastKeyCode = 0;
    		private long tLastKeyTime = 0;
    		private int tCursorLocation = 0;
    		private int tKeyAction = 0;
    		
    		public void run(){
    			
    			tSearch = (EditText) findViewById(R.id.main_search);
    			tSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    			tPreferencesEditor = tSharedPreferences.edit();
    			
    			tLastKeyCode = 0;
    	        tLastKeyTime = System.currentTimeMillis();
    	        tCursorLocation = 0;
    	        
    	        
    			tSearch.addTextChangedListener(new TextWatcher(){
    	    		public void afterTextChanged(Editable s) {
    	    			Log.w(TAG,"afterTextChanged() " + s.toString() );
    	    			
    	    			if( tSearch.hasFocus() ){
    	    				tSearchSaved = tSearch.getText().toString();
    	    				tPreferencesEditor.putString("search", tSearchSaved);
    	    				tPreferencesEditor.commit();
    	    			}else{
    	    				return;
    	    			}

    	    		}
    	    		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    	    			//Log.w(TAG,"beforeTextChanged() " + s.toString() + " start("+start+") count("+count+") after("+after+")" );
    	    			//tCursorLocation = start; // not to use for tCursorLocation, onTextChanged found to be better.
    	    		}
    	    		public void onTextChanged(CharSequence s, int start, int before, int count) {
    	    			//Log.w(TAG,"onTextChanged() " + s.toString() + " start("+start+") before("+before+") count("+count+")" );
    	    			tCursorLocation = start + count;
    	    		}
    			});
    	        
    			tSearch.setOnLongClickListener(new OnLongClickListener(){

    				public boolean onLongClick(View v) {
    					tSearch.setText("");
    					return false;
    				}
    	        	
    	        });
    			
    			
					
    	        tSearch.setOnKeyListener(new OnKeyListener(){

    				public boolean onKey(View v, int keyCode, KeyEvent event) {
    					//if( action == KeyEvent.)
    					
    					tKeyAction = event.getAction();
    					if( tKeyAction != KeyEvent.ACTION_DOWN ){
    						//Log.w(TAG,"keyCode " + keyCode);
    						return false;
    					}
    					//if( mSelectionControl != null ){ mSelectionControl.interrupt(); mSelectionControl = null; }
    					//mSelection = 0;
    					if( keyCode == tLastKeyCode && keyCode > 0){
    						if( keyCode == KeyEvent.KEYCODE_DEL && tSearch.length() > 0 ){
    							if( tLastKeyTime < (System.currentTimeMillis() - 1880/2) ){
    								tLastKeyTime = System.currentTimeMillis();
    								tLastKeyCode = 0;
    								return false;
    							}
    							int back = 0;
    							String text = tSearch.getText().toString();
    							for(back = 1; back < tSearch.length(); back++ ){
    								if( text.charAt(tCursorLocation - back) == ' ' ){
    									break;
    		    					}
    								if( (tCursorLocation - back) == 0 ){
    									break;
    								}
    							}
    							if( back == 1 ){
    								tLastKeyCode = 0;
    								return false;
    							}else{
    								
    								tLastKeyCode = 0;
    								tSearch.setSelection(tCursorLocation - back, tCursorLocation);
    								
    								return true;
    							}
    						}
    					}
    					tLastKeyTime = System.currentTimeMillis();
    					tLastKeyCode = keyCode;
    					return false;
    				}
    	        	
    	        });
    	        
    	        tSearch.setOnFocusChangeListener(new OnFocusChangeListener(){
    				public void onFocusChange(View v, boolean hasFocus) {
    					if( tSearch.getText().toString().contains("Name, Alias, Phone") ){
    						tSearch.setText("");
    						tSearch.setTextColor(Color.rgb(0, 0, 0));
    					}
    				}
    	        });
    	        
    	        if(tSearch.length() > 0 ){
    	        	tSearch.setSelection(0,tSearch.length());
    	        }
    	        
    	        //tSearchSaved = tSharedPreferences.contains("search") ? tSharedPreferences.getString("search", "") : "";
    	        //tSearch.setText(tSearchSaved);

    		}
    		

    	};
    	searchFieldThread.setPriority(Thread.MAX_PRIORITY);
    	searchFieldThread.start();
    }
    
    private void remoteSearchService(final String who) {

		// Background Query
		Thread remoteSearchThread = new Thread(){

			
			private EditText tSearch;
			private String tSearchSaved;
			private SharedPreferences tSharedPreferences;
			private Editor tPreferencesEditor;
			
        	public void run(){
        		tSearch = (EditText) findViewById(R.id.main_search);
    			tSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    			tPreferencesEditor = tSharedPreferences.edit();
    			
        		//Log.w(TAG,"TypeWatcher Background Query() ========================");
        		
        		SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
        		
        		//String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
		        String search = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		        String lastSearch = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		        String lastSearchProcessed = "none";
		        
		        long lastTime = System.currentTimeMillis() - 900;
		        DefaultHttpClient httpClient = null;
		        SyncProcessing sp = new SyncProcessing(PersonSearch.this);
		        sp.setSharedPreferences(sharedPreferences);
		        long httpClientBirthday = System.currentTimeMillis() - (4*60*1000);
		        
		        for(;;){
		        	
		        	SystemClock.sleep(50);
		        	

		        	
		        	String httpstatus = sharedPreferences.contains("httpstatus") ? sharedPreferences.getString("httpstatus", "") : "";
        			if( httpstatus == "" ){
        				httpClientBirthday = System.currentTimeMillis() - (4*60*1000);
        			}

        			
		        	if( httpClientBirthday < (System.currentTimeMillis() - (3*60*1000) )){ // && webaddress.length() > 0 && login.length() > 0 && password.length() > 0
		        		
		        		String webaddress = sharedPreferences.contains("webaddress") ? sharedPreferences.getString("webaddress", "") : "";
	    				String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
		            	String password = sharedPreferences.contains("password") ? sharedPreferences.getString("password", "") : "";
		            	boolean validlogin = sharedPreferences.contains("validlogin") ? sharedPreferences.getBoolean("validlogin", false) : false;
		            	if( login.length() == 0 || password.length() == 0 || webaddress.length() == 0 || !validlogin ){
		            		SystemClock.sleep(1880); // sleep for a sec, no reason to JUMP when the values are not available
		            		continue;
		            	}
		            	
		        		//Log.w(TAG,"Refreshing HttpClient");

		        		Editor pe = sharedPreferences.edit(); pe.putString("httpstatus", "Connecting"); pe.commit();
		        		//String

		        		PersonSearch.this.safeUpdateTitle(TAG + " remoteSearchService 277 for " + who);
		        		//String httpstatus = sharedPreferences.contains("httpstatus") ? sharedPreferences.getString("httpstatus", "") : "";
		        		safeViewUpdate("Establishing a connection with webmail site.", R.id.main_searchtitle, "TextView", TAG + " remoteSearchService 256 for " + who);
		            	
		            	for( int attempt = 1; attempt <= 3; attempt ++ ){
		            		httpClient = sp.getOwaSession(TAG + " remoteSearchService 298 for " + who, webaddress, login, password);
		            		if( httpClient != null ){
		            			Log.w(TAG,"httpClient Replied");
		            			break;
		            		}
		            		httpClientBirthday = System.currentTimeMillis() - (2*60*1000);
		            		//Editor pe = sharedPreferences.edit(); 
		            		pe.putString("httpstatus", ""); pe.commit();
		            		pe.putBoolean("validlogin", false);
		            		pe.commit();
		            		PersonSearch.this.safeUpdateTitle(TAG + " remoteSearchService 306");
		            		//pe.putString("httpstatus", ""); pe.commit();
			        		//String httpstatus = sharedPreferences.contains("httpstatus") ? sharedPreferences.getString("httpstatus", "") : "";
		            		
		            		safeViewUpdate("Establishing a connection with webmail attempt "+attempt+"/3 failed.", R.id.main_searchtitle, "TextView", TAG + " remoteSearchService 271 for " + who);
			        		
		            		SystemClock.sleep(1880 * attempt);
		            	}
		            	if( httpClient == null ){
		            		Log.w(TAG,"remoteSearchService 327 Connection failed.  SLEEP and move on.");
		            		safeViewUpdate("Establishing a connection with webmail failed, sleeping for 30 seconds then retrying.", R.id.main_searchtitle, "TextView", TAG + " remoteSearchService 277 for " + who);

		            		String newsearch = "";
		            		for( int i = 0; i < 30; i++){
		            			newsearch = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		            			if(newsearch != "" && newsearch != search ) { break; }
		            			SystemClock.sleep(1000);
		            		}
		            	}else{
		            		//Editor pe = sharedPreferences.edit(); 
		            		Log.w(TAG,"remoteSearchService 328 Connected for " + who);
		            		//pe = sharedPreferences.edit();
		            		pe.putString("httpspeed", "");
		            		pe.putString("httpstatus", "Connected");
		            		pe.putBoolean("validlogin", true);
		            		pe.commit();
		            		PersonSearch.this.safeUpdateTitle(TAG + " remoteSearchService 330 for " + who);
			        		//String httpstatus = sharedPreferences.contains("httpstatus") ? sharedPreferences.getString("httpstatus", "") : "";
			        		
		            		safeViewUpdate("Connection established with webmail site.", R.id.main_searchtitle, "TextView", TAG + " remoteSearchService 292 for " + who);

		            	}
		        		httpClientBirthday = System.currentTimeMillis();
		        		
		        	}
		        	
		        	boolean validlogin = sharedPreferences.contains("validlogin") ? sharedPreferences.getBoolean("validlogin", false) : false;
		        	if( !validlogin ){
		        		Log.w(TAG,"remoteSearchService 415 no valid login, don't search for " + who);
		        		SystemClock.sleep(1880);
		        		continue;
		        	}
		        	
		        	search = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		        	
		        	
		        	// Search is determined
		        	
			        if( search != lastSearch ){
			        	lastSearch = search;
			        	lastTime = System.currentTimeMillis();
			        }
			        
			        else if( search != lastSearchProcessed && lastTime < ( System.currentTimeMillis() - 1880 )){
			        	
			        	Log.w(TAG,"remoteSearchService 385 Starting Remote Query " + search + " for " + who);
			        	buildBackgroundQuery(TAG + " remoteSearchService 386 for " + who, httpClient);
		        	
			        	lastTime = System.currentTimeMillis();
			        	lastSearchProcessed = search;
			        }else{
			        	SystemClock.sleep(500);
			        }
			        
			        
		        }
		        
		        
        	}

		};
		//t.setDaemon(true);
		remoteSearchThread.start();
	}
    
    
	private void localSearchService(final String who) {
        // List Query
    	Thread localSearchThead = new Thread(){
    		
    		private EditText tSearch;
			private SharedPreferences tSharedPreferences;
			private Editor tPreferencesEditor;
			private boolean tDeepState = false;
			private boolean tShowCounter = false;
			
			
        	public void run(){
        		tSearch = (EditText) findViewById(R.id.main_search);
    			tSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    			tPreferencesEditor = tSharedPreferences.edit();
        		
		        String search = tSharedPreferences.contains("search") ? tSharedPreferences.getString("search", "") : "";
		        int count = tSharedPreferences.contains("count") ? tSharedPreferences.getInt("count", 0) : 0;
		        String httpstatus = tSharedPreferences.contains("httpstatus") ? tSharedPreferences.getString("httpstatus", "") : "";
		        String httpspeed = tSharedPreferences.contains("httpspeed") ? tSharedPreferences.getString("httpspeed", "") : "";
		        String searchstatus = tSharedPreferences.contains("searchstatus") ? tSharedPreferences.getString("searchstatus", "") : ""; // (searchstatus)
		        String lastSearch = tSharedPreferences.contains("search") ? tSharedPreferences.getString("search", "") : ""; // yeah, "search" is not a typeo
		        String lastSearchProcessed = "";
		        long lastChangeTime = System.currentTimeMillis() - 900;
		        String escalation = "";
		        
		        String knownHttpStatus = httpstatus;long lastKnownHttpChange = System.currentTimeMillis();
		        String knownSearchStatus = httpstatus;long lastKnownSearchChange = System.currentTimeMillis();
		        SyncProcessing sp = new SyncProcessing(PersonSearch.this);
		        sp.setSharedPreferences(tSharedPreferences);
		        
		        for(;;){

		        	SystemClock.sleep(50);
		        	
		        	search = tSharedPreferences.contains("search") ? tSharedPreferences.getString("search", "") : "";

		        	// Register Changes
			        if( search != lastSearch ){
			        	lastSearch = search;
			        	lastChangeTime = System.currentTimeMillis();
			        }

		        	
		        	searchstatus = tSharedPreferences.contains("searchstatus") ? tSharedPreferences.getString("searchstatus", "") : ""; // (searchstatus)
		        	
			        if( search != lastSearchProcessed && lastChangeTime < ( System.currentTimeMillis() - 1880 )){
			        	Log.w(TAG,"localSearchService 464 Search is "+search+" for " + who);
			        	tShowCounter = false;
			        	// this shields the user from seeing the connection in a hung state and save the processor this request that might not be of the utmost i
			        	
	            		safeViewUpdate("Searching cached personal and corporate listings.", R.id.main_searchtitle, "TextView", TAG + " localSearchService 403 for " + who);
			        	

			        	Log.w(TAG,"localSearchService 471 Starting Local Data Query " + search + " for " + who);
			        	tDeepState = false;
			        	tPreferencesEditor.putBoolean("deepsearch", false); tPreferencesEditor.commit();
			        	
			        	Message msg = new Message();
						Bundle b = new Bundle();
							b.putString("who", TAG + " localSearchService 449 for " + who);
						msg.setData(b);
						mViewClicker.sendMessage(msg);
						
			        	lastChangeTime = System.currentTimeMillis();
			        	lastSearchProcessed = search;
			        }
			        
			        else if( ( (count == 0 || searchstatus == "" || searchstatus.contains("slo")) || lastChangeTime < ( System.currentTimeMillis() - 1880 * 10 ))  && !tDeepState && lastChangeTime < ( System.currentTimeMillis() - 1880 * 2 )){
			        	tDeepState = true;
						
						lastChangeTime = System.currentTimeMillis();
			        	tPreferencesEditor.putBoolean("deepsearch", true); tPreferencesEditor.commit();
			        	
			        	Message msg = new Message();
						Bundle b = new Bundle();
							b.putString("who", TAG + " localSearchService 465 for " + who);
						msg.setData(b);
						mViewClicker.sendMessage(msg);
						
			        }else if( !tShowCounter && lastChangeTime < (System.currentTimeMillis() - 1880 * 30) ){
			        	tShowCounter = true;
			        	buildBackgroundCounter(TAG + " localSearchService 502 for " + who);
			        }else if( search == lastSearchProcessed ){
			        	SystemClock.sleep(500);
			        }
			        
			        httpstatus = tSharedPreferences.contains("httpstatus") ? tSharedPreferences.getString("httpstatus", "") : "";
			        searchstatus = tSharedPreferences.contains("searchstatus") ? tSharedPreferences.getString("searchstatus", "") : "";
			        search = tSharedPreferences.contains("search") ? tSharedPreferences.getString("search", "") : "";
			        httpspeed = tSharedPreferences.contains("httpspeed") ? tSharedPreferences.getString("httpspeed", "") : "";
			          
			        
			        
			        if( httpstatus.length() > 0 ){
			        	
			        	if( knownHttpStatus != httpstatus ){
				        	knownHttpStatus = httpstatus;
				        	escalation = "";
				        	lastKnownHttpChange = System.currentTimeMillis();
				        	tPreferencesEditor.putString("httpspeed", "");
				        	tPreferencesEditor.commit();
				        	PersonSearch.this.safeUpdateTitle(TAG + " localSearchService 519 for " + who);
			        	}
				        
				        if( httpstatus.contains("Connecting") ){ // 5 ms, is this shorter or longer than a comparison, seems more reliable.
					        	
					        	if( lastKnownHttpChange < (System.currentTimeMillis() - 1880 * 30) ){ escalation = "huuuuung"; }
					        	else if( lastKnownHttpChange < (System.currentTimeMillis() - 1880*9) ){ escalation = "slooooow"; }
					        	else if( lastKnownHttpChange < (System.currentTimeMillis() - 1880*7) ){ escalation = "sloooow"; }
					        	else if( lastKnownHttpChange < (System.currentTimeMillis() - 1880*5) ){ escalation = "slooow"; }
					        	else if( lastKnownHttpChange < (System.currentTimeMillis() - 1880*3) ){ escalation = "sloow"; }
					        	else if( lastKnownHttpChange < (System.currentTimeMillis() - 1880) ){ escalation = "slow"; }
					        	
					        	if( httpspeed != escalation ){
					        		tPreferencesEditor.putString("httpspeed", escalation); tPreferencesEditor.commit();
					        		Log.w(TAG,"localSearchService 545 httpstatus("+httpstatus+") searchstatus("+searchstatus+") httpspeed("+httpspeed+") escalation("+escalation+") search("+search+") for " + who);
					        		PersonSearch.this.safeUpdateTitle(TAG + " localSearchService 546 for " + who);
					        	}

				        }else{
	
					        if( searchstatus.length() > 0 && search == searchstatus ){ // 5 ms, is this shorter or longer than a comparison, seems more reliable.
						        if( knownSearchStatus == searchstatus ){
						        	
						        	if( lastKnownSearchChange < (System.currentTimeMillis() - 1880 * 30) ){ escalation = "huuuuung"; }
						        	else if( lastKnownSearchChange < (System.currentTimeMillis() - 1880*9) ){ escalation = "slooooow"; }
						        	else if( lastKnownSearchChange < (System.currentTimeMillis() - 1880*7) ){ escalation = "sloooow"; }
						        	else if( lastKnownSearchChange < (System.currentTimeMillis() - 1880*5) ){ escalation = "slooow"; }
						        	else if( lastKnownSearchChange < (System.currentTimeMillis() - 1880*3) ){ escalation = "sloow"; }
						        	else if( lastKnownSearchChange < (System.currentTimeMillis() - 1880) ){ escalation = "slow"; }
						        	
						        	if( httpspeed != escalation ){
						        		tPreferencesEditor.putString("httpspeed", escalation); tPreferencesEditor.commit();
						        		Log.w(TAG,"localSearchService 552 httpstatus("+httpstatus+") searchstatus("+searchstatus+") httpspeed("+httpspeed+") escalation("+escalation+") search("+search+") for " + who);
						        		PersonSearch.this.safeUpdateTitle(TAG + " localSearchService 553 for " + who);
						        	}
							        
						        }else{
						        	Log.w(TAG,"localSearchService 560 searchstatus("+knownSearchStatus+" to "+searchstatus+") change for " + who);
						        	knownSearchStatus = searchstatus;
						        	escalation = "";
						        	lastKnownSearchChange = System.currentTimeMillis();
						        	tPreferencesEditor.putString("httpspeed", ""); tPreferencesEditor.commit();
						        }
					        }
					      
				        }
			        }
			        
			        
			        
		        }//for
		        
		        
        	}
		};

		localSearchThead.start();
	}
	
	
	
	private void safeUpdateTitle(String who) {
    	
    	SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    	boolean deepsearch = sharedPreferences.contains("deepsearch") ? sharedPreferences.getBoolean("deepsearch",false) : false;
    	String httpstatus = sharedPreferences.contains("httpstatus") ? sharedPreferences.getString("httpstatus", "") : "";
    	boolean validlogin = sharedPreferences.contains("validlogin") ? sharedPreferences.getBoolean("validlogin",false) : false;

		int count = sharedPreferences.contains("count") ? sharedPreferences.getInt("count", 0) : 0;
		String httpspeed = sharedPreferences.contains("httpspeed") ? sharedPreferences.getString("httpspeed", "") : "";
		try {
			Message msg = new Message();
			Bundle b = new Bundle();
				b.putBoolean("deepsearch", deepsearch);
				b.putInt("count", count);
				if( validlogin ){
					b.putString("httpstatus", httpstatus);
					b.putString("httpspeed", httpspeed);
				}else{
					b.putString("httpstatus", "");
					b.putString("httpspeed", "(login-failure)");
				}
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
	    	String httpstatus = b.containsKey("httpstatus") ? b.getString("httpstatus") : "";
	    	String httpspeed = b.containsKey("httpspeed") ? b.getString("httpspeed") : "";
	    	String who       = b.containsKey("who") ? b.getString("who") : "";
	    	//int count = sharedPreferences.contains("count") ? sharedPreferences.getInt("count", 0) : 0;
	    	boolean deepsearch = b.containsKey("deepsearch") ? b.getBoolean("deepsearch") : false;
	    	int count = b.containsKey("count") ? b.getInt("count") : 0;
	    	
			String title = "Galaxy";
			
			if( deepsearch ){
				title += " DeepSearch";
			}else{
				if( count > 0 ){
					title += " NameSearch";
				}
			}
			if( count > 0 ){
				title += " (" + count + ")";
			}
			
			if( httpstatus != "" ){
				title += " " + httpstatus;
			}else{
				if( count > 0 ){
					title += " Local";
				}
			}
			
			if( httpspeed != "" ){
				title += " " + httpspeed;
			}else{
				title += "";
			}
			
			PersonSearch.this.setTitle(title);
			Log.w(TAG,"mTitleHandler 652 title("+title+") for " + who);
			//Toast.makeText(mContext, title, Toast.LENGTH_LONG).show();
	    }
    };
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    super.onActivityResult(requestCode, resultCode, intent);
	    
	    
		switch(requestCode){
		case ACTIVITYCODE_CONFLOGIN:
			mSearchButton.performClick();
    		safeViewUpdate("Configuration saved. Type to search.", R.id.main_searchtitle, "TextView", TAG + " onActivityResult() 1 for ACTIVITYCODE_CONFLOGIN");// plain text
			break;
		}
	}

	private void buildBackgroundCounter(final String who) {
		
		Thread t = new Thread(){
        	public void run(){
        		//DbAdapter db = new DbAdapter(PersonSearch.this);
				//db.loadDb(TAG + " buildBackgroundCounter() 721","contactStore");
        		
        		Cursor countCursor = null;
				int count = 0;
				countCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), new String[] {"count(*)"}, "status > 0", null, null);
				if( countCursor != null){if( countCursor.moveToFirst() ){ count = countCursor.getInt(0); } countCursor.close();}
        		
				//int count = 0;
				//count = db.getCount("contactStore", "status > 0");
				String message = "";
				
				if( count > 1 ){
					message = "Local cache contains " + count + " records.";
				}else if( count == 1 ){
					message = "Local cache contains " + count + " record.";
				}else if(count < 1){
					message = "Local cache is empty.";
				}
				
	    		safeViewUpdate(message, R.id.main_searchtitle, "TextView", TAG + " buildBackgroundCounter() 662 for " + who);

        	}
		};
		
		t.start();
	}
    
	private void buildBackgroundQuery(final String who, final DefaultHttpClient httpClient) {
		
		
		Thread t = new Thread(){
        	public void run(){
        		
        		//Log.w(TAG,"backgroundQuery() ========================");
        		
        		SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);

        		String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
		        String search = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		        String username = login.replaceAll(".*?/", "")
				.replaceAll(".*?\\\\", "");
		        if( search == "" ){ search = username; Log.w(TAG,"509 Searching for username("+username+")");}
		        int age = sharedPreferences.contains("age") ? sharedPreferences.getInt("age", 5) : 5;
		
		        
		        Cursor searchStore = null;
				searchStore = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"searchStore"), new String[] {Custom.ID,Custom.LAST_UPDATED}, "status > 0 AND search = \""+search+"\"", null, null);
        		startManagingCursor(searchStore);
        		if( searchStore == null ){
        			Log.e(TAG,"buildBackgroundQuery() 783 searchStore query resulted in null, table might not exist. for " + who);
        			return;
        		}
        		if( !searchStore.moveToFirst() ){
        			Log.w(TAG,"buildBackgroundQuery() 787 searchStore query resulted in empty, might be a new search, saving. for " + who);
        			ContentValues cv = new ContentValues();
        			cv.put("search", search);
        			SqliteWrapper.insert(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"searchStore"), cv);
        		}else{
        			long rowId     = searchStore.getLong(0);
        			long updated   = Long.parseLong(searchStore.getString(1));
        			Log.w(TAG,"buildBackgroundQuery() 792 searchStore replied with row("+rowId+") updated("+(updated/1000)+") for search("+search+") compared with("+(System.currentTimeMillis()/1000)+") for " + who);
        			if( (updated) > System.currentTimeMillis() - (age*60*1000) ){ //60 seconds is tight, too tight
        				
	            		safeViewUpdate("These results are "+(int)( (System.currentTimeMillis()-updated)/1000 )+" seconds old.", R.id.main_searchtitle, "TextView", TAG + " buildBackgroundQuery() 741 for " + who);
        				
        				
        				Editor pe = sharedPreferences.edit(); pe.putString("searchstatus", ""); pe.putString("httpspeed", ""); pe.commit(); // (httpspeed)
        				PersonSearch.this.safeUpdateTitle(TAG + " buildBackgroundQuery() 813 for " + who);
        				
        				return;
        			}else{
        				ContentValues cv = new ContentValues();
            			cv.put("status", 1);
        				SqliteWrapper.update(mContext, mContext.getContentResolver(), Uri.withAppendedPath(Uri.withAppendedPath(DataProvider.CONTENT_URI,"searchStore"),""+rowId), cv, null, null);
        			}
        		}
        		//searchStore.close();
        		
        		
		    	String searchstatus = sharedPreferences.contains("searchstatus") ? sharedPreferences.getString("searchstatus", "") : ""; // (searchstatus)
		        Editor pe = sharedPreferences.edit(); pe.putString("searchstatus", search); pe.commit(); // (searchstatus)
		        
	    		safeViewUpdate("Remote webmail contact search is underway.", R.id.main_searchtitle, "TextView", TAG + " buildBackgroundQuery() 782 for " + who);

				
				long start = System.currentTimeMillis();
        		//*
		        SyncProcessing s = new SyncProcessing(PersonSearch.this);
		        s.setSharedPreferences(sharedPreferences);
		        String newText = "";
		        
		        String webaddress = sharedPreferences.contains("webaddress") ? sharedPreferences.getString("webaddress", "") : "";
		    	login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
		    	String password = sharedPreferences.contains("password") ? sharedPreferences.getString("password", "") : "";
		    	
		    	// return 1:good 0:genericproblem -1:loginfailuremissingrefkey -2:badusernamepassword -3:missingvalues -4:wrongversion -5:goodversionbadparse -6:httpfailurehostnotresolved -7:generichttpfailure
		    	Log.w(TAG,"buildBackgroundQuery() 986 goto SyncProcessing.searchContact() for " + who);
		    	int status = s.searchContact(TAG + " buildBackgroundQuery() 897 for " + who, webaddress,login,password,search,httpClient);
		    	//Editor pe = mSharedPreferences.edit(); 
		        if( status > 0 ){
		        	newText = "Remote webmail search in "+(int) ( (System.currentTimeMillis() - start)/1000)+" seconds.";
		        }else if( status == -9 ){
		        	newText = "Login reply from webmail was empty, which happened after correctly receiving the initial login page, maybe webmail site is down.";
		        }else if( status == -8 ){
		        	newText = "Login reply from webmail wasn't was was expected, which happened after correctly receiving the initial login page, maybe webmail site is down.";
		        }else if( status == -7 ){
		        	newText = "Generic HTTP client failure, failure could be due to network connectivity locally or between here and there.";
		        }else if( status == -6 ){
		        	newText = "Webmail site URL is not resolving in an IP Address, no DNS Lookup available, failure could be due to network connectivity locally or between here and there.";
		        }else if( status == -5 ){
		        	newText = "Webmail version was determined correct but I wasn't able to understand the login reply.";//, I'm looking for 
		        }else if( status == -4 ){
		        	newText = "Webmail version isn't Microsoft Outlook Webmail (OWA 8), email support havenskys@gmail.com to get support added for your webmail service, mention 'Galaxy' in the subject.";
		        }else if( status == -3 ){
		        	newText = "Displaying only local records, one of the configuration values is empty.";
		        }else if( status == -2 ){
		        	newText = "Displaying only local storage, positively identified a Login/Password incorrect error.";
		        }else if( status == -1 ){
		        	newText = "Login failed, missing expected login result.";
		        }else{
		        	newText = "Displaying only local storage, I had a problem connecting (maybe you know why, give it another try if you want). " + status;
		        }
		        if( status < 0 ){
		        	pe.putString("httpstatus", ""); 
		        	pe.putBoolean("validlogin", false);
		        	pe.commit();
		        }else{
		        	pe.putBoolean("validlogin", true);
		        	pe.commit();
		        }
		        
		        
		        
		        String currentSearch = sharedPreferences.contains("search") ? sharedPreferences.getString("search", "") : "";
		        searchstatus = sharedPreferences.contains("searchstatus") ? sharedPreferences.getString("searchstatus", "") : ""; // if it still == (searchstatus)
		        Log.w(TAG,"buildBackgroundQuery() 919 returnfrom SyncProcessing.searchContact() status("+status+") message("+newText+") searchstatus("+searchstatus+") search("+search+") currentSearch("+currentSearch+") for " + who);
		        pe = sharedPreferences.edit(); pe.putString("httpspeed", ""); pe.commit();
		        if( searchstatus == search && search == currentSearch ){
		        	/*Editor*/
		        	pe = sharedPreferences.edit();
		        	pe.putString("searchstatus", ""); 
		        	pe.putString("httpspeed", ""); pe.commit();
		        	PersonSearch.this.safeUpdateTitle(TAG + " buildBackgroundQuery() 926 for " + who);
		        } // if it still == (searchstatus)
		        
				if( currentSearch.length() == 0 || search == currentSearch ){
					
		    		safeViewUpdate(newText, R.id.main_searchtitle, "TextView", TAG + " buildBackgroundQuery() 848 for " + who);

		    		
					if( status > 0 ){ // only need to load the data when we've gathered it.
						Message msg = new Message();
						Bundle b = new Bundle();
							b.putString("who", TAG + " buildBackgroundQuery 794 for " + who);
						msg.setData(b);
						mViewClicker.sendMessage(msg);
					}
				}
				
				
		        //*/
        	}
        };
        t.start();
	}
	
	
	
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
    
    
    private Handler mViewClicker = new Handler() {
	    public void handleMessage(Message msg) {
	    	Bundle b = msg.getData();
	    	String who = b.containsKey("who") ? b.getString("who") : "";
	    	Log.w(TAG,"mViewClicker 831 for " + who);
	    	mSearchButton.performClick();
	    }
	};
    
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
	    			TextView v = (TextView) PersonSearch.this.findViewById(ref);
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
						Log.w(TAG,"mViewUpdateHandler 1070 Throwable "+e.getLocalizedMessage()+" for " + who);
						e1.printStackTrace();
					}
	    			e.printStackTrace();
	    		}
	    		
	    	}
	    
	    }
	    
	};

    
    
	@Override
	protected void onRestart() {
		Log.i(TAG,"onRestart() ++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		if( mSearchButton != null ){
			mSearchButton.performClick();
		}
		super.onRestart();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG,"onStart() ++++++++++++++++++++++++++++++++++++++++++++++++++++++++");		
	}
	



	private void listLoader(String who) {
    	Log.i(TAG,"listLoader 1147 for " + who);
    	
    	ListView mainListView = (ListView) findViewById(android.R.id.list);
    	TextView noResultsView = (TextView) findViewById(android.R.id.empty);
    	noResultsView.setText("Searching");
    	//SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
    	mPreferencesEditor.putInt("count", 0);
    	mPreferencesEditor.commit();
    	
    	boolean deepsearch = mSharedPreferences.contains("deepsearch") ? mSharedPreferences.getBoolean("deepsearch",false) : false;
    	String search = mSharedPreferences.contains("search") ? mSharedPreferences.getString("search", "") : "";
    	search = search
    				.replaceAll("\"", "")
    				.trim();
    	String login = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
    	String username = login.replaceFirst(".*?/", "").replaceFirst(".*?\\\\", "");
    	
    	if( search.length() == 0){ Log.w(TAG,"listLoader 1237 Final search is empty.");}
    	else { Log.w(TAG,"listLoader 1160 Final Search is "+search+" for " + who); }


    	//if( !mDataStore.isOpen() ){ mDataStore.close(); mDataStore = null;}
		//if( mDataStore == null ){ 
			//mDataStore = new DbAdapter(this); 
			//mDataStore.loadDb(TAG + " listLoader 1164 for " + who,"contactStore");
		//}
		
		String listWhere = "";
    	Cursor listCursor = null;
    	
    	Cursor idCursor = null;
		long myrecordid = -1;
		idCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), new String[] {"_id"}, "alias = \""+username.replaceAll("\"", "")+"\"", null, null);
		if( idCursor != null){if( idCursor.moveToFirst() ){ myrecordid = idCursor.getLong(0); } idCursor.close();}
    	//long myrecordid = mDataStore.getId("contactStore", "alias = \""+username.replaceAll("\"", "")+"\"");
		
    	if( myrecordid > 0 ){
    		// GOOD
    		Log.w(TAG,"listLoader 1269 I found my personal record");
    	}else{
    		if( search == "" ){
    			search = username;
    		}
    	}
    	
    	
    	if( search == "" ){
    		ContentValues cv = new ContentValues();
    		cv.put("selectedlast", System.currentTimeMillis() );
    		SqliteWrapper.update(mContext, mContext.getContentResolver(), Uri.withAppendedPath(Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"),""+myrecordid), cv, null, null);
    		//mDataStore.updateEntry("contactStore",myrecordid,new String[] {"selectedlast"}, new long[]{ System.currentTimeMillis() } );
			//listCursor = mDataStore.detailQuery("contactStore", new String[] {"_id","name","alias","title","phone","office","company"}, "status > 0 AND selectedlast > 0", null, null, null, "selectedlast asc", null);
    		listWhere = "status > 0 AND selectedlast > 0";
			
			
    	}else 
		if( search != "" ){
			if( search.trim().contains(" ") ){
				String[] slist = search.split(" ");
				String newSearch = "";
				for(int i = 0; i < slist.length; i++){
					if( slist[i].trim().length() > 0 ){ //trim \n
						newSearch += " AND name like \"%"+slist[i].trim()+"%\"";
					}
				}
				Log.w(TAG,"New Search " + newSearch );
				//listCursor = mDataStore.detailQuery("contactStore", new String[] {"_id","name","alias","title","phone","office","company"} ,"status > 0 "+newSearch+" ", null, null, null, "selectedlast desc, name asc", null);
				listWhere = "status > 0 "+newSearch+" ";
			}else{
				if( !deepsearch ){
					//listCursor = mDataStore.detailQuery("contactStore", new String[] {"_id","name","alias","title","phone","office","company"},"status > 0 AND name like \"%"+search+"%\"", null, null, null, "selectedlast desc, name asc", null);
					listWhere = "status > 0 AND name like \"%"+search+"%\"";
				}else{
					/*/
					listCursor = mDataStore.detailQuery("contactStore", new String[] {"_id","name","alias","title","phone","office","company"},"status > 0" +
							" AND (name like \"%"+search+"%\"" +
							" OR alias like \"%"+search+"%\"" +
							" OR phone like \"%"+search+"%\"" +
							" OR email like \"%"+search+"%\"" +
							")", null, null, null, "selectedlast desc, name asc", null);
					//*/
					listWhere = "status > 0" +
						" AND (name like \"%"+search+"%\"" +
						" OR alias like \"%"+search+"%\"" +
						" OR phone like \"%"+search+"%\"" +
						" OR email like \"%"+search+"%\" )";
				}
			}
		}
		
    	listCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), 
				new String[] {"_id","name","alias","title","phone","office","company"},
        		listWhere, // Future configurable time to expire seen and unread
        		null, 
        		null);
		
		if( listCursor == null ){
			Log.e(TAG,"listLoader 1200 listCursor is null for " + who);
			return;
		}
		
		startManagingCursor(listCursor);
		
		if( !listCursor.moveToFirst() ){
			Log.w(TAG,"listLoader 1204 listCursor is empty for " + who);
		
		}else if( !(listCursor.getColumnIndex("name") > 0) ){
			Log.e(TAG,"listLoader 1207 listCursor, firstrow is missing column 'name' for " + who);
			listCursor.close();
			return;
		}

		

		int count = listCursor.getCount();
		if( count > 0 ){
			mPreferencesEditor.putInt("count", count);
	    	mPreferencesEditor.commit();
	    	
			noResultsView.setText("Found " + count + " Records");
			
			//SystemClock.sleep(100);
			
	    	mainListView.setVisibility(ListView.VISIBLE);

	        String[] from = new String[]{ "name", "title", "phone", "office", "company" };
	        int[] to = new int[]{ R.id.contactrow_name, R.id.contactrow_title, R.id.contactrow_phone, R.id.contactrow_office, R.id.contactrow_company };

	        SimpleCursorAdapter entries = new SimpleCursorAdapter(PersonSearch.this, R.layout.contactrow, listCursor, from, to);
	        //setListAdapter(entries);
	        getListView().setAdapter(entries);
	        
		}else{
			noResultsView.setText("No results found.");
		}

		safeUpdateTitle(TAG + " listLoader 1218 for " + who);
		
    }

		

	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Intent i = new Intent(this, PersonView.class);
    		i.putExtra("contactid", id);
    		
    	//startActivityForResult(i,ACTIVITYCODE_PERSONVIEW);
	}

	public void onClick(View v) {
		if( v.getId() == R.id.main_searchbutton ){
			listLoader(TAG + " onClick() R.id.main_search");
		}
	}
	

}

