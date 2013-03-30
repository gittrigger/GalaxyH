package com.havenskys.galaxy.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.havenskys.galaxy.R;
import com.havenskys.galaxy.SyncProcessing;

public class ConfigureLogin extends Activity implements TextWatcher, OnFocusChangeListener, OnClickListener, OnCheckedChangeListener {

	private final static String TAG = "ConfigureLogin";
	private String mWebaddressSaved, mLoginSaved, mPasswordSaved;
	//private int mAgeSaved;
	private Boolean mSyncAndroidSaved;
	private EditText mWebaddress, mLogin, mPassword;//, mAge;
	private Button mVerify, mClose;
	private TextView mWebaddressTitle, mLoginTitle, mPasswordTitle;//, mAgeTitle;
	private ToggleButton mSyncAndroid;
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG,"onCreate() 38");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        Bundle extras = getIntent().getExtras();
		String which = extras != null ? extras.getString("which") : "webmail";

        setContentView(R.layout.configure_webmail);
		Log.w(TAG,"onCreate() 49 which("+which+")");
		
    	//*/
        mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
        
        
        //
			
        mWebaddressSaved = mSharedPreferences.contains("webaddress") ? mSharedPreferences.getString("webaddress", "") : "";
    	mLoginSaved = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
    	mPasswordSaved = mSharedPreferences.contains("password") ? mSharedPreferences.getString("password", "") : "";
		
		mWebaddress = (EditText) findViewById(R.id.configure_url);
        mLogin = (EditText) findViewById(R.id.configure_login);
        mPassword = (EditText) findViewById(R.id.configure_password);

    	if( mWebaddressSaved != "" ){
    		mWebaddress.setText(mWebaddressSaved);
    		mWebaddress.setTextColor(Color.rgb(0, 0, 0));
    	}else{
    		mWebaddress.requestFocus();
    		mWebaddress.setText("https://");
    		mWebaddress.setSelection(mWebaddress.length(),mWebaddress.length());
    		mWebaddress.setTextColor(Color.rgb(0, 0, 0));
    	}
    	if( mLoginSaved != "" ){
    		mLogin.setText(mLoginSaved);
    		mLogin.setTextColor(Color.rgb(0, 0, 0));
    	}
    	if( mPasswordSaved != "" ){
    		mPassword.setText(mPasswordSaved);
    		mPassword.setTextColor(Color.rgb(0, 0, 0));
    	}
    	
    	mWebaddress.addTextChangedListener(this);
    	mWebaddress.setOnFocusChangeListener(this);
    	
    	mPassword.addTextChangedListener(this);
       	mPassword.setOnFocusChangeListener(this);
        mLogin.addTextChangedListener(this);
       	mLogin.setOnFocusChangeListener(this);
       	
       	mVerify = (Button) findViewById(R.id.configure_verify);
    	mVerify.setOnClickListener(this);

    	mClose = (Button) findViewById(R.id.configure_close);
    	mClose.setOnClickListener(this);
    	
    	
	}

	public void onClick(View v) {
		
		if( v.getId() == R.id.configure_close ){
			this.finish();
		}
		else if( v.getId() == R.id.configure_verify ){
			Thread t = new Thread(){
	        	public void run(){
	        		
	        		SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
	        		
	        		Message msg = new Message();
					Bundle b = new Bundle();
					b.putString("message", "Connecting to webmail site. Please be patient. You will get a reply within 15 seconds.");
					b.putInt("ref", R.id.configure_verify);
					b.putString("type", "Button"); // EditView, etc
					msg.setData(b);
					mHandler.sendMessage(msg);
					
					if( mWebaddressSaved.charAt(mWebaddressSaved.length()-1) == '/' ){
						Editor pe = sharedPreferences.edit();
						String newaddress = mWebaddressSaved.subSequence(1, mWebaddressSaved.length()-2).toString();
						mWebaddress.setText(newaddress);
						pe.putString("webaddress", newaddress );
						pe.commit();
					}
					
					
	        		//*
			        SyncProcessing s = new SyncProcessing(ConfigureLogin.this);
			        s.setSharedPreferences(sharedPreferences);
			        String newText = "";
			    	String webaddress = sharedPreferences.contains("webaddress") ? sharedPreferences.getString("webaddress", "") : "";
			    	String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
			    	String password = sharedPreferences.contains("password") ? sharedPreferences.getString("password", "") : "";
			    	//String age = sharedPreferences.contains("age") ? sharedPreferences.getString("age", "") : "";
			    	
			    	Editor pe = sharedPreferences.edit();
			    	int status = s.owaLogin(TAG + " verifyThread 133", webaddress,login,password);
			    	// return 1:good 0:genericproblem -1:loginfailuremissingrefkey -2:badusernamepassword -3:missingvalues -4:wrongversion -5:goodversionbadparse -6:httpfailurehostnotresolved -7:generichttpfailure
			    	boolean validlogin = false;
			        if( status > 0 ){
			        	newText = "Healthy connection established. This is good, press close and search away.";
			        	
			        	msg = new Message();
						b = new Bundle();
						b.putInt("ref", R.id.configure_close);
						msg.setData(b);
						mHandlerFocus.sendMessage(msg);
						
			        	validlogin = true;
			        }else if( status == -9 ){
			        	newText = "Login reply from webmail was empty, which happened after correctly receiving the initial login page, maybe webmail site is down.";
			        }else if( status == -8 ){
			        	newText = "Login reply from webmail wasn't was was expected, which happened after correctly receiving the initial login page, maybe webmail site is down.";
			        }else if( status == -7 ){
			        	newText = "Generic Http Client Failure, maybe site is down.";
			        }else if( status == -6 ){
			        	newText = "Webmail site not reachable.  This comes up when the cellular internet access is down and is usually resolved in a few minutes.";
			        }else if( status == -5 ){
			        	newText = "Correct webmail version but I wasn't able to understand it.";
			        }else if( status == -4 ){
			        	newText = "Our webmail version test failed, email support havenskys@gmail.com.";
			        }else if( status == -3 ){
			        	newText = "One of the values is empty.";
			        }else if( status == -2 ){
			        	newText = "Login/Password incorrect.";
			        }else if( status == -1 ){
			        	newText = "Login failed, missing expected login result.";
			        }else{
			        	newText = "I had a problem connecting. " + status;
			        }
			        
			        msg = new Message();
					b = new Bundle();
					b.putString("message", newText);
					b.putInt("ref", R.id.configure_verify);
					b.putString("type", "Button"); // EditView, etc
					msg.setData(b);
					mHandler.sendMessage(msg);
					
					
					pe.putBoolean("validlogin", validlogin);
					pe.commit();
			        ///
	        	}
	        };
	        t.start();
		}
		//*/
	}

	private Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
	   //Log.i(TAG,"mToastHandler()");
	    	Bundle b = msg.getData();
	    	String type = b.containsKey("type") ? b.getString("type") : "";
	    	String message = b.containsKey("message") ? b.getString("message") : "";
	    	int ref = b.containsKey("ref") ? b.getInt("ref") : 0;
	    	if( ref == 0 || type == "" || message == ""){
	    		Log.w(TAG,"mHandler was missing an value.");
	    		return;
	    	}
	    	if( type == "TextView" ){
	    		TextView v = (TextView) ConfigureLogin.this.findViewById(ref);
	    		v.setText(message);
	    	}
	    	if( type == "Button" ){
	    		Button v = (Button) ConfigureLogin.this.findViewById(ref);
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
		msg.setData(b);
		mHandler.sendMessage(msg);
		//*/
	    
	};
	
	

	private Handler mHandlerFocus = new Handler() {
	    public void handleMessage(Message msg) {
	   //Log.i(TAG,"mToastHandler()");
	    	Bundle b = msg.getData();
	    	int ref = b.containsKey("ref") ? b.getInt("ref") : 0;
	    	if( ref == 0 ){
	    		Log.w(TAG,"mHandler was missing an value.");
	    		return;
	    	}
	    	
    		Button v = (Button) ConfigureLogin.this.findViewById(ref);
    		v.requestFocusFromTouch();
    	
	    	//int duration = b.containsKey("duration") ? b.getInt("duration") : Toast.LENGTH_SHORT;
	        //Toast.makeText(RSASMS.this, message, duration).show();
	    }
	    /*
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("message", "");
		b.putInt("ref", "");
		b.putString("type", "TextView"); // EditView, etc
		msg.setData(b);
		mHandler.sendMessage(msg);
		//*/
	    
	};
	
	
	public void afterTextChanged(Editable s) {
		Log.w(TAG,"afterTextChanged() " + s.toString() );
		
		mPreferencesEditor = mSharedPreferences.edit();
		
		if( mWebaddress.hasFocus() ){
			mWebaddressSaved = mWebaddress.getText().toString().trim();
			if( mWebaddressSaved != "http://"){
				mPreferencesEditor.putString("webaddress", mWebaddressSaved);
			}
		}
		else if( mLogin.hasFocus() ) {
			mLoginSaved = mLogin.getText().toString().trim();
			mPreferencesEditor.putString("login", mLoginSaved);
		}
		else if( mPassword.hasFocus() ){
			mPasswordSaved = mPassword.getText().toString().trim();
			mPreferencesEditor.putString("password", mPasswordSaved);
		}
		//else if( mAge.hasFocus() ){
			//mAgeSaved = Integer.parseInt(mAge.getText().toString());
			//mPreferencesEditor.putInt("age", mAgeSaved);
		//}
		
		mPreferencesEditor.commit();

	}
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		Log.w(TAG,"beforeTextChanged() " + s.toString() );
		
	}
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		Log.w(TAG,"onTextChanged() " + s.toString() + " start("+start+") before("+before+") count("+count+")" );
	}

	
	public void onFocusChange(View v, boolean hasFocus) {
		
		if(hasFocus){
			
			switch(v.getId()){
			case R.id.configure_url:
				if( mWebaddressSaved == "" ){
					mWebaddress.setText("https://");
		    		mWebaddress.setSelection(mWebaddress.length(),mWebaddress.length());
					mWebaddress.setTextColor(Color.rgb(0, 50, 100));
				}
				break;
			case R.id.configure_login:
				if( mLoginSaved == "" ){
					if( mLogin.length() > 3 ){
						mLogin.setText("");
					}
					mLogin.setTextColor(Color.rgb(0, 50, 100));
				}
				break;
			case R.id.configure_password:
				if( mPasswordSaved == "" ){
					if( mPassword.length() > 3 ){
						mPassword.setText("");
					}
					mPassword.setTextColor(Color.rgb(0, 50, 100));
				}
				break;
			}
			//*/
		}else{
			switch(v.getId()){
			case R.id.configure_url:
				if( mWebaddressSaved.length() > 0 ){
					if( mWebaddressSaved.charAt(mWebaddressSaved.length()-1) == '/' && mWebaddressSaved != "http://" ){
						SharedPreferences sp = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
						Editor pe = sp.edit();
						String newaddress = mWebaddressSaved.subSequence(0, mWebaddressSaved.length()-1).toString();
						mWebaddress.setText(newaddress);
						pe.putString("webaddress", newaddress );
						pe.commit();
					}
				}
				break;
			}
		}
		
	}

	public void onCheckedChanged(CompoundButton toggle, boolean isChecked) {
		SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
		Editor pe = sharedPreferences.edit();
		
		switch( toggle.getId() ){
		case R.id.configure_syncandroid:
			pe.putBoolean("syncandroid", isChecked );
		}
		
		pe.commit();
	}
}


