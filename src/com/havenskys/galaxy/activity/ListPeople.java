package com.havenskys.galaxy.activity;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.havenskys.galaxy.Custom;
import com.havenskys.galaxy.R;

public class ListPeople extends ListActivity implements Runnable {

	private static String TAG = "ListPeople";
	private static String CATEGORY = "people";
	private static int LAYOUT = R.layout.listpeople;
	private static int TOUCHPAD = R.id.listview_footer;
	private static int CONSOLETEXT = R.id.listview_text;
	
	private Handler mHandler;
	private Thread mThread;
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	private Custom mLog;
	private ListView mListView;
	private ContentResolver mResolver;
	private Context mContext;
	private Drawable mBackground;
	private Handler mBackgroundChange;
	private long mLastFocus = -1;
	private long mLastFocusStart = 0;
	private RelativeLayout mFocusView;
	private int mFocusPosition = -1;
	private int mScrollState = 0;
	private int mMidButtonWidth, mLeftButtonEnd, mRightButtonStart;
	private TextView mConsoleText;
	private ImageView mConsoleTouch;
	private long mActionEventTime = 0;
	private long mActionTouchEventTime = 0;
	private long mFocusId;
	private int TRACKBALL_ACTION_SPACE = 300;//ms
	private int mActionLastDirection = -1;
	private String mYesterday, mToday;
	private int mWidth, mHeight;
	
	public void run() {
		mLog.refreshConsoleTouch(mConsoleTouch, mHandler);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(LAYOUT);
	  
	  
	  mLog = new Custom(this, TAG + " onCreate() 115");
	  mLog.i(TAG,"onCreate() ++++++++++++++++++++");
	  //mListView = getListView();
	  mListView = (ListView) findViewById(android.R.id.list);
	  mResolver = this.getContentResolver();
	  mContext = this;
	  mSharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
	  mPreferencesEditor = mSharedPreferences.edit();
	  mLog.setSharedPreferences(mSharedPreferences, mPreferencesEditor);
	  //int count = mSharedPreferences.getInt("count",0);mPreferencesEditor.putInt("count", 1).commit();
	    
      mConsoleText = (TextView) findViewById(CONSOLETEXT);
      mConsoleTouch = (ImageView) findViewById(TOUCHPAD);
      mConsoleTouch.setFocusable(false);
      mConsoleTouch.setFocusableInTouchMode(false);
      
      mBackground = this.getResources().getDrawable(R.drawable.galaxybk);
      mListView.setBackgroundDrawable(mBackground);
      mBackgroundChange = new Handler(){
    	  public void handleMessage(Message msg) { 
	  			if( getListView().getBackground() == null && mScrollState == 0 ){
	  				mLog.w(TAG, "mBackgroundChange Thread, setting background mScrollState("+mScrollState+")");
	  				//getListView().setBackgroundResource(R.drawable.starbright);
	  				getListView().setBackgroundDrawable(mBackground);
	  				//mBackground = null;
	  			//}else if(b == null && mScrollState > 0){
	  				//mLog.w(TAG, "mBackgroundChange Thread, checking again in a while mScrollState("+mScrollState+")");
	  				//mBackgroundChange.sendEmptyMessageDelayed(16, 1880);
	  			}else{
	  				mLog.w(TAG, "mBackgroundChange Thread, doing nothing mScrollState("+mScrollState+")");
	  				//getListView().setBackgroundDrawable(null);
	  			}
        	}
        };
      
      
      mHandler = new Handler();
      
      mThread = new Thread(null, this, "touchconsole_runnable");
      mThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){

		public void uncaughtException(Thread thread, Throwable ex) {
			// TODO Auto-generated method stub
			mLog.e(TAG, "mThread uncaughtException() " + ex.getMessage() );
			//mThread.start();
		}
    	  
      });
      
      
      mConsoleText.setText("");
	  //final int totalF = total;
	  //mConsoleText.postDelayed(new Runnable(){ public void run() { mConsoleText.setText(totalF + " Records"); } }, 10 * 1000);
	  
	  //if(total > 0 ){
		  //mConsoleText.postDelayed(new Runnable(){ public void run() { mConsoleText.setText("Touch Here"); } }, 5 * 1000);
		  //mConsoleText.postDelayed(new Runnable(){ public void run() { mConsoleText.setText(""); } }, 10 * 1000);
	  //}
      
      mConsoleTouch.setOnTouchListener(new OnTouchListener(){

  		public boolean onTouch(View v, MotionEvent event) {

  			if(event.getAction() == MotionEvent.ACTION_UP){
  				mActionTouchEventTime = 0;
  				return true;
  			}
  			if(event.getAction() == MotionEvent.ACTION_CANCEL){
  				mActionTouchEventTime = 0;
  				return true;
  			}
  			
  			if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE){
  				
  				if( (System.currentTimeMillis() - mActionTouchEventTime) < (1880*2) ){ // Future configurable, scroll rate
  					return true;
  				}
  				mActionTouchEventTime = System.currentTimeMillis();
  				
  				
  				
  				int pos = getListView().getSelectedItemPosition();
  				int firstVis = getListView().getFirstVisiblePosition();
  				int top = getListView().getFirstVisiblePosition();
  				int cnt = getListView().getCount();
  				String direction = "way";
  				int origpos = pos;
  				
  				/*/
  				if( pos == -1 ){
  					getListView().requestFocusFromTouch();
  					pos = getListView().getSelectedItemPosition();
  					if( pos > -1 ){
  						mLog.w(TAG,"Console Touch acquired position after requesting touch");
  					}
  				}//*/
  				
  				//mLog.w(TAG,"Console Touch");
  				
  				if( pos == -1 && mFocusPosition > -1 ){
  					//mLog.w(TAG,"Console Touch changing pos("+pos+") to known mFocusPosition("+mFocusPosition+")");
  					pos = mFocusPosition;
  				}
  				
  				if( event.getX() > mRightButtonStart ){
  					direction = "down";// POS UP
  					if(pos == -1){
  						//mLog.w(TAG,"Console Touch changing pos("+pos+") to firstVis("+firstVis+")");
  						pos = firstVis;// yes, plus 2 in total
  					}else{
  						//mLog.w(TAG,"Console Touch changing pos("+pos+") to upper("+(pos + 1)+")");
  						pos += 1;
  					}
  					if( pos >= cnt ){
  						//mLog.w(TAG,"Console Touch changing pos("+pos+") to cnt("+(cnt-1)+")");
  						pos = cnt-1;
  					}
  				}else if(event.getX() < mLeftButtonEnd){
  					direction = "up";// POS DOWN
  					if(pos == -1){
  						//mLog.w(TAG,"Console Touch changing pos("+pos+") to firstVis("+firstVis+")");
  						pos = firstVis;// yes, odd, nullified
  					}else{
  						//mLog.w(TAG,"Console Touch changing pos("+pos+") to lower("+(pos - 1)+")");
  						pos -= 1;
  					}
  					if( pos < 0 ){
  						//mLog.w(TAG,"Console Touch droping focus");
  						getListView().clearFocus();
  						//getListView().requestFocus(View.FOCUS_UP);
  						setTitle("");
  						return true;
  					}
  				}else if(event.getX() > mLeftButtonEnd && event.getX() < mRightButtonStart ){
  					direction = "action";
  					getListView().setSelection(pos);
  					mPreferencesEditor.putInt("position", pos);
  					mPreferencesEditor.putLong("id", mFocusId);
  					mPreferencesEditor.commit();
  					setTitle(CATEGORY + ":" + mFocusId);
  					return true;
  				}
  				
  				getListView().requestFocusFromTouch();
  				getListView().setSelected(true);
  				
  				int fromTop = -1;
  				int fromTopHeight = -1;
  				if( pos > -1 ){
  					try {
  						View cv = getListView().getChildAt(pos);
  						fromTop = cv.getTop();
  						fromTopHeight = cv.getHeight();
  						if( (fromTopHeight + fromTop) < (fromTopHeight * .6) ){
  							mLog.w(TAG, "ConsoleTouch top entry is hidden");
  							if( direction == "up" ){
  								if( pos > 0 ){
  									pos --;
  								}
  							}else{
  								if( pos < cnt-1 ){
  									pos ++;
  								}
  							}
  						}
  					} catch (NullPointerException e){
  						mLog.e(TAG, "NullPointerException fromTop("+fromTop+") fromTopHeight("+fromTopHeight+") " + e.getLocalizedMessage() );
  					}
  				}
  				
  				//mLog.w(TAG, "ConsoleTouch original("+origpos+") pos("+pos+") Y("+fromTop+") H("+fromTopHeight+") vis("+(fromTopHeight + fromTop)+") limit("+(fromTopHeight * .6)+") top("+top+") direction("+direction+") firstVis("+firstVis+") cnt("+cnt+")");
  				
  				//getListView().getChildAt(pos).requestFocusFromTouch();
  				
  				
  				//getListView().setSelection(pos);
  				if( fromTop < -1 ){
  					getListView().setSelection(pos);
  				} else {
  					getListView().setSelectionFromTop(pos, 50);
  				}
  				
  				/*/
  				mRowId = getListView().getItemIdAtPosition(pos);
  				if( mRowId > -1 ){
  					Bundle b = new Bundle();
  			    	b.putLong("id", mRowId);
  			    	Message m = new Message();
  					m.setData(b);
  					mUpdateSeenHandler.sendMessageDelayed(m,100);
  				}//*/
  				
  				
  				//mLog.w(TAG, "onTouch() pos("+pos+") top("+top+") cnt("+cnt+") X("+event.getX()+") Y("+event.getY()+") ");

  			}
  			return true;
  			
  		}
  		  
  	  });
      
      
      mListView.setOnFocusChangeListener(new OnFocusChangeListener(){

			public void onFocusChange(View child, boolean hasFocus) {
				if( true ){return;}
				mLog.i(TAG, "onFocusChange() tag("+ mListView.getItemIdAtPosition(child.getId()) +") getId("+child.getId()+") hasFocus("+hasFocus+") ");
				
				//mFocusView = (RelativeLayout) child;
				//mTitle = (TextView) mFocusView.getChildAt(4);
				if( hasFocus ){
					//mLastFocusView.setTextColor(Color.argb(255, 250, 150, 25));
					//View c = child;
					//child.requestFocusFromTouch();
					//mListView.setSelected(true);
					//mListView.requestFocusFromTouch();
					//RelativeLayout r = (RelativeLayout) child.get;
					//mTitle = (TextView) r.getChildAt(4);
					//mTitle.setTextColor(Color.argb(255, 250, 150, 25));
					//mLastFocusView = mTitle;
					//if( mTitle != null ){
						//mTitle.setTextColor(Color.argb(255, 250, 150, 25));
					//}
					
					//RelativeLayout r = (RelativeLayout) getListView().findFocus();
					//mTitle = (TextView) r.getChildAt(4);
					//mTitle.setTextColor(Color.argb(255, 250, 150, 25));
					//mLastFocusView = mTitle;
					
					
					//if( getSelectedItemId() == mLastFocus ){
						//mTitle.setTextColor(Color.argb(255, 250, 150, 25));
					//}
				}else{

					
					//if( mTitle != null ){
						//mTitle = null;
						//mLastFocus = mLastFocus;
						//mTitle.setTextColor(Color.argb(255, 250, 250, 250));
					//}
					
						
				}
				
				
				//child.setTag("set");
				
				/*
				child.setOnFocusChangeListener(new OnFocusChangeListener(){
					public void onFocusChange(View v, boolean hasFocus) {
						if( !hasFocus ){
							RelativeLayout rl = (RelativeLayout) v;
							TextView tView = (TextView) rl.getChildAt(4);
							tView.setTextColor(Color.argb(255, 250, 250, 250));
						}
					}
				});/*/
				
				//tView.setTextColor(Color.argb(255, 250, 150, 25));
					
				//mBundleSwap.putLong("id", mLastFocus);
				//mMsgSwap.setData(mBundleSwap);
				//mUpdateSelection.sendMessage(mMsgSwap);
				
				/*
				if( !hasFocus && mLastFocusView != null ){
					if( !mLastFocusView.hasFocus() ){
						mTitle = (TextView) mLastFocusView.getChildAt(4);
						mTitle.setTextColor(Color.argb(200, 250, 250, 250));
					}
					//mSummary = (TextView) mLastFocusView.getChildAt(5);
					//mSummary.setTextColor(Color.argb(255, 250, 250, 250));
					
					//mSummary = (TextView) mLastFocusView.getChildAt(1);
					//mSummary.setTextColor(Color.argb(255, 250, 250, 250));
					//mSummary = (TextView) mLastFocusView.getChildAt(2);
					//mSummary.setTextColor(Color.argb(255, 250, 250, 250));
					//mSummary = (TextView) mLastFocusView.getChildAt(3);
					//mSummary.setTextColor(Color.argb(255, 250, 250, 250));
				}	
				if( hasFocus && mLastFocusView != null ){
					if( mLastFocusView.hasFocus() ){
						mTitle = (TextView) mLastFocusView.getChildAt(4);
						mTitle.setTextColor(Color.argb(200, 250, 150, 25));
					}
				}//*/
			}
	  		
	  	});
	  	
	  	
	  	mListView.setOnScrollListener(new OnScrollListener(){

			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if( true ){return;}
				mLog.i(TAG, "onScrollStateChanged() scroll("+mScrollState+") topid("+getListView().getItemIdAtPosition(firstVisibleItem)+") firstVisibleItem("+firstVisibleItem+") visibleItemCount("+visibleItemCount+") totalItemCount("+totalItemCount+")");

				if( mScrollState > 0 ){
					//getListView().setSelected(false);
					//mFocusPosition = -1;
					//mFocusId = -1;
				}
				/*
				long diff = (System.currentTimeMillis() - mVisibleTimer);
				if( diff >= 1880 && diff < 10000 ){
					mLog.w(TAG,"onScrollStateChanged() seen top 0("+mVisibleList[0]+") 1("+mVisibleList[1]+") 2("+mVisibleList[2]+")");
					Bundle b = new Bundle();
			    	b.putLong("id", mVisibleList[0]);
			    	b.putLong("id1", mVisibleList[1]);
			    	b.putLong("id2", mVisibleList[2]);
			    	Message m = new Message();
					m.setData(b);
					mUpdateSeenHandler.sendMessageDelayed(m,100);
				}
				
				if( mScrollState > 0 ){
					//rowidView = (TextView) childView.getChildAt(9);
					//rowid = Long.parseLong(rowidView.getText().toString());
					//mLog.w(TAG,"onChildViewAdded() seen rowid("+rowid+")");
					if( visibleItemCount > 3 ){
						mVisibleList[2] = getListView().getItemIdAtPosition(firstVisibleItem);
						mVisibleList[1] = getListView().getItemIdAtPosition(firstVisibleItem-1);
						mVisibleList[0] = getListView().getItemIdAtPosition(firstVisibleItem-2);
					}
				}else{
					mVisibleList[2] = -1;
				}
				//*/
				
				
				/*
				if( mScrollState > 0 ){
					if(mVisibleList[0] > -1 || mVisibleList[1] > -1 || mVisibleList[2] > -1){
						long diff = (System.currentTimeMillis() - mVisibleTimer);
						if( diff >= 1880 && diff < 10000 ){
							mLog.i(TAG, "onScrollStateChanged() setting seen("+mVisibleList[0]+","+mVisibleList[1]+") ago("+(diff/100)+" deciseconds) ");
							//if(mVisibleList[0] > -1)
							{
								Bundle b = new Bundle();
						    	b.putLong("id", mVisibleList[0]);
						    	b.putLong("id1", mVisibleList[1]);
						    	b.putLong("id2", mVisibleList[2]);
						    	Message m = new Message();
								m.setData(b);
								mUpdateSeenHandler.sendMessageDelayed(m,300);
							}
						}
					}
				}//*/
				
				
				//if( mTitle != null ){
					//mTitle.setTextColor(Color.argb(255, 250, 250, 250));
					//mLastFocusView = null;
				//}
			}

			public void onScrollStateChanged(AbsListView view, int scrollState) {
				//if( true ){return;}
				//mLog.i(TAG, "onScrollStateChanged() state("+scrollState+") ");
				mScrollState = scrollState;
				
				if( scrollState > 0 ){
					getListView().setBackgroundDrawable(null);
					mLog.i(TAG, "onScrollStateChanged() state("+scrollState+") clearing background");
				}else{
					if( getListView().getBackground() == null ){
						mBackgroundChange.sendEmptyMessageDelayed(17, 1880 );
					}
					
					//mLog.i(TAG, "onScrollStateChanged() state("+scrollState+") setting background");
				}
			}
	  		
	  	});
	  
	  	mListView.setOnItemSelectedListener(new OnItemSelectedListener(){

			public void onItemSelected(AdapterView<?> arg0, View child, int position, long itemid) {
				//if( true ){return;}
				//mLog.i(TAG, "onItemSelected() position("+position+") itemid("+itemid+") ");
				if( child != null ){ child.requestFocusFromTouch(); }
				mFocusPosition = position;
				mFocusId = itemid;
				
				if( mLastFocus != itemid ){
					//int cnt = getListView().getCount();
					//if( position == (cnt -1) ){
						//mRowStart = 1;
						//mRowCount = mRowCount + 10;
						//mLog.loadlist(listView.this,mRowStart,mRowCount);
						//child.setPadding(0, 0, 0, 80);
					//}
				}
				
				mLastFocus = itemid;
				mLastFocusStart = System.currentTimeMillis();
				//mLastFocusView = mTitle;
				
				//if( mListView.getSelectedItemPosition() == position ){
					
				//}
				
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				//mLog.i(TAG, "onItemSelected() nothing selected ");
				mLastFocus = -1;
				mLastFocusStart = System.currentTimeMillis();
				
				//getListView().setSelected(false);
				mFocusPosition = -1;
				mFocusId = -1;
			}
	  		
	  	});
	  	
	  	
		mListView.setOnHierarchyChangeListener(new OnHierarchyChangeListener(){
	
			public void onChildViewAdded(View parent, View child) {
				
				/*/
				String tag = child.getTag().toString();
				if( tag == null ){
					mLog.e(TAG,"tag is empty");
				}else{
					if( tag == "footer" ){
						return;
					}
				}//*/
				
				//*
				try {
					//childView = (RelativeLayout) child;
				} catch (ClassCastException e){
					mLog.w(TAG, "Failed to create childView, probably a header or footer.");
					//e.printStackTrace();
					return;
				}

				
			}
	
			public void onChildViewRemoved(View parent, View child) {
				
			}
			
		});
      
      
      
      
      
      
	}
	
	
	@Override
	protected void onStart() {
		mLog.i(TAG,"onStart() ++++++++++++++++++++");
		super.onStart();
		
		//mConsoleTouchReset = true;
		
		if( mThread.isAlive() ){
			mLog.i(TAG, "onStart() verified mThread is alive");
		}else{
			mLog.w(TAG, "onStart() is starting mThread");
			try {
				mThread.start();
			} catch (IllegalThreadStateException e){
				mLog.w(TAG,"onStart() mThread already started.");
			}
		}

		//mHandler.postDelayed(this, 100);
		//mHandler.post(this);
		//mHandler.postDelayed(this, 10 * 1000);
		//Thread thr = new Thread(null, this, "touchconsole_runnable");
	    //thr.start();
		
		
		
		long id = mSharedPreferences.contains("id") ? mSharedPreferences.getLong("id",-1) : -1;
		mLog.loadlist(this);
		//loadRecord(id);
		
		Date d = new Date();
		mToday = (d.getYear() + 1900) + "-";
		mToday += ( d.getMonth() < 10 ) ? "0" + (d.getMonth()+1) : ""+ (d.getMonth()+1);
		mToday += ( d.getDate() < 10 ) ? "-0" + d.getDate() : "-" + d.getDate();
		
		d.setTime(System.currentTimeMillis() - (24 * 60 * 60 * 1000) );
		mYesterday = (d.getYear() + 1900) + "-";
		mYesterday += ( d.getMonth() < 9 ) ? "0" + (d.getMonth()+1) : ""+ (d.getMonth()+1);
		mYesterday += ( d.getDate() < 10 ) ? "-0" + d.getDate() : "-" + d.getDate();
		
		Display display = getWindowManager().getDefaultDisplay();

		mWidth = display.getWidth();
		mHeight = display.getHeight();
		
		mMidButtonWidth = (int) (mWidth * .23);
		mLeftButtonEnd = (mWidth/2) - (mMidButtonWidth/2);
		mRightButtonStart = mLeftButtonEnd + mMidButtonWidth;
		mLog.w(TAG,"Button Boundaries left("+mLeftButtonEnd+") midsize("+mMidButtonWidth+") right("+mRightButtonStart+") ");
				
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		mPreferencesEditor.putString("category", CATEGORY);
		mPreferencesEditor.putLong("id", id);
		mPreferencesEditor.commit();
		setTitle(CATEGORY+":"+id);
		//mListView.setSelector(mItemClickDrawable);
	}
	
	@Override
	public boolean dispatchTrackballEvent(MotionEvent ev) {
		
		// MOVEMENT, UP or DOWN in to mode
		if( ev.getAction() == MotionEvent.ACTION_MOVE && ev.getY() != 0 ){
			
			int direction = -1;
			if( ev.getY() > 1 ){
				direction = 1; //pos -
			}else if( ev.getY() < 1 ){
				direction = 2; //pos -
			}
			int pos = getListView().getSelectedItemPosition();
		
			if( pos == 0 && direction == 2){
				//ev.setAction(MotionEvent.ACTION_MOVE);
				//listView.this.setTitle("");
				return super.dispatchTrackballEvent(ev);
			}
			
			if( (System.currentTimeMillis() - mActionEventTime) < TRACKBALL_ACTION_SPACE){// && direction == mActionLastDirection){
				ev.setAction(MotionEvent.ACTION_CANCEL);
				super.dispatchTrackballEvent(ev);
				return true;
				//Log.w(TAG,"Canceling Trackball motion("+(ev.getEventTime() - mActionEventTime)+") < " + TRACKBALL_ACTION_SPACE);
				//ev.setAction(ev.ACTION_CANCEL);
				//return super.dispatchTrackballEvent(ev);
			}
			
			mActionLastDirection = direction;
			//mUpdateSeenHandler.sendEmptyMessage(0);
			
			//mLastActionEventTime = mActionEventTime;
			mActionEventTime = System.currentTimeMillis();			
			
			
			int top = getListView().getFirstVisiblePosition();
			int cnt = getListView().getCount();
			int pag = getListView().getChildCount();
			long rowid = -1;
			
			//mLog.w(TAG, "Trackball UP/DOWN direction("+direction+") pos("+pos+") top("+top+") cnt("+cnt+") pag("+pag+") rowid("+rowid+") X("+ev.getX()+") Y("+ev.getY()+") top("+getListView().getFirstVisiblePosition()+")");
			
			if( pos == AdapterView.INVALID_POSITION ){
				
				getListView().requestFocusFromTouch();
				getListView().setSelection(top);
				rowid = getListView().getItemIdAtPosition(top);
				
				if( rowid < -1 ){
					return false;
				}
				
				
				//Bundle b = new Bundle();
		    	//b.putLong("id", rowid);
		    	//Message m = new Message();
				//m.setData(b);
				//mUpdateSeenHandler.sendMessageDelayed(m,100);
				
				return false;
			}
			//else if(top < pos ){
				//getListView().setSelection(top);
				//return true;
			//}
			
			if( pos < 0 ){ pos = 0; }
			if( pos >= cnt ){
				pos = cnt-1;
			}
			
			//if( true ){
				//return super.dispatchTrackballEvent(ev);
			//}
			
			
			
			
			/*
			// UP
			if( ev.getY() < 0 && (ev.getY() * -1) > 1.3  ){ // UP -0.16_7, -0.3_, -0.5, -0.6_, ???
				//.3 page turn
				//.5 glide page turn
				return super.dispatchTrackballEvent(ev);
			}
			// DOWN
			if( ev.getY() > 0 && ev.getY() > 1.3  ){ // UP -0.16_7, -0.3_, -0.5, -0.6_, ???
				//.3 page turn
				//.5 glide page turn
				return super.dispatchTrackballEvent(ev);
			}//*/

			
			
			// DOWN
			if( ev.getY() > 1  ){ // UP -0.16_7, -0.3_, -0.5, -0.6_, ???
				pos += pag - 1;
				if(pos > cnt ){pos = cnt-1;}
				getListView().setSelectionFromTop(pos, 50);
				//mLog.w(TAG, "Trackball PAGE DOWN pos("+pos+") X("+ev.getX()+") Y("+ev.getY()+") top("+getListView().getFirstVisiblePosition()+")");
			}
			// UP
			else if( ev.getY() < -1  ){ // UP -0.16_7, -0.3_, -0.5, -0.6_, ???
				pos -= pag - 1;
				if(pos < 0){pos = 0;}
				if(pos > 1){
					getListView().setSelectionFromTop(pos, 50);
				}else{
					getListView().setSelectionFromTop(pos, 0);
				}
			}
			
			
			// DOWN
			if( ev.getY() > .1  ){ // UP -0.16_7, -0.3_, -0.5, -0.6_, ???
				//.3 page turn
				//.5 glide page turn
				//int pos = getListView().getSelectedItemPosition();
				//if( pos > 0 ){
				pos += 1;
				getListView().setSelectionFromTop(pos, 50);
				
			}
			// UP
			else if( (ev.getY() * -1) > .1  ){ // UP -0.16_7, -0.3_, -0.5, -0.6_, ???
				//.3 page turn
				//.5 glide page turn
				//int pos = getListView().getSelectedItemPosition();
				pos -= 1;
				if(pos > -1){
					//if( pos > 1 ){
						getListView().setSelectionFromTop(pos, 50);
					//}
					//getListView().setSelection(pos-1);
				}else{
					ev.setAction(MotionEvent.ACTION_MOVE);
					super.dispatchTrackballEvent(ev);
					return false;
				}
			}
			
			//rowid = getListView().getItemIdAtPosition(pos);
			//Bundle b = new Bundle();
	    	//b.putLong("id", rowid);
	    	//Message m = new Message();
			//m.setData(b);
			//mUpdateSeenHandler.sendMessageDelayed(m,100);
			
			return true;
				
			
		} else if(ev.getAction() == ev.ACTION_MOVE && ev.getX() != 0 && ev.getY() != 0){
			// Diaganal Movement
			//mLog.w(TAG, "Trackball DIAG X("+ev.getX()+") Y("+ev.getY()+") top("+getListView().getFirstVisiblePosition()+")");
			ev.setAction(ev.ACTION_CANCEL);
		} else if(ev.getAction() == ev.ACTION_MOVE && ev.getX() != 0){
			// LEFT RIGHT Movement
			//mLog.w(TAG, "Trackball RIGHT/LEFT X("+ev.getX()+") Y("+ev.getY()+") top("+getListView().getFirstVisiblePosition()+")");
			ev.setAction(ev.ACTION_CANCEL);
		}
		
		
		return super.dispatchTrackballEvent(ev);
	}
	
}
