package com.havenskys.galaxy.activity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.havenskys.galaxy.SqliteWrapper;
import com.havenskys.galaxy.SyncProcessing;

public class NightWorker extends Service {

	private static final String TAG = "NightWorker";
	private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    
    
    /* ****************************
     * Service Handler
     */
    private final class ServiceHandler extends Handler {
	    	
    	//private DbAdapter mDataStore;
    	private SyncProcessing mSyncProcessing;
		 
    	public ServiceHandler(Looper looper) { super(looper); }
	
		public void handleMessage(Message msg) {
			Log.w(TAG,"ServiceHandler() handleMessage() +++++++++++++++++++++++++++++");
			
			int serviceId = msg.arg1;
			Intent intent = (Intent) msg.obj;
			String action = "";
			if( intent != null ){
				action = intent.getAction();
			}
			
			SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
			Editor preferencesEditor = sharedPreferences.edit();
			long lastDownload = sharedPreferences.contains("last_personaldownload") ? sharedPreferences.getLong("last_personaldownload", 0) : 0;
			long lastUpdate = sharedPreferences.contains("last_personalupdate") ? sharedPreferences.getLong("last_personalupdate", 0) : 0;
			long webServiceStatus = sharedPreferences.contains("status_webservice") ? sharedPreferences.getLong("last_webservice_status", 0) : 0;
			int syncReview = sharedPreferences.contains("syncreview") ? sharedPreferences.getInt("syncreview",5) : 5;
			boolean syncAndroid = sharedPreferences.contains("syncandroid") ? sharedPreferences.getBoolean("syncandroid",false) : false;
			
			
			//preferencesEditor.putLong("last_personaldownload", 0);
			preferencesEditor.putLong("last_webservice_status", System.currentTimeMillis());
			preferencesEditor.putLong("last_personalupdate", System.currentTimeMillis() );
			preferencesEditor.commit();
			
			String webaddress = sharedPreferences.contains("webaddress") ? sharedPreferences.getString("webaddress", "") : "";
	    	String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
	    	String password = sharedPreferences.contains("password") ? sharedPreferences.getString("password", "") : "";
	    	
	    	if( login.length() > 0 ){
	    		Log.w(TAG,"prepareContacts() 78 login exists");
	    	}else{
	    		Log.w(TAG,"prepareContacts() 80 login doesn't exists.");
	    	}
			
			verifyContacts(TAG + " ServiceHandler 89");
			
			//String[] columns = new String[] {"_id","phone","title","company","email","mobile","fname","lname","department","notes"};
			//int updateWait = 1;
			
			for(;;){
				SystemClock.sleep(1000);
		
				lastDownload = sharedPreferences.contains("last_personaldownload") ? sharedPreferences.getLong("last_personaldownload", 0) : 0;
				//lastUpdate = sharedPreferences.contains("last_personalupdate") ? sharedPreferences.getLong("last_personalupdate", 0) : 0;
				syncAndroid = sharedPreferences.contains("syncandroid") ? sharedPreferences.getBoolean("syncandroid",false) : false;
				syncReview = sharedPreferences.contains("syncreview") ? sharedPreferences.getInt("syncreview",5) : 5;
				
				if( syncAndroid ){
					if( lastDownload < (System.currentTimeMillis() - syncReview * 60 * 60 * 1000) ){
	
						syncAndroidContacts(TAG + " ServiceHandler 99");
						//updateWait = 1;
					}
					
					/*/
					if( lastUpdate < (System.currentTimeMillis() - updateWait * 60 * 1000) ){
						preferencesEditor.putLong("last_personalupdate", System.currentTimeMillis() );
						preferencesEditor.commit();
						if( mDataStore == null ){
							mDataStore = new DbAdapter(NightWorker.this);
							mDataStore.loadDb(TAG + " ServiceHandler 108", "contactStore");
						}
						
						
						login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
						String username = login.replaceAll(".*?/", "").replaceAll(".*?\\\\", "");
						
						
						Cursor updateme = null;
						updateme = mDataStore.getEntry("contactStore", columns , "type = \"IPM.Contact\"" );
						int updatecnt = 0;
						if( updateme != null ){
							if( updateme.moveToFirst() ){
								int len = updateme.getCount();
								for( int row = 0; row < len; row ++ ){
									updateme.moveToPosition(row);

									String phone = updateme.getString(1);
									String title = updateme.getString(2);
									String company = updateme.getString(3);
									String email = updateme.getString(4);
									String mobile = updateme.getString(5);
									String fname = updateme.getString(6);
									String lname = updateme.getString(7);
									String department = updateme.getString(8);
									String notes = updateme.getString(9);
									//String postal = "311 Bellevue Ave E. Apt 405 Seattle, WA 98102"
									
									if( mSyncProcessing.updateAddContact(TAG + " ServiceHandler 132", "Galaxy: "+ username, mobile, phone, email, fname, lname, title, department, company, notes, null) ){
										updatecnt++;
									}
									
								}
							}
							updateme.close();
						}
						if( updatecnt == 0 ){  updateWait++; }
						if( updateWait > 30 ){ updateWait = 30; }
						if( mDataStore != null ){
							mDataStore.close();
							mDataStore = null;
						}
					}
					//COL_TIMEUPDATE > (System.currentTimeMillis()
					//type = IPM.Contact
					//*/
					
					
				}
				//startWebService().start();
				
			}

		}

		private void syncAndroidContacts(String who) {
			
			Log.i(TAG,"syncAndroidContacts() 114 for " + who);
			
			SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_WRITEABLE);
			Editor preferencesEditor = sharedPreferences.edit();
			
			mSyncProcessing = new SyncProcessing(NightWorker.this);
			mSyncProcessing.setSharedPreferences(sharedPreferences);
			
			String webaddress = sharedPreferences.contains("webaddress") ? sharedPreferences.getString("webaddress", "") : "";
	    	String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
	    	String password = sharedPreferences.contains("password") ? sharedPreferences.getString("password", "") : "";
	    	int syncReview = sharedPreferences.contains("syncreview") ? sharedPreferences.getInt("syncreview",5) : 5;
	    	
			// Retry in 10 minutes if failure occurs
			preferencesEditor.putLong("last_personaldownload", (System.currentTimeMillis() - syncReview * 60 * 60 * 1000) + (10 * 60 * 1000) );
			preferencesEditor.commit();
			
			// DOWNLOAD CURRENT
			if( mSyncProcessing.owaLogin(TAG + " syncAndroidContacts() 129 for " + who, webaddress, login, password) > 0 ){
			
				//https://webmail.t-mobile.com/OWA/?ae=Folder&t=IPF.Contact
				//https://webmail.t-mobile.com/OWA/?ae=Folder&t=IPF.Contact&pg=2
				//<a href="#" id="lnkLstPg" onClick="return onClkPg('3');">				
				Log.i(TAG,"syncAndroidContacts() 134 goto owaLogin() for " + who);
				
				String status = mSyncProcessing.safeHttpGet(TAG + " syncAndroidContacts() 136 for " + who, new HttpGet(webaddress + "/OWA/?ae=Folder&t=IPF.Contact"));
				if( status.contains("200") ){
					Log.i(TAG,"syncAndroidContacts() 138 Personal Database Download Succeeded for " + who);
					
					String httpPage = mSyncProcessing.getHttpPage();
					String[] httpPageLines = httpPage.replaceAll("<td", "\n<td").split("\n");
					int lastPage = 1;
					for( int c = 0; c < httpPageLines.length; c++ ){
						
						if( httpPageLines[c].contains("lnkLstPg") ){
							String lastPage2 = httpPageLines[c].replaceFirst(".*lnkLstPg", "").replaceFirst(".*onClkPg..","").replaceFirst("'.*","");
							lastPage = Integer.parseInt(lastPage2);
							Log.w(TAG,"syncAndroidContacts() 147 httpPage lastPage("+lastPage2+") line("+httpPageLines[c]+")");	
						}else{
							//Log.i(TAG,"syncAndroidContacts() 141 httpPage line("+httpPageLines[c]+")");
						}
					}
					Log.i(TAG,"syncAndroidContacts() 141 httpPage length("+httpPage.length()+") lnkLstPg("+lastPage+")");
					
					
					mSyncProcessing.processContactSearch(TAG + " syncAndroidContacts() 156");
					//*/
					for( int getpage = 2; getpage <= lastPage; getpage++){
						if( mSyncProcessing.safeHttpGet(TAG + " syncAndroidContacts() 159", new HttpGet(webaddress + "/OWA/?ae=Folder&t=IPF.Contact&pg="+getpage)).contains("200") ){
							Log.w(TAG,"Successfully downloaded personal database page " + getpage);
							mSyncProcessing.processContactSearch(TAG + " syncAndroidContacts() 161");
						}else{
							Log.w(TAG,"syncAndroidContacts() 163 Error getting personal database page " + getpage);
						}
					}
					//*/
				}else{
					Log.e(TAG,"syncAndroidContacts() 155 Personal Database Download Failed for " + who);
				}
				
			}else{
				Log.e(TAG,"syncAndroidContacts() 158 owaLogin() Failed for " + who);
			}
			
			// TODO GET RECORDS INTO ARRAYS(3)
			
			//mSyncProcessing.updateAddContact(TAG + " verifyContacts() 218 for " + who, "Galaxy: "+ username, "206-555-1212", "206-555-2222", "galaxy@docchompssoftware.com", "Galaxy", "Person", "Cooperator", "Software", "Doc Chomps Software", "This is a test entry.", "311 Bellevue Ave E. Apt 405 Seattle, WA 98102");
			// 
			
			// Set real time
			preferencesEditor.putLong("last_personaldownload", System.currentTimeMillis() );
			preferencesEditor.commit();
			
			
		}

		private void verifyContacts(String who) {

			SharedPreferences sharedPreferences = getSharedPreferences("Preferences", MODE_WORLD_READABLE);
			//Editor preferencesEditor = sharedPreferences.edit();	
			//preferencesEditor.putLong("last_personaldownload", 0);
			//preferencesEditor.putLong("last_webservice_status", System.currentTimeMillis());
			//preferencesEditor.commit();
			
	    	String login = sharedPreferences.contains("login") ? sharedPreferences.getString("login", "") : "";
	    	
			mSyncProcessing = new SyncProcessing(NightWorker.this);
			
	    	//syncProcess.androidDataPrint("content://calendar/calendars");
	    	//syncProcess.androidDataPrint("content://contacts/people","number_key is not null AND last_time_contacted > " + ( (System.currentTimeMillis() - (90*24*60*60*1000))/1000) );
	    	//syncProcess.androidDataPrint("content://calendar/events","calendar_id != 2");
	    	//syncProcess.androidDataPrint("content://calendar/attendees","calendar_id != 2");
	    	//syncProcess.androidDataPrint("content://calendar/reminders");
	    	//syncProcess.androidDataPrint("content://calendar/calendar_alerts");
			//mSyncProcessing.androidDataPrint("content://contacts/organizations");
			//mSyncProcessing.androidDataPrint("content://contacts/phones");
			//mSyncProcessing.androidDataPrint("content://contacts/settings");
			//mSyncProcessing.androidDataPrint("content://contacts/groupmembership");
			
			//Object[][] recordid = mSyncProcessing.getAndroidData("content://contacts/groups", "_id", "name = \"Galaxy\"", null);
			
			if( login.length() > 0 ){
				Log.w(TAG,"prepareContacts() 156 login("+login+")");
				String username = login.replaceAll(".*?/", "").replaceAll(".*?\\\\", "");
				
				mSyncProcessing.updateAddAndroidContact(TAG + " verifyContacts() 218 for " + who, "Galaxy: "+ username, "206-555-1212", "206-555-2222", "galaxy@docchompssoftware.com", "Galaxy", "Person", "Cooperator", "Software", "Doc Chomps Software", "This is a test entry.", "311 Bellevue Ave E. Apt 405 Seattle, WA 98102");
				
				if( true ){ // Cleanup Groups
		    		Log.w(TAG,"updateAddContact() 1476 Cleaning up Groups for " + who);
		    		Object[][] data = mSyncProcessing.getAndroidData("content://contacts/groups","_id","name like \"Galaxy%\"",null);
		    		if( data != null ){
		    			Log.w(TAG,"updateAddContact() 1476 Found "+data.length+" entries for " + who);
		    			for( int row = 0; row < data.length; row++ ){
		    				long groupid = new Long( data[row][0].toString() );
		    				if( groupid > 0 ){
		    					Log.w(TAG,"updateAddContact() 1476 groupid("+groupid+") for " + who);
			    				Object[][] grouplist = mSyncProcessing.getAndroidData("content://contacts/groupmembership","_id","group_id = " + groupid,null);
			    				if( grouplist == null ){
			    					Log.w(TAG,"updateAddContact() 1482 Deleting unused Galaxy groupid("+groupid+") for " + who);
			    					SqliteWrapper.delete(NightWorker.this, NightWorker.this.getContentResolver(), Uri.parse("content://contacts/groups/"+groupid), null, null);
			    				}
		    				}else{
		    					Log.w(TAG,"updateAddContact() 1476 no groupid for " + who);
		    				}
		    			}
		    		}
		    	}
	
			}else{
				Log.w(TAG,"prepareContacts() 214 login doesn't exist.");
			}
			
			if( false ){
				//mSyncProcessing.androidDataPrint("content://contacts/groupmembership");
				mSyncProcessing.androidDataPrint("content://contacts/groups","name like \"Haven%\" OR name like \"Galaxy%\"");
				mSyncProcessing.androidDataPrint("content://contacts/organizations");
				mSyncProcessing.androidDataPrint("content://contacts/phones","name like \"Haven%\" OR name like \"Galaxy%\"");
				mSyncProcessing.androidDataPrint("content://contacts/people/raw","name like \"Haven%\" OR name like \"Galaxy%\"");
				//mSyncProcessing.androidDataPrint("content://contacts/contact_methods/email/raw","");
				mSyncProcessing.androidDataPrint("content://contacts/contact_methods/email","data like \"Haven%\" OR data like \"Galaxy%\"");
			}
		}

		public Thread startWebService() {
			
			Thread webServiceThread = new Thread(){
				public void run(){
			        Log.w(TAG,"Starting web service. ==============================");
			        //if( true ){ return; }
					CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
					CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
					ByteBuffer buffer = ByteBuffer.allocate(512);
					Selector selector = null;
					ServerSocketChannel server = null;
					SelectionKey serverkey = null;
					try {
						selector = Selector.open();
						server = ServerSocketChannel.open();
						server.socket().bind(new java.net.InetSocketAddress(8007));
						server.configureBlocking(false);
						serverkey = server.register(selector, SelectionKey.OP_ACCEPT);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					for(;;){
						Log.w(TAG,"Socket For LOOP =========================================");
						try {
							selector.select();
							Set keys = selector.selectedKeys();
							
							for( Iterator i = keys.iterator(); i.hasNext(); ){
								SelectionKey key = (SelectionKey) i.next();
								i.remove();
								
								if( key == serverkey ){
									if( key.isAcceptable() ){
										SocketChannel client = server.accept();
										client.configureBlocking(false);
										SelectionKey clientkey = client.register(selector, SelectionKey.OP_READ);
										clientkey.attach(new Integer(0));
									}
								}else{
									SocketChannel client = (SocketChannel) key.channel();
									if( !key.isReadable() ){ continue;}
									int bytesread = client.read(buffer);
									if( bytesread == -1 ){
										key.cancel();
										client.close();
										continue;
									}
									
									buffer.flip();
									String request = decoder.decode(buffer).toString();
									buffer.clear();
									Log.w(TAG,"Incoming Request " + request);
									
									
									String response = "HTTP/1.1 200 OK\r\n"; 
									response += "Date " + new Date().toString() + "\r\n";
									response += "Cache-Control: private, max-age=0\r\n";
									
									if( request.contains("vcalendar") ){
										response += "Content-type: text/calendar\r\n\r\n";
												
										response += "BEGIN:VCALENDAR\n" +
												"VERSION:2.0\n" +
												"PRODID:-//Haven Skys//NONSGML v1.0//EN\n" +
												"BEGIN:VEVENT\n" +
												"DTSTART:20090508T170000Z\n" +
												"DTEND:20090508T180000Z\n" +
												"ORGANIZER:MAILTO:ryven@mac.com\n" +
												"STATUS:CONFIRMED\n" +
												"SUMMARY:Test Entry\n" +
												"END:VEVENT\n" +
												"END:VCALENDAR\n";
										response += "\n";
										
									}else{
										
										//response += "Date: Fri, 08 May 2009 21:48:59 GMT\r\n";
										response += "Content-type: text/html\r\n\r\n" +
												"<html><title>Welcome to your iCalendar Service</title><body bgcolor=#000000 text=#eeeecc>" +
												"<font size=+2>Hello " + new Date().toString() + "</font><br>\r\n" +
												"</body></html>";
									}
									
									Log.w(TAG,"Responding " + response);
									client.write(encoder.encode(CharBuffer.wrap(response)));
									Log.w(TAG,"Response sent");
								
									
									if( true ){
										Log.w(TAG,"Closing client and key");
										key.cancel();
										client.close();
									}else{
										Log.w(TAG,"Moving on to next request");
										int num = ((Integer)key.attachment()).intValue();
										key.attach(new Integer(num+1));
									}
								}
							}
							
							/*
							client = server.accept();
							Log.w(TAG,"Recieved Connection =========================================");
							//if( client != null ){
							Log.w(TAG,"Responding");
								String response = "HTTP/1.1 200 OK\r\n"; 
								//HTTP/1.1 200 OK
								//Cache-Control: private, max-age=0
								response += "Date: Fri, 08 May 2009 21:48:59 GMT\r\n";
								response += "Content-type: text/plain\r\n\r\n";
								response += "Hello " + new Date().toString() + "\r\n";
								client.write(encoder.encode(CharBuffer.wrap(response)));
								client.close();
							//}	
								//*/
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
				}
			};
			
			return webServiceThread;
		}
    }
	
	 
	 
	@Override
	public void onCreate() {
		super.onCreate();
		Log.w(TAG,"onCreate() NightWorker");
		
		
		Log.i(TAG,"onCreate() HandlerThread");
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
       
        Log.i(TAG,"onCreate() HandlerThread.start()");
        thread.start();

        Log.i(TAG,"onCreate() HandlerThread.getLooper()");
        mServiceLooper = thread.getLooper();
        
        Log.i(TAG,"onCreate() ServiceHandler");
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mServiceHandler.sendEmptyMessage(7);

       Log.i(TAG,"onCreate() end");
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w(TAG,"onDestroy() ++++++++++++++++++++++++");
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Log.w(TAG,"onRebind() ++++++++++++++++++++++++");
	}

	@Override
    public void onStart(Intent intent, int startId) {
	    Log.i(TAG,"onStart() ++++++++++++++++++++++++++++++++++++++++++++");
	        //mResultCode = intent.getIntExtra("result", 0);
	        //mIntent = intent;
	        //mIntentExtras = mIntent.getExtras();

	        Message msg = mServiceHandler.obtainMessage();
	        msg.arg1 = startId;
	        msg.obj = intent;
	        mServiceHandler.sendMessage(msg);
	        
	       //Log.i(TAG,"onStart() Complete");
	    }

	@Override
	public boolean onUnbind(Intent intent) {
		Log.w(TAG,"onUnbind() ++++++++++++++++++++++++");
		return super.onUnbind(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.w(TAG,"onBind() ++++++++++++++++++++++++");
		return null;
	}

	
	
}
