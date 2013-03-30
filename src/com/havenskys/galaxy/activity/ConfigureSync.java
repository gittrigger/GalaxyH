package com.havenskys.galaxy.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.havenskys.galaxy.R;

public class ConfigureSync extends Activity implements TextWatcher, OnClickListener, OnCheckedChangeListener {

	private final static String TAG = "ConfigureSync";
	private int mReviewSaved;
	private Boolean mSyncAndroidSaved;
	private EditText mReview;
	private TextView mSyncAndroidTitle;
	private Button mClose;
	private ToggleButton mSyncAndroid;
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.configure_sync);

        mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
		mPreferencesEditor = mSharedPreferences.edit();
		
		mSyncAndroidSaved = mSharedPreferences.contains("syncandroid") ? mSharedPreferences.getBoolean("syncandroid",false) : false;
		mSyncAndroid = (ToggleButton) findViewById(R.id.configure_syncandroid);
		mSyncAndroid.setOnCheckedChangeListener(this);
    	mSyncAndroid.setChecked(mSyncAndroidSaved);
    	
    	mReview = (EditText) findViewById(R.id.configure_syncreview);
    	mReview.addTextChangedListener(this);
    	mReviewSaved = mSharedPreferences.contains("syncreview") ? mSharedPreferences.getInt("syncreview",5) : 5;
    	if( mReviewSaved > 0 ){
    		mReview.setText(Integer.toString(mReviewSaved));
    	}
    	
    	mClose = (Button) findViewById(R.id.configure_close);
    	mClose.setOnClickListener(this);
    	
    	
    	//SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
		//Editor preferencesEditor = sharedPreferences.edit();
		
		String webaddress = mSharedPreferences.contains("webaddress") ? mSharedPreferences.getString("webaddress", "") : "";
    	String login = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
    	String username = login.replaceAll(".*?/", "").replaceAll(".*?\\\\", "");

    	mSyncAndroidTitle = (TextView) findViewById(R.id.configure_syncrandroidtitle);
    	if( login.length() > 0 ){
    		mSyncAndroidTitle.setText("Syncronization of contacts from your personal database on the webmail service to this Android device. Contacts are grouped as 'Galaxy: "+username+"'");
    	}else{
    		
    	}
    	
	}

	public void onClick(View v) {
		
		if( v.getId() == R.id.configure_close ){
			this.finish();
		}
		//*/
	}

	public void afterTextChanged(Editable s) {
		Log.w(TAG,"afterTextChanged() " + s.toString() );
		
		if( mReview.hasFocus() ){
			mReviewSaved = Integer.parseInt(mReview.getText().toString());
			if( mReviewSaved <= 0 ){
				mReviewSaved = 1;
				mReview.setText(mReviewSaved);
			}
			//mPreferencesEditor.putLong("last_personaldownload", 0 );
			mPreferencesEditor.commit();
			mPreferencesEditor.putInt("syncreview", mReviewSaved);
		}
		
		mPreferencesEditor.commit();

	}
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		Log.w(TAG,"beforeTextChanged() " + s.toString() );
		
	}
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		Log.w(TAG,"onTextChanged() " + s.toString() + " start("+start+") before("+before+") count("+count+")" );
	}

	public void onCheckedChanged(CompoundButton toggle, boolean isChecked) {
		SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
		Editor pe = sharedPreferences.edit();
		
		switch( toggle.getId() ){
		case R.id.configure_syncandroid:
			pe.putBoolean("syncandroid", isChecked );
			pe.putLong("last_personaldownload", 0 );
			pe.commit();
		}
		
		pe.commit();
	}
}


