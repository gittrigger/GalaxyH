package com.havenskys.galaxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.util.Log;

public class SyncProcessing {

	final static String TAG = "SyncProcessing";

	private Context mContext;
	private ContentResolver mResolver;
	//private DbAdapter mDataStore;
	private String mOWASessionKey; 
	
	private DefaultHttpClient mHttpClient;
	private HttpResponse mHttpResponse;
	private HttpEntity mHttpEntity;
    private String mHttpPage;
    private String mSessionId, mDestination, mPostpath;
    private List<Cookie> mHttpCookie;
    private String mUrl, mLogin, mPassword;
    
    private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
    
	
	
	public SyncProcessing(Context ctx){
		mContext = ctx;
		mResolver = ctx.getContentResolver();
		mOWASessionKey = "";
	}
	
	public void setSharedPreferences(SharedPreferences sharedPreferences){
		mSharedPreferences = sharedPreferences;
		mPreferencesEditor = sharedPreferences.edit();
		//mPreferencesEditor.putString("destination", destination); mPreferencesEditor.commit();
		//String destination = mSharedPreferences.contains("destination") ? mSharedPreferences.getString("destination", "") : "";
	}
	
	@Override
	protected void finalize() throws Throwable {
    	//if( mDataStore != null ){ mDataStore.close(); }
		super.finalize();
	}

	
    /** Cannot access the calendar */
    public static final int NO_ACCESS = 0;
    /** Can only see free/busy information about the calendar */
    public static final int FREEBUSY_ACCESS = 100;
    /** Can read all event details */
    public static final int READ_ACCESS = 200;
    public static final int RESPOND_ACCESS = 300;
    public static final int OVERRIDE_ACCESS = 400;
    /** Full access to modify the calendar, but not the access control settings */
    public static final int CONTRIBUTOR_ACCESS = 500;
    public static final int EDITOR_ACCESS = 600;
    /** Full access to the calendar */
    public static final int OWNER_ACCESS = 700;
    public static final int ROOT_ACCESS = 800;

    // attendee relationship
    public static final int RELATIONSHIP_NONE = 0;
    public static final int RELATIONSHIP_ATTENDEE = 1;
    public static final int RELATIONSHIP_ORGANIZER = 2;
    public static final int RELATIONSHIP_PERFORMER = 3;
    public static final int RELATIONSHIP_SPEAKER = 4;

    // attendee type
    public static final int TYPE_NONE = 0;
    public static final int TYPE_REQUIRED = 1;
    public static final int TYPE_OPTIONAL = 2;

    // attendee status
    public static final int ATTENDEE_STATUS_NONE = 0;
    public static final int ATTENDEE_STATUS_ACCEPTED = 1;
    public static final int ATTENDEE_STATUS_DECLINED = 2;
    public static final int ATTENDEE_STATUS_INVITED = 3;
    public static final int ATTENDEE_STATUS_TENTATIVE = 4;

    
	public DefaultHttpClient getOwaSession(String who, String url, String login, String password){
		Log.w(TAG,"getOwaSession() 173 url("+url+") login("+login+") password("+password+") for " + who);
		int status = owaLogin(TAG + " getOwaSession() 174 for " + who, url, login, password);
		if( status < 1){ 
			Log.e(TAG,"getOwaSession() 176 failed with status("+status+") url("+url+") login("+login+") password("+password+") SLEEP(500) for " + who);
			SystemClock.sleep(500);
			return null; 
		}
		return mHttpClient;
	}
	
	
	// return 1:good 0:genericproblem -1:loginfailuremissingrefkey -2:badusernamepassword -3:missingvalues -4:wrongversion -5:goodversionbadparse -6:httpfailurehostnotresolved -7:generichttpfailure -8:loginreplyunexpected -9:loginreplyempty
	public int owaLogin(String who, String url, String login, String password) {
		//printToast("OWA Login");
		if( url == null || login == null || password == null ){ return -3; }
		if( url == "" || login == "" || password == "" ){ return -3; }
		Log.w(TAG,"owaLogin() 187 url("+url+") login("+login+") password("+password+") for " + who);
		
		mUrl = url;
		mLogin = login;
		mPassword = password;
		
    	
    	mHttpClient = new DefaultHttpClient();
    	String destination = "";
    	String postpath = "";
    	if( mSharedPreferences != null ){
			//mPreferencesEditor.putString("destination", destination); mPreferencesEditor.commit();
			destination = mSharedPreferences.contains("destination") ? mSharedPreferences.getString("destination", "") : "";
	
			//mPreferencesEditor.putString("postpath", postpath); mPreferencesEditor.commit();
			postpath = mSharedPreferences.contains("postpath") ? mSharedPreferences.getString("postpath", "") : "";
    	}
		
    	if( destination == "" || postpath == "" ) {
	    	// ----------------------------------
	    	// LOGIN FORM
	    	Log.w(TAG,"owaLogin() 216 Getting Login page for " + who);
	        //HttpGet httpget = new HttpGet("https://webmail.xxxxxxx.com/Exchweb/bin/auth/owalogon.asp?url=https://webmail.xxxxxxx.com/Exchange&reason=0&replaceCurrent=1");
	        HttpGet httpget = new HttpGet(url);
	        String httpStatus = safeHttpGet(TAG + " getOwaSession() 219 for " + who, httpget);
	        if( httpStatus.contains("440") ){ // Login Timeout
	        	//Log off and Retry
	        	owaLogoff(TAG + " owaLogin() 222 for " + who);
	        }
	        if( httpStatus.contains("HTTPERRORTHROWN ") ){
	        	Log.e(TAG,"searchContact() 486 "+httpStatus+" for " + who);
	        	if( httpStatus.contains("Host is unresolved")){
	        		return -6;
	        	}else{
	        		return -7;
	        	}
	        }
	        if( !httpStatus.contains("200") ){
	        	Log.e(TAG,"owaLogin() 225 Server replied negatively. for " + who);
	        	if( httpStatus != null ){
	        		Log.w(TAG,"owaLogin() 227 (?badurl) httpReplyStatus " + httpStatus + " for " + who);
	        	}
	        	//String[] pageA = mHttpPage.split("\n");
	            //for( int i = 0; i < pageA.length; i ++){ Log.w(TAG,i+": " + pageA[i]); }
	        	return 0;
	        }
	        
	        // Page replied correctly at this point
	        
	        if( !mHttpPage.contains("/owa/8") ){
	        	Log.e(TAG,"owaLogin() 235 Login page doesn't appear to be Outlook Webmail Access 8 for " + who);
	        	//String[] pageA = mHttpPage.split("\n");
	            //for( int i = 0; i < pageA.length; i ++){ Log.w(TAG,i+": " + pageA[i]); }
	        	return -4;
	        }
	        
	        String[] pageParts = mHttpPage.split("\n");
	        int partsLen = pageParts.length;
	        for(int i = 0; i < partsLen; i ++){
	        	//<input type="hidden" name="destination" value="https://webmail.xxxxxxx.com/Exchange">
	        	if( pageParts[i].contains("name=\"destination\"") ){
	        		destination = pageParts[i].replaceFirst(".*value=\"", "").replaceFirst("\".*", "").trim();
	        	}
	        	//<form action="owaauth.dll" method="POST" name="logonForm" autocomplete="off">
	        	if( pageParts[i].contains("name=\"logonForm\"") ){
	        		postpath = pageParts[i].replaceFirst(".*action=\"", "").replaceFirst("\".*", "").trim();
	        	}
	        	if( destination.length() > 0 && postpath.length() > 0 ){ break; }
	        	//Log.i(TAG,pageParts[i]);
	        }
	        
	        if( destination.length() == 0 ){
	        	Log.e(TAG,"owaLogin() 257 No Destination value for " + who);
	        	return -5;
	        }
	        if( postpath.length() == 0 ){
	        	Log.e(TAG,"owaLogin() 261 No Post value for " + who);
	        	return -5;
	        }
	        

        	Log.w(TAG,"owaLogin() 266 " + httpget.getURI() + " for " + who);
        	if( httpget.containsHeader("Location")){
        		Log.w(TAG,"owaLogin() 280 Has a Location Header for " + who);
        	}
        	//Log.w(TAG,"");
        	
        	
        	Log.w(TAG,"owaLogin() 273 GET() url("+url+") destination("+destination+") postpath("+postpath+") basepath() for " + who);

        	if( mSharedPreferences != null ){
	    		mPreferencesEditor.putString("destination", destination); mPreferencesEditor.commit();
	    		//String destination = mSharedPreferences.contains("destination") ? mSharedPreferences.getString("destination", "") : "";
	        	
	    		mPreferencesEditor.putString("postpath", postpath); mPreferencesEditor.commit();
	    		//String postpath = mSharedPreferences.contains("postpath") ? mSharedPreferences.getString("postpath", "") : "";
	        }
		}
    	
    	
    	mDestination = destination;
        mPostpath = postpath;

        
        // ----------------------------------
    	// POST LOGIN FORM

        //HttpPost httpPost = new HttpPost("https://webmail.xxxxxxx.com/Exchweb/bin/auth/owaauth.dll");
        HttpPost httpPost = new HttpPost(url + "/Exchweb/bin/auth/" + mPostpath);

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("destination",mDestination));
		nvps.add(new BasicNameValuePair("flags","0"));
		nvps.add(new BasicNameValuePair("forcedownlevel","0"));
		nvps.add(new BasicNameValuePair("trusted","4"));//4 is trusted
		nvps.add(new BasicNameValuePair("username",login));
		nvps.add(new BasicNameValuePair("password",password));

		Log.w(TAG,"owaLogin() Create Login Cookie for " + who);
		Date expiredate = new Date();
        expiredate.setTime(System.currentTimeMillis() + 2*7*24*60*60*1000);
        Cookie loginCookie = new BasicClientCookie("logondata", "acc=4&lgn="+login+"; expires="+expiredate.toString());
        CookieStore cs = mHttpClient.getCookieStore();
        cs.addCookie(loginCookie);
        mHttpClient.setCookieStore(cs);
        
        Log.w(TAG,"owaLogin() 311 Logging in. for " + who );
        String httpStatus = safeHttpPost(TAG + " getOwaSession() 312 for " + who, httpPost, nvps);
        
        
        
        String[] pageParts = mHttpPage.split("\n");
        int partsLen = pageParts.length;
        for(int i = 0; i < partsLen; i ++){
        	//<input type="hidden" name="destination" value="https://webmail.xxxxxxx.com/Exchange">
        	if( pageParts[i].contains("name=\"destination\"") ){
        		mDestination = pageParts[i].replaceFirst(".*value=\"", "").replaceFirst("\".*", "").trim();
        	}
        	//<form action="owaauth.dll" method="POST" name="logonForm" autocomplete="off">
        	if( pageParts[i].contains("name=\"logonForm\"") ){
        		mPostpath = pageParts[i].replaceFirst(".*action=\"", "").replaceFirst("\".*", "").trim();
        	}
        	if( mDestination.length() > 0 && mPostpath.length() > 0 ){ break; }
        	//Log.i(TAG,pageParts[i]);
        }

        if( httpStatus.contains("440") ){ // Login Timeout
        	//Log off and Retry
        	owaLogoff(TAG + " owaLogin() 174 for " + who);
        }
        if( !httpStatus.contains("200") ){
        	Log.e(TAG,"owaLogin() 336 Server replied negatively. Interesting, was it because of skipping the login page? for " + who);
        	if( httpStatus != null ){
        		Log.w(TAG,"owaLogin() 338 Server replied negatively. httpStatus("+httpStatus+") for " + who);
        	}
        	// Possibly a redownload of the login page will work
        	
        	if( mSharedPreferences != null ){
	        	mPreferencesEditor.putString("destination", "");// mPreferencesEditor.commit();
	    		//String destination = mSharedPreferences.contains("destination") ? mSharedPreferences.getString("destination", "") : "";
	        	
	    		mPreferencesEditor.putString("postpath", ""); mPreferencesEditor.commit();
	    		//String postpath = mSharedPreferences.contains("postpath") ? mSharedPreferences.getString("postpath", "") : "";
        	}
        	
        	return 0;
        }
        
        
        
        
        if( mHttpPage.contains("a_sFldId") ){
        	Log.i(TAG,"owaLogin() 387 Login Successful for " + who);
        	return 1;
        }else{
        	
        	//String[] pageA = mHttpPage.split("\n");
            //for( int i = 0; i < pageA.length; i ++){ Log.w(TAG,i+": " + pageA[i]); }
        	if( mHttpPage != null ){
        		if( mHttpPage.contains("The user name or password that you entered is not valid.") ){ 
        			Log.e(TAG,"owaLogin() 390 Login Failure positive identification of failure. for " + who);
        			return -2;
        		}
        		Log.e(TAG,"owaLogin() 390 Login Failure (reply received but not what is expected) for " + who);
        		return -8;
        	}else{
        		Log.e(TAG,"owaLogin() 390 Login Failure (no reply received) for " + who);
        		return -9;
        	}
        }
        
	}
	

	public int searchContact(String who, String url, String login, String password, String search, DefaultHttpClient httpClient){
		Log.w(TAG,"searchContact() 403 url("+url+") login("+login+") password("+password+") search("+search+") for " + who);
		mHttpClient = httpClient;
		return searchContact(who, url, login, password, search);
	}
	
	
	// return 1:good 0:genericproblem -1:loginfailuremissingrefkey -2:badusernamepassword -3:missingvalues -4:wrongversion -5:goodversionbadparse -6:httpfailurehostnotresolved -7:generichttpfailure
	public int searchContact(String who, String url, String login, String password, String search){
		if( url == null || login == null || password == null ){ return -3; }
		if( url == "" || login == "" || password == "" ){ return -3; }
		Log.w(TAG,"searchContact() 411 url("+url+") login("+login+") password("+password+") search("+search+") for " + who);
		
		mUrl = url;
		mLogin = login;
		mPassword = password;

		
		//if( mDataStore.getId("browserStore", "key == \"logondata\" AND updated > " + (System.currentTimeMillis() - (3*60*60*1000) )) < 1 ){
			//Log.w(TAG,"Looks like we might be in a timeout condition, login again, just in case.");
		if( mHttpClient == null ){
			Log.w(TAG,"searchContact() 421 goto owaLogin() for " + who);
			int status = owaLogin(who, url, login, password);
			if( status < 1){ 
				Log.e(TAG,"searchContact() 424 failed with status("+status+") for " + who); 
				return status; 
			}
			Log.w(TAG,"searchContact() 427 for " + who);
		}else{
			Log.w(TAG,"searchContact() 429 Using mHttpClient from Memory for " + who);
		}

		
		//mDataStore = new DbAdapter(mContext);
		//mDataStore.loadDb(TAG + " searchContact() 463 for " + who,"contactStore");

        
        //?ae=Folder&t=IPF.Contact&newSch=1&scp=1&
		String httpStatus = safeHttpGet(TAG + " searchContact() 467 for " + who, new HttpGet( url + "/OWA/?ae=Dialog&t=AddressBook&ctx=1&sch=" + search.trim().replaceAll(" ", "%20")));
        if( httpStatus.contains("440") ){ // Login Timeout
        	//Log off and Retry
        	owaLogoff(TAG + " searchContact() 470 for " + who);
        	SystemClock.sleep(25);
        	owaLogin(who, mUrl, mLogin, mPassword);
        }
        if( httpStatus.contains("HTTPERRORTHROWN ") ){
        	Log.e(TAG,"searchContact() 489 "+httpStatus+" for " + who);
        	if( httpStatus.contains("Host is unresolved")){
        		return -6;
        	}else{
        		return -7;
        	}
        }
        if( !httpStatus.contains("200") ){
        	Log.e(TAG,"searchContact() 470 Server replied negatively. for " + who);
        	if( httpStatus != null ){
        		Log.w(TAG,"searchContact() 477 Server replied negatively. httpStatus("+httpStatus+") for " + who);
        	}
        	//mDataStore.close();
        	return 0;
        }

        processContactSearch(TAG + " searchContact() 483 for " + who);

        
        //HttpGet httpGet = new HttpGet("https://webmail.xxxxxxx.com/OWA/?ae=Dialog&t=AddressBook&ctx=1&sch=" + search.trim().replaceAll(" ", "%20"));
        //?ae=Folder&t=IPF.Contact&newSch=1&scp=1&
		httpStatus = safeHttpGet(TAG + " searchContact() 488 for " + who, new HttpGet( url + "/OWA/?ae=Folder&t=IPF.Contact&newSch=1&scp=1&sch=" + search.trim().replaceAll(" ", "%20")));
        if( httpStatus.contains("440") ){ // Login Timeout
        	//Log off and Retry
        	owaLogoff(TAG + " searchContact() 491 for " + who);
        	SystemClock.sleep(25);
        	owaLogin(who, mUrl, mLogin, mPassword);
        }
        //*
        if( !httpStatus.contains("200") ){
        	Log.e(TAG,"searchContact() 519 Server replied negatively. for " + who);
        	return 0;
        }//*/

        processContactSearch(TAG + " searchContact() 501 for " + who);
        
        //mDataStore.close();

		return 1;
	}
	
	
	
	
	
	
	//https://webmail.xxxxxxx.com/OWA/?ae=Dialog&t=AddressBook&ctx=1&sch=ryan
	
	/*
	 * 

personal
0 checkbox
1 name
2 email
3 phone
4 title
5 company

<tr><td nowrap align="center"><input type="checkbox" name="chkRcpt" value="RgAAAAB/crD7Er07RKUkMujUmZSsBwDZxyW4GH/UQ7RRuzj4G+y6AAAEcXP3AADZxyW4GH/UQ7RRuzj4G+y6AAAE5vuLAAAR" onClick="onClkRChkBx(this);">&nbsp;</td>
<td nowrap class="sc"><h1><a href="#" id="RgAAAAB/crD7Er07RKUkMujUmZSsBwDZxyW4GH/UQ7RRuzj4G+y6AAAEcXP3AADZxyW4GH/UQ7RRuzj4G+y6AAAE5vuLAAAR" title="In Folder: Contacts" onClick="return onClkRcpt(this, 3);">Weckstein, George</a></h1>&nbsp;</td>
<td nowrap>&nbsp;</td>
<td nowrap><img class="cPh" src="8.1.340.0/themes/base/work.gif" alt="Business phone">7024359300&nbsp;</td>
<td nowrap>&nbsp;</td>
<td nowrap>&nbsp;</td>
</tr>

company
0 checkbox
1 name
2 alias
3 phone
4 office
4 title
5 company

<tr><td nowrap align="center"><input type="checkbox" name="chkRcpt" value="Ysd7sZmKrkyxnnZVXpO62A==" onClick="onClkRChkBx(this);">&nbsp;</td>
<td nowrap class="sc"><h1><a href="#" id="Ysd7sZmKrkyxnnZVXpO62A==" onClick="return onClkRcpt(this, 1);">Weckstein, Ryan</a></h1>&nbsp;</td>
<td nowrap>RWeckst&nbsp;</td>
<td nowrap>425-383-4931&nbsp;</td>
<td nowrap>WA-Field Service Center, Newport Five&nbsp;</td>
<td nowrap>Project Manager ...&nbsp;</td>
<td nowrap>T-Mobile&nbsp;</td></tr>
	 */
	
	
	public void processContactSearch(String who) {
		
		/*/
		if( mDataStore != null ){ if( !mDataStore.isOpen()){ mDataStore.close(); mDataStore = null;} }
		if( mDataStore == null ){
			mDataStore = new DbAdapter(mContext);
			mDataStore.loadDb(TAG + " processContactSearch() 585 for " + who,"contactStore");
		}//*/
		
		String detailList = "";
		String remoteid,name,fname,lname,alias,phone,office,title,company,email,type;
		
		if( mHttpPage.contains("var a_sT = \"IPF.Contact\"") ){
			type = "personal";
		}else{
			type = "corporate";
		}
		
		String[] pageA = mHttpPage.replaceAll("<tr>", "\n<tr>").split("\n");
        for( int i = 0; i < pageA.length; i ++){
        	//Log.w(TAG,i+": " + pageA[i]);
        	
        	
        	if( !pageA[i].contains("chkRcpt\" value=\"") ){
        		if(pageA[i].contains("name=\"hidcanary\"")){
            		mOWASessionKey = pageA[i].replaceFirst(".*name=\"hidcanary\" value=\"", "").replaceFirst("\".*", "").trim();
            		Log.w(TAG,"processContactSearch() 594 webmailKey(" + mOWASessionKey + ") for " + who);
        		}	
        		continue; 
        	}
            // session ID = var a_sFldId = "LgAAAAB%2fcrD7Er07RKUkMujUmZSsAQDZxyW4GH%2fUQ7RRuzj4G%2by6AAAEcXP2AAAC";
        	remoteid = ""; name = ""; fname = ""; lname = ""; email = ""; alias = ""; phone = ""; office = ""; title = ""; company = ""; 
    		
    		String[] rowParse = pageA[i]
    		                   .replaceAll("&nbsp;", "")
    		                   .split("</td>");
    		
    		if( rowParse.length <= 4 ){ continue; }
    		
    		if( !rowParse[0].contains("type=\"checkbox\"") ){	//|| rowParse.length < 8 ){
    			Log.w(TAG,"processContactSearch() 568 This doesn't appear to be the predicted data. for " + who);
    			continue;
    		}
    		
			remoteid = rowParse[0]
			    	.replaceFirst(".*value=\"", "")
			    	.replaceFirst("\".*", "")
			    	.trim();
    		
    		if( remoteid.length() == 0 ){ continue; }
    		
    		if( type == "personal" ){
    			// Personal Contacts Folder
    			name    = rowParse[1].replaceAll("<.*?>", "").trim();
	    		email   = rowParse[2].replaceAll("<.*?>", "").trim();
	    		phone   = rowParse[3].replaceAll("<.*?>", "").trim();
	    		title   = rowParse[4].replaceAll("<.*?>", "").trim();
	    		company = rowParse[5].replaceAll("<.*?>", "").trim();
    		}else{
    			// Company GAL
	    		name    = rowParse[1].replaceAll("<.*?>", "").trim();
	    		alias   = rowParse[2].replaceAll("<.*?>", "").trim();
	    		phone   = rowParse[3].replaceAll("<.*?>", "").trim();
	    		office  = rowParse[4].replaceAll("<.*?>", "").trim();
	    		title   = rowParse[5].replaceAll("<.*?>", "").trim();
	    		company = rowParse[6].replaceAll("<.*?>", "").trim();
    		}

    		if( name.contains(",") ){
    			String[] nameparts = name.split(",",2);
    			fname = nameparts[1].trim();
    			if( nameparts.length > 1 ){
    				lname = nameparts[0].trim();
    			}
    		}else{
    			fname = name;
    		}

    		
        
	    	try {
	    			
				detailList += remoteid + "\n";
	        	
	        	List <NameValuePair> values = new ArrayList <NameValuePair>();
	        	if( alias.length() > 0 ){ values.add(new BasicNameValuePair("alias",alias)); }
	        	if( phone.length() > 0 ){ values.add(new BasicNameValuePair("phone",phone)); }
	        	if( office.length() > 0 ){ values.add(new BasicNameValuePair("office",office)); }
	        	if( title.length() > 0 ){ values.add(new BasicNameValuePair("title",title)); }
	        	if( company.length() > 0 ){ values.add(new BasicNameValuePair("company",company)); }
	        	if( email.length() > 0 ){ values.add(new BasicNameValuePair("email",email)); }
	        	if( name.length() > 0 ){ values.add(new BasicNameValuePair("name",name)); }
	        	if( type.length() > 0 ){ values.add(new BasicNameValuePair("type",type)); }
	            //values.add(new BasicNameValuePair("mobile",mobile));
	            //values.add(new BasicNameValuePair("fax",fax));
	        	if( fname.length() > 0 ){ values.add(new BasicNameValuePair("fname",fname)); }
	        	if( lname.length() > 0 ){ values.add(new BasicNameValuePair("lname",lname)); }
	            //values.add(new BasicNameValuePair("department",department));
	            //values.add(new BasicNameValuePair("management",management));
	            //values.add(new BasicNameValuePair("egal",egal));
	            //values.add(new BasicNameValuePair("street",street));
	            //values.add(new BasicNameValuePair("city",city));
	            //values.add(new BasicNameValuePair("state",state));
	            //values.add(new BasicNameValuePair("postal",postal));
	            //values.add(new BasicNameValuePair("notes",notes));
	    		updateAddContactRecord( TAG + " 821 for " + who, remoteid, values);

	    	} catch (SQLiteConstraintException e){

	    		Log.w(TAG,"processContactSearch() 618 SQLiteConstraintException (ignoring) " + e.getLocalizedMessage() + " for " + who);
				Log.w(TAG,"processContactSearch() 619 Error Record ID("+remoteid+") name("+name+") alias("+alias+") phone("+phone+") office("+office+") title("+title+") company("+company+") email("+email+") for " + who);
				SystemClock.sleep(100);
	        } catch (SQLiteException e){
	        	
				Log.w(TAG,"processContactSearch() 618 SQLiteException (ignoring) " + e.getLocalizedMessage() + " for " + who);
				Log.w(TAG,"processContactSearch() 619 Error Record ID("+remoteid+") name("+name+") alias("+alias+") phone("+phone+") office("+office+") title("+title+") company("+company+") email("+email+") for " + who);
				SystemClock.sleep(100);
			} finally {
				
			}
		
        }
        
        processContactThread(who, detailList, mUrl, mLogin, mPassword);
        
	}
	
	
	
	//https://webmail.t-mobile.com/OWA/?ae=Item&t=AD.RecipientType.User&id=Ysd7sZmKrkyxnnZVXpO62A%3d%3d&ctx=1
	//https://webmail.t-mobile.com/OWA/?ae=Item&t=IPM.Contact&id=RgAAAAB%2fcrD7Er07RKUkMujUmZSsBwDZxyW4GH%2fUQ7RRuzj4G%2by6AAAEcXP3AADZxyW4GH%2fUQ7RRuzj4G%2by6AAAE5vwQAAAR
	
	private void processContactThread(final String who, final String detailList, final String url, final String login, final String password ) {
		
		
		Thread t = new Thread(){
			public void run() {
				
				//mDataStore = new DbAdapter(mContext);
				//mDataStore.loadDb(TAG + " processContactThread() 585 for " + who,"contactStore");
				
				SystemClock.sleep(1880);
				Log.w(TAG,"processContactThread() 643 for " + who);
				//SyncProcessing.this.owaLogin(url,login,password);
				
				SyncProcessing sp = new SyncProcessing(mContext);
				sp.getOwaSession(who, url, login, password);
				//DefaultHttpClient httpClient = .this.getOwaSession(url,login,password);
				
		        int failcnt = 0;
		        String[] reviewlist = detailList.split("\n");
		        for(int i = 0; i < reviewlist.length; i++){
		        	if( failcnt > 5 ){
		        		Log.e(TAG,"processContentThread() failure limit("+failcnt+") for " + who);
		        		break;
		        	}
		        	
		        	
		        	//if( i > 5 ){break;}// just deeply review the top results
		        	SystemClock.sleep(100);//sleep through these a little, no reason to push the system hard
		        	if(reviewlist[i].length() > 40){ // may have to do a look up and get the type.
		        		// Personal contact
		        		if( !downloadContact(TAG + " processContentThread() 693 for " + who, reviewlist[i], url + "/OWA/?ae=Item&t=IPM.Contact&id=" + reviewlist[i].replaceAll("/", "%2f").replaceAll("=", "%3d").replaceAll("\\+", "%2b") ) ){
		        			failcnt++;
		        		}
		        	}else{
		        		// Company contact
		        		if( !downloadContact(TAG + " processContentThread() 698 for " + who, reviewlist[i], url + "/OWA/?ae=Item&t=AD.RecipientType.User&id=" + reviewlist[i].replaceAll("/", "%2f").replaceAll("=", "%3d").replaceAll("\\+", "%2b") + "&ctx=1") ){
		        			failcnt++;
		        		}
		        	}//AD.RecipientType.User
		        }
				}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
        
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private boolean downloadContact(final String who, final String remoteid, final String url){
		Log.w(TAG,"downloadContact() 657 remoteid("+remoteid+") for " + who);
		String httpStatus = safeHttpGet(TAG + " downloadContact() 658 for " + who, new HttpGet( url ));
        if( httpStatus.contains("440") ){ // Login Timeout
        	//Log off and Retry
        	owaLogoff(TAG + " searchContact() 700 SLEEP(25) for " + who);
        	SystemClock.sleep(25);
        	owaLogin(who, mUrl, mLogin, mPassword);
        }
        if( !httpStatus.contains("200") ){
        	Log.e(TAG,"downloadContact() 705 Server replied negatively. for " + who);
        	if( httpStatus != null ){
        		Log.w(TAG,"downloadContact() 707 Server replied negatively. httpStatus("+httpStatus+") for " + who);
        	}
        	return false;
        }
        
        Thread t = new Thread(){
			public void run() {
				processContact(TAG + " downloadContact() 712 for " + who, remoteid);
			}
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        return true;
        
	}
	
	
	private boolean processContact(String who, String remoteid) {
		
		Log.w(TAG,"processContact() 716 remoteid("+remoteid+") for " + who);
		
		//remoteid, name, alias, phone, office, title, company, email, type, mobile, fax, fname, lname, department, management, egal, street, city, state, postal, notes
		String foundRemoteid,name,alias,phone,office,title,company,email, mobile, fax, fname, lname, department, management, egal, street, city, state, postal, notes;
		String[] pageA = mHttpPage.replaceAll("><tr>", ">\n<tr>").replaceAll("<td .*?>", "<td>").split("\n");

		foundRemoteid = ""; name = ""; alias = ""; phone = ""; office = ""; title = ""; company = ""; email = "";
    	mobile = "";fax = "";fname = "";lname = "";department = "";management = "";egal = "";street = "";city = "";state = "";postal = "";notes = "";
		
    	int recordcnt = 0;
		for( int i = 0; i < pageA.length; i ++){
			
			//Log.w(TAG,"processContact() 728 "+i+": " + pageA[i] + " for " + who);
			
			if( pageA[i].contains("<textarea name=\"notes\"")){
				notes = pageA[i].replaceAll("</textarea>.*", "").replaceFirst(".*?textarea.*?>", "").trim();
			}
			if( foundRemoteid.length() == 0){
				if( pageA[i].contains("<input type=\"hidden\" name=\"hidid\"") || pageA[i].contains("<input type=\"hidden\" name=\"chkRcpt\"") ){
					foundRemoteid = pageA[i].replaceFirst(".*value=\"", "").replaceFirst("\".*", "").trim();
				}
			}
			//chkRcpt
			//if( pageA[i].contains("<input type=\"hidden\" name=\"hidid\"")){
				//foundRemoteid = pageA[i].replaceFirst(".*value=\"", "").replaceFirst("\".*", "");
			//}
        	if( pageA[i].contains("</td><td>") ){
        		//Log.w(TAG,"processContact() 736 "+i+": " + pageA[i] + " for " + who);
        		
        		String[] part = pageA[i].replaceFirst("</td><td>", ":SPLIT:").replaceAll("<.*?>", "").split(":SPLIT:",2);
        		if( part[0] != null ){
        			if( part[1] != null && part[0].length() > 0 ){
        				Log.w(TAG,"processContact() 741 parsed column(" + part[0] + ") value(" + part[1] + ") for " + who);
        				if( part[0].equalsIgnoreCase("alias") 		){ 	alias = part[1]; }
        				if( part[0].equalsIgnoreCase("e-mail") 		){ 	email = part[1];
        				
	        				if( pageA[i].contains("onClkAddRcpt") ){
	    						email = pageA[i].replaceFirst(".*onClkAddRcpt\\('", "").replaceFirst("'.*", "");
	    					}else
    						if( pageA[i].contains("mailto") ){
        						email = pageA[i].replaceFirst(".*mailto:", "").replaceFirst("\".*", "");
        					}else
        					if( pageA[i].contains("\\(") ){
        						email = pageA[i].replaceFirst(".*\\(", "").replaceFirst("\\).*", "");
        					}
        				}
        				if( part[0].equalsIgnoreCase("office") 		){	office = part[1]; }
        				if( part[0].equalsIgnoreCase("phone") 		){ 	phone = part[1]; }
        				if( part[0].equalsIgnoreCase("mobile") 		){ 	mobile = part[1]; }
        				if( part[0].equalsIgnoreCase("fax") 		){ 	fax = part[1]; }
        				if( part[0].equalsIgnoreCase("First name") 	){ 	fname = part[1]; }
        				if( part[0].equalsIgnoreCase("Last name") 	){ 	lname = part[1]; }
        				if( part[0].equalsIgnoreCase("Job Title") 	){ 	title = part[1]; }
        				if( part[0].equalsIgnoreCase("Department") 	){ 	department = part[1]; }
        				if( part[0].equalsIgnoreCase("Company") 	){ 	company = part[1]; }
        				if( part[0].equalsIgnoreCase("Management") 	){ 	
        					//management = part[1];
        					//management = mHttpPage.replaceFirst(".*?>Management<", "").replaceFirst("</table>.*", "").replaceAll("<.*?>", " ").replaceAll("\\s+", " ").trim();
        					management = "";
        					for( i++ ; i < pageA.length; i++ ){
        						if( pageA[i].contains("</table>") ){ break; }
        						management += pageA[i].replaceAll("<.*?>", "") + "; ";
        					}
        					//management = management.replaceFirst(", $", "");
        					management = management.replaceAll("; ; ", "; ").replaceFirst("; $", "");
        					//Log.w(TAG,"processContact() 804 parsed management(" + management + ") for " + who);
        				}
        				if( part[0].equalsIgnoreCase("Shares same manager") ){ 	
        					//management = part[1];
        					//egal = mHttpPage.replaceFirst(".*>Shares same manager<", "").replaceFirst("</table>.*", "").replaceAll("<.*?>", " ").replaceAll("\\s+", " ").trim();
        					egal = "";
        					for( i++ ; i < pageA.length; i++ ){
        						if( pageA[i].contains("</table>") ){ break; }
        						egal += pageA[i].replaceAll("<.*?>", "") + "; ";
        					}
        					egal = egal.replaceAll("; ; ", "; ").replaceFirst("; $", "");
        					//Log.w(TAG,"processContact() 817 parsed egal(" + egal + ") for " + who);
        				}
        			}
        		}
        		recordcnt++;
        	}else if(pageA[i].contains("name=\"hidcanary\"")){
        		mOWASessionKey = pageA[i].replaceFirst(".*name=\"hidcanary\" value=\"", "").replaceFirst("\".*", "").trim();
        		Log.w(TAG,"processContact() 826 webmailKey(" + mOWASessionKey + ") for " + who);
        	}else{
        		continue;
        	}
        	//if( name == ""){ return false; }
        	
        	/*/
    		String[] rowParse = pageA[i]
    		                   .replaceAll("&nbsp;", "")
    		                   .split("</td>");
    		//*/


    		
		}
		
		if( foundRemoteid.length() > 0 ){
			
        		//Log.w(TAG,"processContact() 801 Updating remoteid("+remoteid+") foundRemoteid("+foundRemoteid+") for " + who);
        		
	        	List <NameValuePair> values = new ArrayList <NameValuePair>();
	            values.add(new BasicNameValuePair("alias",alias));
	            values.add(new BasicNameValuePair("phone",phone));
	            values.add(new BasicNameValuePair("office",office));
	            values.add(new BasicNameValuePair("title",title));
	            values.add(new BasicNameValuePair("company",company));
	            values.add(new BasicNameValuePair("email",email));
	            //values.add(new BasicNameValuePair("name",name));
	            //values.add(new BasicNameValuePair("type",type));
	            values.add(new BasicNameValuePair("mobile",mobile));
	            values.add(new BasicNameValuePair("fax",fax));
	            values.add(new BasicNameValuePair("fname",fname));
	            values.add(new BasicNameValuePair("lname",lname));
	            values.add(new BasicNameValuePair("department",department));
	            values.add(new BasicNameValuePair("management",management));
	            values.add(new BasicNameValuePair("egal",egal));
	            values.add(new BasicNameValuePair("street",street));
	            values.add(new BasicNameValuePair("city",city));
	            values.add(new BasicNameValuePair("state",state));
	            values.add(new BasicNameValuePair("postal",postal));
	            values.add(new BasicNameValuePair("notes",notes));
	    		updateAddContactRecord( TAG + " 821 for " + who, remoteid, values);
	    	
		}

		if( recordcnt == 0 ){ return false; }
        return true;
	}

	private String owaLogoff(String who) {
		Log.w(TAG,"owaLogoff() 799 for " + who);
    	String[] pageA = mHttpPage.replaceAll("</td>", "\n</td>").split("\n");
    	String logoffKey = "";
        for( int i = 0; i < pageA.length; i ++){
        	if( pageA[i].contains("onClkLgf") ){
        		logoffKey = pageA[i]
        		                  .replaceFirst(".*onClkLgf('", "")
        		                  .replaceFirst("'.*", "");
        		break;
        	}
        }
        if( logoffKey.length() > 0 ){
        	Log.w(TAG,"owaLogoff() 811 Log OFF for " + who);
        	HttpGet httpGet = new HttpGet( mUrl + "/OWA/logoff.owa?canary=" + logoffKey);
        	//logoff.owa?canary=
        	String httpStatus = safeHttpGet(TAG + " owaLogoff() 816 for " + who, httpGet);
        	return httpStatus;
        }else{
        	Log.w(TAG,"owaLogoff() 819 failed for " + who);
        }
        return "";
	}

	
    public String safeHttpPost(String who, HttpPost httpPost, List<NameValuePair> nvps) {
    	Log.w(TAG,"safeHttpPost() 972 getURI("+httpPost.getURI()+") for " + who);
        //HttpParams params = httpclient.getParams();
        //params.setParameter("Cookies", "logondata=acc=1&lgn=DDDD0/uuuu; expires="+expiredate.toString());
        //httpclient.set
    	
    	String responseCode = ""; mHttpPage = "";
        try {
        	Log.w(TAG,"safeHttpPost() 979 UrlEncodeFormValues for " + who);
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			Log.w(TAG,"safeHttpPost() 981 httpclient.execute() for " + who);
	        mHttpResponse = mHttpClient.execute(httpPost);

	        if( mHttpResponse != null ){
		        Log.w(TAG,"safeHttpPost() " + mHttpResponse.getStatusLine());
		        
		        Log.w(TAG,"safeHttpPost() response.getEntity()");
		        mHttpEntity = mHttpResponse.getEntity();
	
		        if (mHttpEntity != null) {
			        //byte[] bytes = ;
			        mHttpPage = new String(EntityUtils.toByteArray(mHttpEntity));
			        Log.w(TAG,"safeHttpPost() 993 Downloaded " + mHttpPage.length() + " bytes. for " + who);
			        
			        mHttpCookie = mHttpClient.getCookieStore().getCookies();
			        //
			        // Print Cookies
			        if ( !mHttpCookie.isEmpty() ) { for (int i = 0; i < mHttpCookie.size(); i++) { Log.w(TAG,"safeHttpPost() Cookie: " + mHttpCookie.get(i).toString()); } }
	
			        //
			        // Print Headers
			        Header[] h = mHttpResponse.getAllHeaders(); for( int i = 0; i < h.length; i++){ Log.w(TAG,"safeHttpPost() Header: " + h[i].getName() + ": " + h[i].getValue()); }
			        
			        // Clear memory
			        mHttpEntity.consumeContent();
		        }
		        
		        // Get response code string
		        responseCode = mHttpResponse.getStatusLine().toString();
	        }
		} catch (UnsupportedEncodingException e) {
			Log.w(TAG,"safeHttpPost() 1012 UnsupportedEncodingException for " + who);
			e.printStackTrace();
			responseCode = "HTTPERRORTHROWN " + e.getLocalizedMessage();
		} catch (ClientProtocolException e) {
			Log.w(TAG,"safeHttpPost() 1015 ClientProtocolException for " + who);
			e.printStackTrace();
			responseCode = "HTTPERRORTHROWN " + e.getLocalizedMessage();
		} catch (IOException e) {
			Log.w(TAG,"safeHttpPost() 1018 IOException for " + who);
			e.printStackTrace();
			responseCode = "HTTPERRORTHROWN " + e.getLocalizedMessage();
		} catch (IllegalStateException e) {
			Log.w(TAG,"safeHttpPost() 1021 IllegalState Exception for " + who);
			e.printStackTrace();
		}

		return responseCode;
		
    }
    


	public String safeHttpGet(String who, HttpGet httpget) {
		
		Log.w(TAG,"safeHttpGet() 1033 getURI("+httpget.getURI()+") for " + who);
		if( httpget.getURI().toString() == "" ){
			Log.e(TAG,"safeHttpGet 1035 Blocked empty request for " + who);
			return "";
		}
		
		String responseCode = ""; mHttpPage = "";
		
		try {

			Log.w(TAG,"safeHttpGet() 1044 httpclient.execute() for " + who);
			mHttpResponse = mHttpClient.execute(httpget);
			
			if( mHttpResponse != null ){
		        Log.w(TAG,"safeHttpGet() 1048 " + mHttpResponse.getStatusLine() + " for " + who);
		        
		        Log.w(TAG,"safeHttpGet() 1050 response.getEntity() for " + who);
		        mHttpEntity = mHttpResponse.getEntity();
	
		        if (mHttpEntity != null) {
			        //byte[] bytes = ;
			        mHttpPage = new String(EntityUtils.toByteArray(mHttpEntity));
			        Log.w(TAG,"safeHttpGet() 1056 Downloaded " + mHttpPage.length() + " bytes. for " + who);
			        
			        mHttpCookie = mHttpClient.getCookieStore().getCookies();
			        //
			        // Print Cookies
			        //if ( !mHttpCookie.isEmpty() ) { for (int i = 0; i < mHttpCookie.size(); i++) { Log.w(TAG,"safeHttpGet() Cookie: " + mHttpCookie.get(i).toString()); } }
			        
			        //
			        // Print Headers
		        	//Header[] h = mHttpResponse.getAllHeaders(); for( int i = 0; i < h.length; i++){ Log.w(TAG,"safeHttpGet() Header: " + h[i].getName() + ": " + h[i].getValue()); }
			        
			        mHttpEntity.consumeContent();
				}
			}
	        responseCode = mHttpResponse.getStatusLine().toString();
			
		} catch (ClientProtocolException e) {
			Log.w(TAG,"safeHttpGet() 1121 ClientProtocolException for " + who);
			Log.w(TAG,"safeHttpGet() 1122 IO Exception Message " + e.getLocalizedMessage());
			e.printStackTrace();
			responseCode = "HTTPERRORTHROWN " + e.getLocalizedMessage();
		} catch (NullPointerException e) {
			Log.w(TAG,"safeHttpGet() 1126 NullPointer Exception for " + who);
			Log.w(TAG,"safeHttpGet() 1127 IO Exception Message " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.w(TAG,"safeHttpGet() 1130 IO Exception for " + who);
			//if( e.getLocalizedMessage().contains("Host is unresolved") ){ SystemClock.sleep(1880); }
			Log.w(TAG,"safeHttpGet() 1132 IO Exception Message " + e.getLocalizedMessage());
			StackTraceElement[] err = e.getStackTrace();
			for(int i = 0; i < err.length; i++){
				Log.w(TAG,"safeHttpGet() 1135 IO Exception Message " + i + " class(" + err[i].getClassName() + ") file(" + err[i].getFileName() + ") line(" + err[i].getLineNumber() + ") method(" + err[i].getMethodName() + ")");
			}
			responseCode = "HTTPERRORTHROWN " + e.getLocalizedMessage();
		} catch (IllegalStateException e) {
			Log.w(TAG,"safeHttpGet() 1139 IllegalState Exception for " + who);
			Log.w(TAG,"safeHttpGet() 1140 IO Exception Message " + e.getLocalizedMessage());
			e.printStackTrace();
			//if( responseCode == "" ){
				//responseCode = "440"; //440 simulates a timeout condition and recreates the client.
			//}
		}
		
		return responseCode;
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
        	}

            dataCursor.close();
        }
        
        
		return reply;
	}
	

	public String getHttpPage() {
		if( mHttpPage == null ){ return ""; }
		return new String(mHttpPage);
	}

	
	/*
	 *         
	 *  List <NameValuePair> values = new ArrayList <NameValuePair>();
        values.add(new BasicNameValuePair("destination",mDestination));
		updateAddContactRecord(String who, String remoteid, List<NameValuePair> values)
	 */
	
	public void updateAddContactRecord(String who, String remoteid, List<NameValuePair> values){

		/*/
		if( mDataStore != null ){ mDataStore.close(); mDataStore = null; }
		if( mDataStore == null ){
			mDataStore = new DbAdapter(mContext);
			mDataStore.loadDb(TAG + " updateAddContactRecord() 1116 for " + who,"contactStore");
		}//*/
		
		
		int valueslen = values.size();
		
		ContentValues columndata = new ContentValues();
		
		//String[] columns = new String[valueslen];
		String name = "";
		
		//String[] storage = new String[valueslen];
		String value = "";

		String valuelist = "";

		int newcolcnt = 0;
		for(int i = 0; i < valueslen; i++){
			name = values.get(i).getName();
			//columns[i] = name;
			
			value = values.get(i).getValue();
			//storage[i] = value;
			
			
    		
			if( value.contains("...") ){
				Log.w(TAG,"FOUND ... in name("+name+") value("+value+")");
				//columns[i] = "";
				//storage[i] = "";
				columndata.put(name,value);
			}else{
				newcolcnt++;
				columndata.put(name,value);
			}
			valuelist += name + "("+value+") ";
		}
		/*/
		if( newcolcnt < valueslen ){
			valuelist = "";
			String[] columns2 = new String[newcolcnt];
			String[] storage2 = new String[newcolcnt];
			int col = 0;
			for(int i = 0; i < valueslen; i++){
				if( columns[i].length() == 0 ){
					continue;
				}
				columns2[col] = columns[i];
				storage2[col] = storage[i];
				col++;
				valuelist += columns[i] + "("+storage[i]+") ";
			}
			columns = columns2;
			storage = storage2;
		}//*/
		
		
		Log.w(TAG,"updateAddContactRecord() 1083 "+valuelist+" for " + who);

		
		try {

    		
			Cursor idCursor = null;
			long rowId = -1;
			idCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), new String[] {"_id"}, "remoteid=\""+remoteid+"\"", null, null);
			if( idCursor != null){if( idCursor.moveToFirst() ){ rowId = idCursor.getLong(0); } idCursor.close();}
			//int rowId = mDataStore.getId("contactStore", "remoteid=\""+remoteid+"\"");
    		
    		
    		if( rowId <= 0 ){
    			ContentValues cv = new ContentValues();
    			cv.put("remoteid",remoteid);
    			cv.put("status",0);
    			Uri newrecord = SqliteWrapper.insert(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), cv);
    			String newid = newrecord.getLastPathSegment();
    			//long newid = mDataStore.addEntry("contactStore", new String[] {"remoteid"}, new String[] {remoteid});
    			if( newid.length() > 0 ){
    				rowId = Long.parseLong(newid);
    			}
    		}
			
        	if( rowId > 0 ){
        		Log.i(TAG,"updateAddContactRecord() 1100 Updating contactStore("+rowId+") remoteid("+remoteid+") for " + who);
        		columndata.put("status", 1);
        		
	        	//mDataStore.updateEntry("contactStore", rowId, columns, storage);
	        	SqliteWrapper.update(mContext, mResolver, Uri.withAppendedPath(Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"),""+rowId), columndata, null, null);
	        	

	        	if( mSharedPreferences != null ){
	        		Log.i(TAG,"Shared Preferences Found ++++++++++++++++++++++++++++++");
	        		boolean syncAndroid = mSharedPreferences.contains("syncandroid") ? mSharedPreferences.getBoolean("syncandroid",false) : false;
		        	if( syncAndroid ){
		        		Log.i(TAG,"Shared Preferences Found syncAndroid ++++++++++++++++++++++++++++++");
		        		
		        		
		        		Cursor typeCursor = null;
		    			String recordType = "";
		    			typeCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), new String[] {"type"}, "_id=\""+rowId+"\"", null, null);
		    			if( typeCursor != null){if( typeCursor.moveToFirst() ){ recordType = typeCursor.getString(0); } typeCursor.close();}
		    			
		        		//String recordType = mDataStore.getString("contactStore", rowId, "type");
		        		String login = mSharedPreferences.contains("login") ? mSharedPreferences.getString("login", "") : "";
						String username = login.replaceAll(".*?/", "").replaceAll(".*?\\\\", "");
			        	if( recordType.contains("personal") ){
			        		Log.i(TAG,"Shared Preferences Found personal record ++++++++++++++++++++++++++++++");
				        	updateAddAndroidContact(TAG + " updateAddContactRecord() 1123", "Galaxy: "+ username, rowId);
			        	}else{
			        		Log.w(TAG,"Shared Preferences NO SYNC recordType("+recordType+") --------------------------");
			        	}
		        	}else{
		        		//Log.e(TAG,"Shared Preferences NO SYNC --------------------------");
		        	}
	        	}else{
	        		Log.e(TAG,"Shared Preferences NOT Found --------------------------");
	        	}
	        
        	}else {
        		Log.w(TAG,"updateAddContactRecord() 1105 Interesting remoteid("+remoteid+") failed update because no row Id. for " + who);
        	}
        	
		} catch (SQLiteException e){
			Log.e(TAG,"updateAddContactRecord() 1135 Caught update failure(SQLiteException) for " + remoteid + " for " + who);
			e.printStackTrace();
		} catch (IllegalStateException e){
			Log.e(TAG,"updateAddContactRecord() 1139 Caught update failure(IllegalStateException) for " + remoteid + " for " + who);
			e.printStackTrace();
		}
		
	}
	
	public void updateAddAndroidContact(String who, String groupname, long rowId) {
		
		//if( mDataStore != null ){ mDataStore.close(); mDataStore = null; }
		//if( mDataStore == null ){
			//mDataStore = new DbAdapter(mContext);
			//mDataStore.loadDb(TAG + " updateAddAndroidContact() 1145 for " + who,"contactStore");
		//}
		
		//Cursor data = mDataStore.getEntry("contactStore", rowId);
		Cursor data = null;
		data = SqliteWrapper.query(mContext, mContext.getContentResolver(), Uri.withAppendedPath(DataProvider.CONTENT_URI,"contactStore"), new String[] {"_id","mobile","phone","email","fname","lname","title","department","company","notes","postal"}, "_id=\""+rowId+"\"", null, null);
		//if( idCursor != null){if( idCursor.moveToFirst() ){ rowId = idCursor.getLong(0); } idCursor.close();}
		if( data != null ){
			if( data.moveToFirst() ){
				String mobile = data.getString(1);
				String phone = data.getString(2);
				String email = data.getString(3);
				String first_name = data.getString(4);
				String last_name = data.getString(5);
				String title = data.getString(6);
				String department = data.getString(7);
				String company = data.getString(8);
				String notes = data.getString(9);
				String postal = data.getString(10);
				updateAddAndroidContact(TAG + " updateAddAndroidContact(by rowId) 1141 for "+ who, groupname, mobile, phone, email, first_name, last_name, title, department, company, notes, postal);
			}
			data.close();
		}
	}

	//mSyncProcessing.updateAddAndroidContact("Galaxy: "+ username, mobile, phone, email, fname, lname, title, department, company, notes);
	public boolean updateAddAndroidContact(String who, String groupname, String mobile, String phone, String email, String first_name, String last_name, String title, String department, String company, String notes, String postal) {

		//if( mDataStore != null ){ if( !mDataStore.isOpen()){ mDataStore.close(); mDataStore = null;} }
		//if( mDataStore == null ){
			//mDataStore = new DbAdapter(mContext);
			//mDataStore.loadDb(TAG + " updateAddAndroidContact() 1173 for " + who,"contactStore");
		//}
		
		String mainnumber = "";
		if( mobile == null ){ mobile = ""; }
		if( phone == null ){  phone = ""; }
		
		if( mobile.length() > 0 ){
			mainnumber = mobile;
		}else if(phone.length() > 0){
			mainnumber = phone;
		}
		
		String numberkey = "";
    	if( mainnumber.length() > 0 ){
    		mainnumber = mainnumber.replaceAll("-", "").replaceAll(" ", "").replaceAll("\\+", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\.", "");
    		
    		int cN;
        	for( int ch = mainnumber.length() - 1; ch >= 0; ch --){

        		char c = mainnumber.charAt(ch);
        		try {
        			
        			cN = Integer.parseInt( ""+c );
        		}
        		catch ( NumberFormatException e ){
        			// Skip non-number characters
        			continue;
        		}
        		numberkey += ""+cN;
        	}
        	// US, add the 1, I believe callerid always reports 1aaabbbgggg
        	
        	if( numberkey.length() == 11 && numberkey.charAt(10) == '1' ){
        		numberkey = numberkey.substring(0, 10);
        	}
        	
        	Log.w(TAG,"updateAddAndroidContact() 1210 updated mainnumber(" + mainnumber + ") to numberkey("+numberkey+") for " + who);
    	}
    	
    	if( numberkey.length() == 0 ){
    		Log.e(TAG,"updateAddAndroidContact() 1214 tried to use an empty phone number found from mobile("+mobile+") and phone("+phone+") for " + who);
    		return false;
    	}
    	
    	
		long groupid = getId("content://contacts/groups", "notes = \""+ groupname +"\"");
		
		if( !( groupid > 0) ){
			Log.w(TAG,"updateAddAndroidContact() 1226 group doesn't exist.");
			//not
			ContentValues values = new ContentValues();
			values.put("should_sync", 0 );
			values.put("notes", groupname );
	        values.put("name", groupname );
	        //values.put("system_id", "Galaxy");
	        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/groups"), values);
	        
	        groupid = getId("content://contacts/groups", "name = \""+ groupname +"\"");
		}
		
		
		if( groupid > 0 ){
			Log.i(TAG,"updateAddAndroidContact() 1240 groupid("+groupid+")");
			ContentValues values = new ContentValues();
			values.put("notes", groupname );
	        values.put("name", groupname );
			SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/groups/" + groupid), values, null, null);
	        
			String fullname = "";
			if( first_name.length() > 0 ){
				fullname = first_name;
			}
			if( last_name.length() > 0 ){
				fullname += " " + last_name;
			}
			fullname = fullname.trim();
			
			String companydept = "";
			if( company.length() > 0  && !company.contains("...")){
				companydept = company;
			}
			if( department.length() > 0 && !department.contains("...") ){
				companydept += " ("+department+")";
			}
			companydept = companydept.trim();
			
			long peopleid = getId("content://contacts/people", "number_key = " + numberkey);
			if( peopleid > 0 ){
				Log.i(TAG,"Found person("+peopleid+") under number_key("+numberkey+")");
				//return;
			}else{
				peopleid = getId("content://contacts/people", "name = \""+fullname+"\"");
			}
			
			if( peopleid > 0 ){
				// exists
				Log.i(TAG,"updateAddAndroidContact() 1274 peopleid("+peopleid+")");
				/*ContentValues */values = new ContentValues();
				if( notes != null ){ values.put("notes", notes); }
		        values.put("name", fullname );
		        SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/people/"+peopleid), values, null, null);
			}else{
				//not
				Log.w(TAG,"updateAddAndroidContact() 1281 person("+fullname+") doesn't exist yet");
		        
		    	/*ContentValues */values = new ContentValues();
		    	if( notes != null ){ values.put("notes", notes); }
		        values.put("name", fullname );
		        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/people"), values);
		        peopleid = getId("content://contacts/people", "name = \""+fullname+"\"");
			}
			
			
			if( peopleid > 0 ){
				
				long groupmembershipid = getId("content://contacts/groupmembership", "person = " + peopleid + " AND group_id = " + groupid);
		        if( groupmembershipid > 0){
		        	Log.w(TAG,"updateAddAndroidContact() 232 groupmembershipid("+groupmembershipid+")");
		        	/*ContentValues*/ values = new ContentValues();
			        //values.put("person", peopleid );
			        //values.put("group_id", 	groupid );
			        //SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/groupmembership/" + groupmembershipid), values, null, null);
		        } else {
			        /*ContentValues*/ values = new ContentValues();
			        values.put("person", peopleid );
			        values.put("group_id", 	groupid );
			        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/groupmembership"), values);
		        }
		        
				
				
				if( companydept.length() > 0 ){
					long orgid = getId("content://contacts/organizations","person = " + peopleid);
					if( orgid > 0 ){
						Log.i(TAG,"updateAddAndroidContact() 288 orgid("+orgid+")");
						/*ContentValues*/ values = new ContentValues();
						if( title.length() > 0  && !title.contains("...") ) { values.put("title", title ); }
						values.put("company", companydept );
				        values.put("person", peopleid );
				        //values.put("isprimary", 1 );
				        values.put("type", Contacts.Organizations.TYPE_WORK );
				        //String x = People.ContactMethods.CONTENT_DIRECTORY;
				        //values.put("system_id", "Galaxy");
				        SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/organizations/" + orgid), values, null, null);
			        }else{
						/*ContentValues*/ values = new ContentValues();
						if( title.length() > 0  && !title.contains("...") ) { values.put("title", title ); }
						values.put("company", companydept );
				        values.put("person", peopleid );
				        //values.put("isprimary", 1 );
				        values.put("type", Contacts.Organizations.TYPE_WORK );
				        //values.put("system_id", "Galaxy");
				        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/organizations"), values);
					}
				}
				
				if( email.length() > 0  && !email.contains("...")){
					long emailid = getId("content://contacts/people/"+peopleid+"/contact_methods", "data like \""+email+"\"");
			        if( emailid > 0 ){
			        	Log.i(TAG,"updateAddAndroidContact() 310 emailid("+emailid+")");
			        	/*ContentValues*/ values = new ContentValues();
				        //values.put("data", "galaxy@docchompssoftware.com" );
				        values.put(ContactMethods.DATA, email );
				        values.put(ContactMethods.KIND, Contacts.KIND_EMAIL );
				        values.put(ContactMethods.TYPE, ContactMethods.TYPE_HOME );
				        SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/people/"+peopleid+"/contact_methods/" + emailid), values, null, null);
			        }else{
				        /*ContentValues*/ values = new ContentValues();
				        values.put(ContactMethods.DATA, email );
				        values.put(ContactMethods.KIND, Contacts.KIND_EMAIL );
				        values.put(ContactMethods.TYPE, ContactMethods.TYPE_WORK );
				        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/people/"+peopleid+"/contact_methods"), values);
				        emailid = getId("content://contacts/people/"+peopleid+"/contact_methods", "data like \""+email+"\"");
			        }
				}
		        
				if( postal.length() > 0  && !postal.contains("...")){
			        long postalid = getId("content://contacts/people/"+peopleid+"/contact_methods", "data like \""+postal+"\"");
			        if( postalid > 0 ){
			        	Log.i(TAG,"updateAddAndroidContact() 329 postalid("+postalid+")");
			        	/*ContentValues*/ values = new ContentValues();
				        //values.put("data", "galaxy@docchompssoftware.com" );
				        values.put(ContactMethods.DATA, postal );
				        values.put(ContactMethods.KIND, Contacts.KIND_POSTAL );
				        values.put(ContactMethods.TYPE, ContactMethods.TYPE_HOME );
				        SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/people/"+peopleid+"/contact_methods/" + postalid), values, null, null);
			        }else{
				        /*ContentValues*/ values = new ContentValues();
				        values.put(ContactMethods.DATA, postal );
				        values.put(ContactMethods.KIND, Contacts.KIND_POSTAL );
				        values.put(ContactMethods.TYPE, ContactMethods.TYPE_WORK );
				        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/people/"+peopleid+"/contact_methods"), values);
				        postalid = getId("content://contacts/people/"+peopleid+"/contact_methods", "data like \""+postal+"\"");
			        }
				}
				
				
				if( phone.length() > 0 ){
					phone = phone.replaceAll("-", "").replaceAll(" ", "").replaceAll("\\+", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\.", "");
					long phoneid = getId("content://contacts/phones", "number = \""+phone+"\" AND person = " + peopleid);
			        if( phoneid > 0 ){
			        	Log.i(TAG,"updateAddAndroidContact() 342 phoneid("+phoneid+")");
			        	/*ContentValues*/ values = new ContentValues();
				        values.put("number", phone );
				        values.put("person", peopleid );
				        values.put("type", People.Phones.TYPE_WORK );
				        SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/phones/" + phoneid), values, null, null);
			        }else{
				        /*ContentValues*/ values = new ContentValues();
				        values.put("number", phone );
				        values.put("person", peopleid );
				        values.put("type", People.Phones.TYPE_WORK );
				        //values.put("primary_email", emailid );
				        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/phones"), values);
			        }
				}
				
				if( mobile.length() > 0 ){
					mobile = mobile.replaceAll("-", "").replaceAll(" ", "").replaceAll("\\+", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\.", "");
					long mobileid = getId("content://contacts/phones", "number = \""+mobile+"\" AND person = " + peopleid);
			        if( mobileid > 0 ){
			        	Log.i(TAG,"updateAddAndroidContact() 342 phoneid("+mobileid+")");
			        	/*ContentValues*/ values = new ContentValues();
				        values.put("number", mobile );
				        values.put("person", peopleid );
				        values.put("type", People.Phones.TYPE_MOBILE );
				        SqliteWrapper.update(mContext, mResolver, Uri.parse("content://contacts/phones/" + mobileid), values, null, null);
			        }else{
				        /*ContentValues*/ values = new ContentValues();
				        values.put("number", mobile );
				        values.put("person", peopleid );
				        values.put("type", People.Phones.TYPE_MOBILE );
				        SqliteWrapper.insert(mContext, mResolver, Uri.parse("content://contacts/phones"), values);
			        }
				}
		        
		        
		        
				//androidDataPrint("content://contacts/people/"+peopleid+"/contact_methods");
				
				return true;
			   
			}else{
				return false;
			}
		}else{
			return false;
		}

	}

	
	
	

	public void androidDataPrint(String basePath){
		androidDataPrint(basePath,"");
	}
	
	public void androidDataPrint(String basePath,String where){
        // ----------------------------------
    	// CALENDAR ENTRIES

        Cursor dataCursor = SqliteWrapper.query(mContext, mResolver, Uri.parse(basePath) 
        		,null //new String[] { "_id", "address", "body", "datetime(date/1000, 'unixepoch', 'localtime') as date" },
        		,where // //"date > " + (System.currentTimeMillis() - ( 365 * 24 * 60 * 60 * 1000) ),
        		,null
        		,null //"date desc"
        		);
        
        
        String colData = "";
        long rowId = 0;
        if( dataCursor != null ){
        	if( dataCursor.moveToFirst() ){
        		Log.w(TAG,"Android Data "+basePath+" oooooooooooooooooooooooooooooooooooooooo");
        		String[] col = dataCursor.getColumnNames();
        		for( int c = 0; c < col.length; c++ ){
        			Log.w(TAG,"    Column["+c+"] " + col[c]);
        		}
        		
        		for( int i = 0; i < dataCursor.getCount(); i++ ){
        			Log.w(TAG, basePath + " Entry " + i + " ");
        			dataCursor.moveToPosition(i);
        			rowId = dataCursor.getInt(dataCursor.getColumnIndex("_id"));
        			for( int c = 0; c < col.length; c++ ){	        				
        				//if( col[c].charAt(0) == '_' ){ continue; }
        				colData = dataCursor.getString(c);
        				Log.w(TAG,"    #"+rowId+" ["+i+"] " + col[c] + ": " + colData);
        			}
        		}
    		}
        	dataCursor.close();
    	}
        
	}
	
}

