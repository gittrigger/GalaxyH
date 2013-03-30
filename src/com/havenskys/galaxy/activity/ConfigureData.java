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
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.havenskys.galaxy.R;

public class ConfigureData extends Activity implements TextWatcher, OnClickListener, OnCheckedChangeListener {

	private final static String TAG = "ConfigureData";
	private int mAgeSaved;
	private EditText mAge;
	private Button mClose;
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG,"onCreate() 38");
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.configure_data);
		
        mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
        
    	mAge = (EditText) findViewById(R.id.configure_age);
    	mAge.addTextChangedListener(this);
    	mAgeSaved = mSharedPreferences.contains("age") ? mSharedPreferences.getInt("age",5) : 5;
    	if( mAgeSaved > 0 ){
    		mAge.setText(Integer.toString(mAgeSaved));
    	}
    	mClose = (Button) findViewById(R.id.configure_close);
    	mClose.setOnClickListener(this);
	}

	public void onClick(View v) {
		
		if( v.getId() == R.id.configure_close ){
			this.finish();
		}

	}

	public void afterTextChanged(Editable s) {
		Log.w(TAG,"afterTextChanged() " + s.toString() );
		
		mPreferencesEditor = mSharedPreferences.edit();
		
		if( mAge.hasFocus() ){
			mAgeSaved = Integer.parseInt(mAge.getText().toString());
			mPreferencesEditor.putInt("age", mAgeSaved);
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
		//case R.id.configure_syncandroid: pe.putBoolean("syncandroid", isChecked ); break;
		}
		
		pe.commit();
	}
}


