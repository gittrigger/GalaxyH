package com.havenskys.galaxy;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class DataProvider extends ContentProvider {

	private static String TAG = "DataProvider";

	private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;
    private Custom mLog;
    private static final int DB_VERSION = 23;
    
    private static final int ALL_MESSAGES = 1;
    private static final int SPECIFIC_MESSAGE = 2;
    private static final int ALL_PEOPLE = 3;
    private static final int SPECIFIC_PEOPLE = 4;
    private static final int ALL_CALENDAR = 5;
    private static final int SPECIFIC_CALENDAR = 6;
    private static final int ALL_BROWSER = 7;
    private static final int SPECIFIC_BROWSER = 8;
    private static final int ALL_SEARCH = 9;
    private static final int SPECIFIC_SEARCH = 10;
    
    private static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(Custom.MAINURI, "mail", ALL_MESSAGES);
        URI_MATCHER.addURI(Custom.MAINURI, "mail/#", SPECIFIC_MESSAGE);
        URI_MATCHER.addURI(Custom.MAINURI, "contactStore", ALL_PEOPLE);
        URI_MATCHER.addURI(Custom.MAINURI, "contactStore/#", SPECIFIC_PEOPLE);
        URI_MATCHER.addURI(Custom.MAINURI, "calendar", ALL_CALENDAR);
        URI_MATCHER.addURI(Custom.MAINURI, "calendar/#", SPECIFIC_CALENDAR);
        URI_MATCHER.addURI(Custom.MAINURI, "browserStore", ALL_BROWSER);
        URI_MATCHER.addURI(Custom.MAINURI, "browserStore/#", SPECIFIC_BROWSER);
        URI_MATCHER.addURI(Custom.MAINURI, "searchStore", ALL_SEARCH);
        URI_MATCHER.addURI(Custom.MAINURI, "searchStore/#", SPECIFIC_SEARCH);
    }

    //public static final Uri CONTENT_URI = Uri.parse( "content://"+Custom.MAINURI+"/people");
    public static final Uri CONTENT_URI = Uri.parse( "content://"+Custom.MAINURI);
	
    //url text unique not null, urltext text, farkpicurl text, farkpictext text, commenturl text, commenttext text, description text
    
 

    // Database creation/version management helper.
    // Create it statically because we don't need to have customized instances.
    private static class DatabaseHelper extends SQLiteOpenHelper {

    	private static String TAG = "DataProviderDB";
    	private Custom mLog;
    	private Context mContext;
        //public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
			//super(context, name, factory, version);
			// TODO Auto-generated constructor stub
		//}
        DatabaseHelper(Context context) {
            super(context, Custom.DATABASE_NAME, null, DB_VERSION);
            mContext = context;
            mLog = new Custom(mContext, TAG + " DatabaseHelper() 52");
            mLog.i(TAG, "DatabaseHelper() 53");
        }

		@Override
        public void onCreate(SQLiteDatabase db){
			
			mLog.i(TAG,"DatabaseHelper onCreate() ++++++++++++++++++++++++");
			String[] sqlline = mLog.getContentSQL().split("\n");
    		for( int i = 0; i < sqlline.length; i++){
    			if( sqlline[i].length() == 0 ){ continue; }
    			
	        	try{
	        		db.execSQL( sqlline[i] );
	            } catch (SQLException e) {
	            	e.printStackTrace();
	            }
    		}
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        	mLog.e(TAG,"DISABLED DatabaseHelper onUpgrade() old("+oldVersion+") new("+newVersion+") ++++++++++++++++++++++++");
            //db.execSQL("DROP TABLE IF EXISTS " + Custom.DATABASE_TABLE_NAME);
            onCreate(db);
        }

    }
    
	
	@Override
	public String getType(Uri uri) {
		//mLog.w(TAG,"getType() uri("+uri+") lastsegment("+uri.getLastPathSegment()+")");
        switch (URI_MATCHER.match(uri)){
        case ALL_MESSAGES:
        	mLog.w(TAG,"getType() uri("+uri+") ALL MESSAGES");
            return "vnd.android.cursor.dir/mail"; // List of items.
        case SPECIFIC_MESSAGE:
        	mLog.w(TAG,"getType() uri("+uri+") Specific Message");
            return "vnd.android.cursor.item/mail";     // Specific item.
        case ALL_PEOPLE:
        	mLog.w(TAG,"getType() uri("+uri+") ALL People");
            return "vnd.android.cursor.dir/people"; // List of items.
        case SPECIFIC_PEOPLE:
        	mLog.w(TAG,"getType() uri("+uri+") Specific People");
            return "vnd.android.cursor.item/people";     // Specific item.
        case ALL_CALENDAR:
        	mLog.w(TAG,"getType() uri("+uri+") ALL Calendar");
            return "vnd.android.cursor.dir/calendar"; // List of items.
        case SPECIFIC_CALENDAR:
        	mLog.w(TAG,"getType() uri("+uri+") Specific Calendar");
            return "vnd.android.cursor.item/calendar";     // Specific item.
        default:
            return null;
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if( mDb == null ){
			return null;
		}
		long rowId = -1;
		
		
		String table = uri.getLastPathSegment();
		
		rowId = mDb.insert(table, "rawcontent", values);
		Uri newUri = Uri.withAppendedPath(uri, ""+rowId);
		//mLog.w(TAG,"insert()  newUri(" + newUri.toString() + ")");
       
       
		// Notify any listeners and return the URI of the new row.
		getContext().getContentResolver().notifyChange(Uri.withAppendedPath(CONTENT_URI,table), null);
       
		mLog.w(TAG,"insert() uri("+uri+") lastsegment/table("+uri.getLastPathSegment()+") notify("+Uri.withAppendedPath(CONTENT_URI,table)+") new("+newUri+")");
		
       /*/
       if( rowId > 100 ){
    	   int del = (int) (rowId - 100);
    	   mDb.execSQL("update " + DATABASE_TABLE_NAME + " set "+ CONTENT +" = \"\" where _id < "+del+" ");
       }
       //*/
       
		return newUri;

	}

	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;
	private Context mContext;
	@Override
	public boolean onCreate() {
		mContext = getContext();
		mLog = new Custom(this.getContext(), TAG + " onCreate() 130");
		mLog.w(TAG, "onCreate() +++++++++++++++++++++++++++++++++++++");
		mDbHelper = new DatabaseHelper(mContext);
		mSharedPreferences = mContext.getSharedPreferences("Preferences", mContext.MODE_WORLD_WRITEABLE);
		mPreferencesEditor = mSharedPreferences.edit();
		
		//final Context con = getContext();
        try{
        	mDb = mDbHelper.getWritableDatabase();
        	
            //mDb = mDbHelper.openDatabase(getContext(), DATABASE_NAME, null, DB_VERSION);
            //mLogger.info("RssContentProvider.onCreate(): Opened a database");
        } catch (Exception ex) {
        	mLog.e(TAG,"Failed to connected to Database, exception");
        	ex.printStackTrace();
              return false;
        }
        if(mDb == null){
        	mLog.e(TAG,"Failed to connected to Database, mDb null");
            return false;
        } else {
            return true;
        }

	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		if( mDb == null ){
			return null;
		}
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

		String table = "";
		table = uri.toString().replaceFirst(".*?" + Custom.MAINURI + "/", "").replaceFirst("/.*", "");
        // Set the table we're querying.
        qBuilder.setTables(table);
        
        long lastquery = mSharedPreferences.getLong("queryfor_"+table, -1 );
        if( lastquery < (System.currentTimeMillis() - 60 * 1000) ){
        	mLog.w(TAG, "query() refresh time uri("+uri.toString()+") table("+table+")");
        	mPreferencesEditor.putLong("queryfor_"+table, System.currentTimeMillis()).commit();
        }

        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
        int urimatcher = URI_MATCHER.match(uri); 
        if( urimatcher == SPECIFIC_MESSAGE
        		|| urimatcher == SPECIFIC_PEOPLE
        		|| urimatcher == SPECIFIC_CALENDAR
        		){
            qBuilder.appendWhere("_id=" + uri.getLastPathSegment()); // + uri.getPathLeafId());
        }

        mLog.e(TAG, "query() uri("+uri.toString()+") table("+table+") selection("+selection+") sort("+sortOrder+") urimatcher("+urimatcher+")");
        //Log.e(TAG, "query(2) uri("+uri.toString()+") table("+table+") urimatcher("+urimatcher+")");
        
        // Set sort order. If none specified, use default.
        if(TextUtils.isEmpty(sortOrder)){
            sortOrder = Custom.DEFAULT_SORT_ORDER;
        }
        if( selection == null ){
        	selection = "";
        	selectionArgs = new String[0];
        }

        // Make the query.
        Cursor c = qBuilder.query(mDb,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        
        c.setNotificationUri(getContext().getContentResolver(), uri);
        //getContext().getContentResolver().notifyChange(Uri.withAppendedPath(CONTENT_URI,table), null);
        
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		//mLog.w(TAG,"update() uri("+uri+") lastsegment("+uri.getLastPathSegment()+") selection("+selection+")");
		
		String table = "";
		table = uri.toString().replaceFirst(".*?" + Custom.MAINURI + "/", "").replaceFirst("/.*", "");
		
		int updateCount = 0;
		int urlmatcher = URI_MATCHER.match(uri);
		if( urlmatcher == ALL_MESSAGES || urlmatcher == ALL_PEOPLE || urlmatcher == ALL_CALENDAR){
			mLog.w(TAG, "update() uri("+uri.toString()+") table("+table+") selection("+selection+")");
			updateCount = mDb.update(table, values, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
		}else{
			String rowid = uri.getLastPathSegment();
			mLog.w(TAG, "update() uri("+uri.toString()+") table("+table+") selection("+selection+") rowid("+rowid+")");
			if( selection == null ){ selection = ""; }
			if( selection.length() == 0 ){ selection = "_id = " + rowid; }
			updateCount = mDb.update(table, values, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(CONTENT_URI,table), null);
		}
		
		// NOTE Argument checking code omitted. Check your parameters!
        

        // Notify any listeners and return the updated row count.
        //getContext().getContentResolver().notifyUpdate(uri, null);
        
		return updateCount;
	}
	

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		String table = "";
		table = uri.toString().replaceFirst(".*?" + Custom.MAINURI + "/", "").replaceFirst("/.*", "");
		
		int rowCount = 0;
		int urlmatcher = URI_MATCHER.match(uri);
		if( urlmatcher == ALL_MESSAGES || urlmatcher == ALL_PEOPLE || urlmatcher == ALL_CALENDAR){
			mLog.w(TAG,"delete() uri("+uri+") lastsegment/table("+table+") selection("+selection+")");			
			rowCount = mDb.delete(table, selection, selectionArgs);
			
	        // Notify any listeners and return the deleted row count.
	        getContext().getContentResolver().notifyChange(uri, null);
	        			
		}else{
			String rowid = uri.getLastPathSegment();
			mLog.w(TAG,"delete() uri("+uri+") lastsegment/table("+table+") id("+rowid+")");
			
			rowCount = mDb.delete(table, Custom.ID + " = " + rowid, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(CONTENT_URI,table), null);
		}
		return rowCount;
		

	}


	
}
