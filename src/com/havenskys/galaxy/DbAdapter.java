package com.havenskys.galaxy;

import java.math.BigInteger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DbAdapter {
	public static final String TAG = "RSASMS DbAdapter";
	
	public static String DATABASE_NAME = "data";
    public static final String COL_ROWID = "_id";
    public static final String COL_TIMECREATE = "created";
    public static final String COL_TIMEUPDATE = "updated";
    public static final String COL_STATUS = "status";
    
    public static int DATABASE_TABLE_VERSION = 1; // if this changes it will
    public static String DATABASE_TABLE = "data";
    
    public static String TABLE_COLUMNS;
    public static String[] TABLE_UPDATES;
    public static String[] TABLE_INDEXLIST;
    public int ROW_COUNT;
	
	private Cursor mCursor;
	
	// Make this to be semi-persistent, flexible, adjustable, resetable, clean.
    private static String mTableCreate;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private Context mContext;
 
    private static class DatabaseHelper extends SQLiteOpenHelper {
    	
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_TABLE_VERSION);
        }
	    private int mTableExistsFailCount;
	    private String[] mTables; //"database.table"
	    //private String[] mColumns; //"database.table.column"
	    
	    //SQLiteDatabase mInsideDb;
	    public void onDestroy(){
	   //Log.e(TAG,"Interesting DatabaseHelper onDestroy() +++++++++++++++++++++++++++");
	    	//mInsideDb.close();
	    }
	    
		public void onCreate(SQLiteDatabase db) {
	//Log.i(TAG,"DatabaseHelper onCreate() +++++++++++++++++++++++++++");
			
			mTableExistsFailCount = 0;
			mTables = new String[] {"","","","","","","","","",""};
			//mInsideDb = db;
			createTable(db);
			//db.close();
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG,"DatabaseHelper onUpgrade() Upgrading table from v" + oldVersion + " to v" + newVersion + ".");
			try {
				Log.w(TAG,"DatabaseHelper onUpgrade() DROP TABLE IF EXISTS " + DATABASE_TABLE );
				db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE );
			//Log.w(TAG,"DatabaseHelper onUpgrade() Table dropped");
				Log.w(TAG,"DatabaseHelper onUpgrade() Creating " + mTableCreate);
				db.execSQL(mTableCreate);
			} catch (SQLiteException e){
				Log.e(TAG,"DatabaseHelper onUpgrade() Caught table creation conflict, it probably already exists.");
			}
			
		}
		

	    // April 11, 2009.  My parents arrived today, unannounced.  trilean off(0) false(1) true(2)
	    public boolean tableExists(String table, SQLiteDatabase db){
	    //Log.w(TAG,"DatabaseHelper tableExists() table("+table+") ++++++++++++++++");


	    	/*
	    	 
	    	 if( !db.isOpen() ){ return false; }
	    	 
	    	Cursor rowData;
	    	rowData = db.query("sqlite_master", new String[] {"name"}, "type = \"table\" AND name = \""+table+"\"", null, null, null, null);
	    	if( rowData == null ){ return false; }
	    	if( rowData.getPosition() < 0 ){ rowData.close(); return false;  }
	    	if( rowData.getString(0).equalsIgnoreCase(table) ){
	    		rowData.close();
	    		return true;
	    	}else {
	    		rowData.close();
	    		return false;
	    	}
	    	
	    	//*/

                try {
                        // I am not using 0 to represent stored data the 10 entries are from 1 to 10, which is an Array with the length of 11
                        //                          10      <      11
                        for( int tableCount = 1; tableCount < mTables.length; tableCount++ ){
                                if( mTables[tableCount].equalsIgnoreCase(DATABASE_NAME+"."+table) ){
                                       //Log.i(TAG,"DatabaseHelper tableExists() table("+table+") found with fast/cache search.");
                                        return true;
                                }
                        }
                } catch (NullPointerException e){
                        //Log.w(TAG,"DatabaseHelper tableExists() NullPointerException [ignore] " + e.getLocalizedMessage());
                        String nothing = "";
                        
                }

               //Log.i(TAG,"DatabaseHelper tableExists() Failcnt condition");
                // Failcnt condition
                int failcnt = 0;
                // I have declared the larger scope variable but I didn't set it to 0 in onCreate, this doesn't exist  as null or 0 is there a way to check this
                try {failcnt = Integer.parseInt(""+mTableExistsFailCount);} catch (NumberFormatException e){mTableExistsFailCount = 0;}


               //Log.i(TAG,"DatabaseHelper tableExists() isOpen()");
                if( !db.isOpen() ){
                       //Log.w(TAG,"DatabaseHelper tableExists("+table+") when DbAdapter is not Open");
                        return false;
                }

                Cursor rowData;
                String where;
                //where = "show tables";
                //where = "PRAGMA table_info(table-name)";
                where = "type = \"table\"";
               //Log.i(TAG,"DatabaseHelper tableExists() db.query("+where+")");
                rowData = db.query("sqlite_master", new String[] {"name"}, where, null, null, null, null);
                if( rowData == null ){
                        Log.e(TAG,"Selecting from sqlite_master resulted in null, returning false(proceed with Table Creation).");
                        return false;
                }
               if( !rowData.moveToFirst() ){
                        Log.e(TAG,"Selecting from sqlite_master resulted in no data, returning false(proceed with Table Creation).");
                        rowData.close();
                        return false;
                }
               //Log.i(TAG,"DatabaseHelper tableExists() test isClosed()");
                if( rowData.isClosed() ){
                       //Log.w(TAG,"DatabaseHelper tableExists() SQL Query closed: " + where);
                        rowData.close();
                        return false;
                }
                if( rowData.getCount() > 0 ){
                       //Log.i(TAG,"DatabaseHelper tableExists() count("+rowData.getCount()+")");
                }else{
                        // No tables exist
                       //Log.i(TAG,"DatabaseHelper tableExists() No tables exist");
                        rowData.close();
                        return false;
                }

                //Map<String><String> syncedTables = mDb.getSyncedTables();
                String[] columns = rowData.getColumnNames();
                String columnsText = "";
                for(int ci = 0; ci < columns.length; ci++){ columnsText += columns[ci]+", "; }
                //columnsText = columnsText.getChars(start, end, buffer, index)
                // tailing comma can be removed with a few commands, possibly too costly for a side process.
               //Log.w(TAG,"DatabaseHelper tableExists() show tables columns("+columnsText+"<--a tailing comma should exist there)");

                switch(failcnt){
                case 0:
                       //Log.w(TAG,"DatabaseHelper tableExists() initialize mTables["+(rowData.getCount()+1)+"]");
                        //mTables = new String[rowData.getCount()+1];
                        mTables = new String[101];
                        break;
                case 100:
                       //Log.i(TAG,"Interesting failcnt == 100, reseting.");
                        failcnt = 0;
                        rowData.close();
                        return false;
                }
                
               if( mTables.length == 0 ){
                        mTables = new String[100];
                        //rowData.close();
                        //return false;
                }

                mTableExistsFailCount = failcnt + 1;

                boolean tableExists = false;
                for(int rowCount = 0; rowCount < rowData.getCount(); rowCount++){
                        rowData.moveToPosition(rowCount);

                        //Review Condition
                        if( rowData.isClosed() ){ 
                        	Log.w(TAG,"Interesting, rowData closed while I was working on it."); 
                        }

                        String tableName = "";
                        tableName = rowData.getString(rowData.getColumnIndex("name"));

                        mTables[rowCount+1] = DATABASE_NAME + "." + tableName;
                        if( tableExists ){ continue; }
                       //Log.i(TAG,"DatabaseHelper tableExists() table: " + tableName);
                        if( tableName.equalsIgnoreCase(table) ){
                               //Log.i(TAG,"DatabaseHelper tableExists() Table Exists("+tableName+") count("+mTables.length+")");
                                tableExists = true;
                        }
                       //Log.i(TAG,"rowCount("+rowCount+")");
                }

                rowData.close();
               //Log.i(TAG,"DatabaseHelper tableExists() return " + (tableExists ? "true" : "false") );
                return tableExists;


			//*/
			
			
	    	
	    }
	    
		
		public void createTable(SQLiteDatabase db) {
	//Log.i(TAG,"createTable()");
			
			if( !tableExists(DATABASE_TABLE,db) ){
			//Log.i(TAG,"DatabaseHelper createTable() Table: " + DATABASE_TABLE);
		        // Create Table
		        
		        try {
		       //Log.i(TAG,"DatabaseHelper createTable() Table: " + mTableCreate );
		        	db.execSQL(mTableCreate);

		        	for(int i = 0; i < TABLE_INDEXLIST.length; i++){
	        			
	        			try {
	        				if( TABLE_INDEXLIST[i] != null ){
	        				if( TABLE_INDEXLIST[i].trim().length() > 0){
	        			//Log.i(TAG,"DatabaseHelper createTable() Table Index: " + TABLE_INDEXLIST[i]);
	        	        		db.execSQL(TABLE_INDEXLIST[i]);
	        				}
	        				}
	        	        } catch(SQLException e) {
	        	        Log.w(TAG,"Db error in index creation: " + e.getLocalizedMessage());
	        	        } finally {
	        	        	
	        	        }
	        		}
		        } catch(SQLiteException e) {
		        	if( e.getLocalizedMessage().contains("already exists") ){
		        	Log.w(TAG,"Not a mistake. :) " + e.getLocalizedMessage());
		        	}else{
		        	Log.w(TAG,"Db error in table creation: " + e.getLocalizedMessage());
		        	}
		        } finally {
		        	
		        }
		        
			}else{
				
			//Log.i(TAG,"DatabaseHelper createTable() Table Exists " + mTableCreate);
				
        		for(int i = 0; i < TABLE_UPDATES.length; i++){
        			
        			try {
        				if( TABLE_UPDATES[i] != null ){
        				if( TABLE_UPDATES[i].trim().length() > 0 ){
        			//Log.i(TAG,"DatabaseHelper createTable() Table Update: " + TABLE_UPDATES[i]);
        					db.execSQL(TABLE_UPDATES[i]);
        				}
        				}
        	        } catch(SQLiteException e) {
        	        Log.w(TAG,"Db error in table update: " + e.getLocalizedMessage());
        	        } finally {
        	        	
        	        }
        		}
        	}
		}

    }
    
    
    public void loadDb(String who, String which){
        //DbAdapter dataStore;
        // -----------------------------------------------
        // Data Storage
        //dataStore = new DbAdapter(mContext);
        
    	if( which == null ){
    		which = new String("browserStore,contactStore,searchStore");
    	}
    	
    	if( which.contains("browserStore") ){
        // browserStore
        this.prepare(who, mContext.getString(R.string.browserStore_database), mContext.getString(R.string.browserStore_table), 
        		new BigInteger(mContext.getString(R.string.browserStore_version)).intValue(), 
        		mContext.getString(R.string.browserStore_columns), mContext.getString(R.string.browserStore_updates).split(";"), 
        		mContext.getString(R.string.browserStore_indexlist).split(";"));
    	}
    	
    	if( which.contains("contactStore") ){
        // contactStore
        this.prepare(who, mContext.getString(R.string.contactStore_database), mContext.getString(R.string.contactStore_table), 
        		new BigInteger(mContext.getString(R.string.contactStore_version)).intValue(), 
        		mContext.getString(R.string.contactStore_columns), mContext.getString(R.string.contactStore_updates).split(";"), 
        		mContext.getString(R.string.contactStore_indexlist).split(";"));
    	}
    	
        if( which.contains("searchStore") ){
        // searchStore
        this.prepare(who, mContext.getString(R.string.searchStore_database), mContext.getString(R.string.searchStore_table), 
        		new BigInteger(mContext.getString(R.string.searchStore_version)).intValue(), 
        		mContext.getString(R.string.searchStore_columns), mContext.getString(R.string.searchStore_updates).split(";"), 
        		mContext.getString(R.string.searchStore_indexlist).split(";"));
        //*/
        }
        //return dataStore;
    }

    
    public DbAdapter(Context ctx){
    	ROW_COUNT = 0;
    	this.mContext = ctx;
    }


    public void prepare(String who, String database, String table, int version, String createcolumns, String[] updatesql, String[] indexsql) {
    	DATABASE_NAME = database;
        DATABASE_TABLE = table;
        DATABASE_TABLE_VERSION = version;
        TABLE_COLUMNS = createcolumns;
        TABLE_UPDATES = updatesql;
        TABLE_INDEXLIST = indexsql;
        
		String d = "create table "+DATABASE_TABLE+" (";
		d += "_id integer primary key autoincrement, ";
		d += TABLE_COLUMNS;
		d += ", " + COL_TIMECREATE + " DATE";
		d += ", " + COL_TIMEUPDATE + " DATE";
		d += ", " + COL_STATUS + " INTEGER"; // < 0 deleted(value * -1), 1 active(created), ++ per update 
        d += ");";
        mTableCreate = d;
	//Log.i(TAG,"DatabaseHelper createTable() Table " + mTableCreate);

        
        this.prepare(who);
	}
    
	public DbAdapter prepare(String who) throws SQLException {
	
    	if( mContext != null ){
    		//if( tableExists(DATABASE_TABLE) )
    	
	    	for( int attempt = 1; attempt <= 2; attempt++){
	    		//Log.i(TAG,"prepare() DatabaseHelper()");
	    		Log.i(TAG,"prepare() DatabaseHelper.getWritableDatabase() for " + who);
	    		try {
	    			mDbHelper = new DatabaseHelper(mContext);
	    			mDb = mDbHelper.getWritableDatabase();
	    			break;
	    		}catch(SQLiteDatabaseCorruptException e){
	    			Log.e(TAG,"SQLite Database CORRUPT Exception");
	    			e.printStackTrace();
	    		}
	    	}
    		
    		//if( !tableExists(DATABASE_TABLE) ){
    			//
    		//}
    		
    		if( !mDbHelper.tableExists(DATABASE_TABLE,mDb) ){
    		//Log.w(TAG,"prepare() createTable " + DATABASE_TABLE);
    			mDbHelper.createTable(mDb);
    		}
    		
    		//}else{
    		//	Log.w(TAG,"prepare() table already exists " + DATABASE_TABLE);
    		//}
    		mDb.close();
    		//mDb = null;
    	}
    //Log.i(TAG,"prepare() return");
        return this;
    }

    
    public void close() {
    	if( mDb != null ){ mDb.releaseReference(); mDb.close(); }
        //if ( mDbHelper != null ){ mDbHelper.close(); }
    }
    
    //public void execSQL(String sql) throws SQLException{
    	//mDb.execSQL(sql);
    //}
    /*
    public boolean tableExists(String table){
    	if( mDb.findEditTable(table).length() > 0 ){
    		return true;
    	}else{
    		return false;
    	}
    }
	/**/
    
    // type should be text, integer, double, or date, please
    public void addColumn(String table, String column, String type) throws SQLException {
   //Log.w(TAG,table + " addColumn("+column+","+type+")");
    	//if( !columnExists(table, column) ){
    	mDb = mDbHelper.getWritableDatabase();
    	mDb.execSQL("alter table "+table+" add column " + column + " " + type);
    	mDb.close();
    	//}
    }

    public long addEmptyEntry(String table){
   //Log.w(TAG,table + " addEmptyEntry()");
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	//cv.put(COL_TIMECREATE, System.currentTimeMillis());
    	cv.put(COL_TIMECREATE, System.currentTimeMillis());
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
    	cv.put(COL_STATUS, 1);
    	long rowId = 0;
    	rowId = mDb.insert(table, null, cv);
    	mDb.close();
    	return rowId;
    }

    public long addEntry(String table, String col, int value){
    	mDb = mDbHelper.getWritableDatabase();
    	long rowId = 0;
    	rowId = addEntry(table, new String[] {col}, new String[] {""+value});
    	mDb.close();
    	return rowId;
    }
    public long addEntry(String table, String col, String value){
    	return addEntry(table, new String[] {col}, new String[] {value});
    }
    public long addEntry(String table, String[] col, String[] values){
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " addEntry()");
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    		values[i] = values[i].trim();
    		cv.put(col[i], values[i]);
    //Log.w(TAG,table + " addEntry("+table+","+col[i]+","+values[i]+")");
    	}
    	cv.put(COL_TIMECREATE, System.currentTimeMillis());
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
    	cv.put(COL_STATUS, 1);
    	long rowId = 0;
    	rowId = mDb.insert(table, null, cv);
    	mDb.close(); return rowId;
    }
        
    public boolean deleteEntry(String table, long rowId){
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " deleteEntry("+table+","+rowId+")");
    	ContentValues cv = new ContentValues();
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
    	cv.put(COL_STATUS, 0);
    	boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	//return mDb.delete(table, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }

    
    public boolean updateEntry(String table, long rowId, String[] col, String[] values) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    		values[i] = values[i].trim();
    //Log.w(TAG,table + " updateEntryString("+rowId+","+col[i]+","+values[i]+")s");
    		cv.put(col[i], values[i]);
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }
    
    public boolean updateEntry(String table, long rowId, String[] col, int[] values) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    //Log.w(TAG,table + " updateEntryInteger("+rowId+","+col[i]+","+values[i]+")s");
    		cv.put(col[i], values[i]);
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;

    }
    public boolean updateEntry(String table, long rowId, String[] col, long[] values) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    //Log.w(TAG,table + " updateEntryLong("+rowId+","+col[i]+","+values[i]+")s");
    		cv.put(col[i], values[i]);
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }
    public boolean updateEntry(String table, String[] col, long[] values, String where) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    //Log.w(TAG,table + " updateEntryLong("+where+","+col[i]+","+values[i]+")s");
    		cv.put(col[i], values[i]);
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, where, null) > 0;
    	mDb.close(); return queryStatus;
    }
    public boolean updateEntry(String table, String[] col, int[] values, String where) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    //Log.w(TAG,table + " updateEntryLong("+where+","+col[i]+","+values[i]+")s");
    		cv.put(col[i], values[i]);
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, where, null) > 0;
    	mDb.close(); return queryStatus;
    }
    public boolean updateEntry(String table, long rowId, String[] col, double[] values) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    //Log.w(TAG,table + " updateEntryDouble("+rowId+","+col[i]+","+values[i]+")s");
    		cv.put(col[i], values[i]);
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }
    public boolean updateEntry(String table, long rowId, String column, String value) {
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " updateEntryString("+rowId+","+column+","+value+")");
    	value = value.trim();
        ContentValues cv = new ContentValues();
        cv.put(column, value);
        cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;        
    	mDb.close(); return queryStatus;
    }
    public boolean updateEntry(String table, String column, String value, String where) {
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " updateEntryString("+where+","+column+","+value+")");
    	value = value.trim();
        ContentValues cv = new ContentValues();
        cv.put(column, value);
        cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, where, null) > 0;        
    	mDb.close(); return queryStatus;
    }
    public boolean updateEntry(String table, long rowId, String column, int value) {
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " updateEntryInteger("+rowId+","+column+","+value+")");
        ContentValues cv = new ContentValues();
        cv.put(column, value);
        cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }    
    public boolean updateEntry(String table, String column, int value, String where) {
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " updateEntryInteger("+where+","+column+","+value+")");
        ContentValues cv = new ContentValues();
        cv.put(column, value);
        cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, where, null) > 0;
    	mDb.close(); return queryStatus;
    }    
    public boolean updateEntry(String table, long rowId, String column, double value) {
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " updateEntryDouble("+rowId+","+column+","+value+")");
        ContentValues cv = new ContentValues();
        cv.put(column, value);
        cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }

    public int getInteger(String table, int rowId, String columnName) {
    	return getInteger(table, rowId, new String[] {columnName} )[0];
    }

    public int[] getInteger(String table, int rowId, String[] columnName) {
		Cursor rowData = null;
		int[] intArray = new int[columnName.length];
		
		//Log.w(TAG,"setContact("+contactid+") getEntry messageId("+mMessageId+") recipient("+mContactNumber[contactid]+")");
		String columnList = "";
		int len = columnName.length;
		//for(int i = 0; i < len; i++){
			//columnList += columnName[i] + "";
			//if( i < len-1 ){
				//columnList += ",";
			//}
		//}
		try {
	//Log.w(TAG,table + " GET  " + columnList + " where _id = "+rowId+".");
			rowData = getEntry(table,columnName,"_id = " + rowId );
			
	//Log.i(TAG,"Checking for null.");
			if( rowData == null ) {
				Log.e(TAG,table+" was null, returning 0."); 
				return new int[] {0};
				}
			
	//Log.i(TAG,"Checking for existence of data.");
			if( !rowData.moveToFirst() ) {
				Log.e(TAG,table+" let us know no records exist, returning 0."); 
				rowData.close(); 
				return new int[] {0};
			}
			
			
			for(int i = 0; i < rowData.getColumnCount(); i++){
		//Log.i(TAG,"Getting "+columnName[i]);
				//intArray[i] = rowData.getInt(rowData.getColumnIndex(columnName[i]));
				intArray[i] = rowData.getInt(i);
			}
	
		} catch (ArrayIndexOutOfBoundsException e){
		Log.e(TAG,"ArrayIndexOutOfBoundsException 968: " + e.getLocalizedMessage());
		} catch (CursorIndexOutOfBoundsException e){
		Log.e(TAG,"CursorIndexOutOfBoundsException 968: " + e.getLocalizedMessage());
		} finally {
			if( rowData != null ){ rowData.close(); }
		}
		
		return intArray;
	}


    public String getString(String table, long id, String columnName) {
    	return getString(table, id, new String[] {columnName} )[0];
    }
    
    public String[] getString(String table, long rowId, String[] columnName) {
		Cursor rowData = null;
		String[] stringArray = new String[columnName.length];
		
		String columnList = "";
		int len = columnName.length;
		//for(int i = 0; i < len; i++){
			//columnList += columnName[i] + "";
			//if( i < len-1 ){
				//columnList += ",";
			//}
		//}
		try {
	//Log.w(TAG,table + " GET  " + columnList + " where _id = "+rowId+".");
			rowData = getEntry(table,columnName,"_id = " + rowId );
			
	//Log.i(TAG,"Checking for null.");
			if( rowData == null ) {
				Log.e(TAG,table+" was null, returning 0."); 
				return new String[] {""};}
			
	//Log.i(TAG,"Checking for existence of data.");
			if( !rowData.moveToFirst() ) {
				Log.e(TAG,table+" let us know no records exist, returning 0."); 
				rowData.close(); return new String[] {""};};
			
			
			for(int i = 0; i < rowData.getColumnCount(); i++){
		//Log.i(TAG,"Getting "+columnName[i]);
				//intArray[i] = rowData.getInt(rowData.getColumnIndex(columnName[i]));
				stringArray[i] = rowData.getString(i);
			}
	
		} catch (ArrayIndexOutOfBoundsException e){
		Log.e(TAG,"ArrayIndexOutOfBoundsException 968: " + e.getLocalizedMessage());
		} catch (CursorIndexOutOfBoundsException e){
		Log.e(TAG,"CursorIndexOutOfBoundsException 968: " + e.getLocalizedMessage());
		} finally {
			if( rowData != null ){ rowData.close(); }
		}
		
		return stringArray;
	}

    
    
    public int getId(String table, String where) {
    	
    //Log.w(TAG,"getId() ++++++++");
    	
		Cursor rowData = null;
		int rowId = 0;
		
		try {
	//Log.w(TAG,table + " GET _id where " + where + ".");
			rowData = getEntry(table,new String[] {"_id"}, where );
		//Log.i(TAG,"Checking for null.");
			if( rowData == null ) {
				Log.e(TAG,table+" was null, returning 0."); 
				return 0;
			}			
			if( !rowData.moveToFirst() ){
			//Log.w(TAG,table+" was empty, returning 0.");
				rowData.close();
				return 0;
			}
			rowId = rowData.getInt(0);
			
		} catch (ArrayIndexOutOfBoundsException e){
		Log.e(TAG,"ArrayIndexOutOfBoundsException 968: " + e.getLocalizedMessage());
		} catch (CursorIndexOutOfBoundsException e){
		Log.e(TAG,"CursorIndexOutOfBoundsException 968: " + e.getLocalizedMessage());
		} finally {
		
		}
		
	//Log.w(TAG,"getId() checking");
	//Log.i(TAG,"Checking for null.");
		if( rowData == null ) {
			Log.e(TAG,table+" was null, returning 0."); 
			return 0;
		}
		
	//Log.w(TAG,"getId() checking closed");
		if( rowData.isClosed() ){
		//Log.w(TAG,table+" was closed, returning 0.");
			return 0;
		}
		
		
		/*
		Log.w(TAG,"getId() checking requery");
		if( !rowData.requery() ){
			Log.w(TAG,table+" wasn't able to be requery(), returning 0.");
			rowData.close();
			return 0;
		}
		//*/
		
		
		
		if( rowData != null ){ rowData.close(); }

		
		return rowId;
	}
	
    public int getCount(String table, String where) {
    	if( table == null || where == null ){
    	Log.e(TAG,"Interesting getCount missing args table("+table+") where("+where+")");
    		return -3;
    	}
    	
    //Log.w(TAG,"getCount("+table+","+where+")");
    	
		int count = 0;
		Cursor rowData = null;
        try {
       //Log.i(TAG, table + " COUNT " + where + "");
        	rowData = getEntry(table, new String[] {"_id"}, where);

        //Log.w(TAG,"getCount() pre return");
            if(rowData == null){ return -1;}
            if(rowData.isClosed()){ rowData.close(); return -3;}
        	if(!rowData.moveToFirst()){ rowData.close(); return -2;}
        	count = rowData.getCount();
        //Log.w(TAG,"getCount() return");

        	//} catch (ArrayIndexOutOfBoundsException e){
        	//Log.e(TAG,"ArrayIndexOutOfBoundsException error getting waitingMessageCount " + e.getLocalizedMessage());
        //} catch (NullPointerException e){
        	//Log.e(TAG,"NullPointerException (does this really mean empty?) error getting waitingMessageCount " + e.getLocalizedMessage());
        } catch (SQLiteException e){
        Log.e(TAG,"SQLiteException error getting Count " + e.getLocalizedMessage());
        } finally {
        	if( rowData != null ){ rowData.close(); }
        }
        return count;
	}
    
    

    
    public boolean updateEntryDate(String table, long rowId, String[] col) {
    	mDb = mDbHelper.getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	for( int i = 0; i < col.length; i++){
    //Log.w(TAG,table + " updateEntryDates("+rowId+","+col[i]+")");
    		//cv.put(col[i], System.currentTimeMillis());
    		cv.put(col[i], System.currentTimeMillis());
    	}
    	cv.put(COL_TIMEUPDATE, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }

    
    public boolean updateEntryDate(String table, long rowId, String col) {
    	mDb = mDbHelper.getWritableDatabase();
   //Log.w(TAG,table + " updateEntryDate("+rowId+","+col+")");
    	ContentValues cv = new ContentValues();
    	cv.put(col, System.currentTimeMillis());
    	//cv.put(col, System.currentTimeMillis());
        boolean queryStatus = false; queryStatus = mDb.update(table, cv, COL_ROWID + "=" + rowId, null) > 0;
    	mDb.close(); return queryStatus;
    }

    
    
    // Get Single row by ID
    public Cursor getEntry(String table, long rowId) throws SQLException {
    	
   //Log.w(TAG,table + " getEntryById("+rowId+")");
    	return detailQuery(table, null, "_id = "+rowId, null, null, null, null, null);
    }
    public Cursor getEntry(String table, long rowId, String column) throws SQLException {
   //Log.w(TAG,table + " getEntryById("+rowId+")");
    	return detailQuery(table, new String[] {column}, "_id = "+rowId, null, null, null, null, null);
    }
    public Cursor getEntry(String table, long rowId, String[] column) throws SQLException {
   //Log.w(TAG,table + " getEntryById("+rowId+")");
    	return detailQuery(table, column, "_id = "+rowId, null, null, null, null, null);
    }
    
    // Get All Entries
    public Cursor getAllEntries(String table) {
   //Log.w(TAG,table + " getAllEntries()");
    	return detailQuery(table, null, COL_STATUS + " > 0", null, null, null, "DESC " + COL_TIMEUPDATE, null);
    }
    
    public Cursor getAllEntries(String table, String[] columns) {
   //Log.w(TAG,table + " getAllEntries()");
    	return detailQuery(table, columns, COL_STATUS + " > 0", null, null, null, "DESC " + COL_TIMEUPDATE, null);
    }
        
    // Get Multiple rows by column and value
    public Cursor getEntry(String table, String column, String value, int limit) throws SQLException {
   //Log.w(TAG,table + " getEntryByColumnString("+column+","+value+","+limit+")l");
    	return detailQuery(table, null, column + " = \"" + value + "\"", null, null, null, COL_TIMEUPDATE+ " DESC", ""+ limit);
    }
    public Cursor getEntry(String table, String column, String value) throws SQLException {
   //Log.w(TAG,table + " getEntryByColumnString("+column+","+value+")");
    	return detailQuery(table, null, column + " = \"" + value + "\"", null, null, null, COL_TIMEUPDATE+ " DESC", null);
    }
    public Cursor getEntry(String table, String[] column, String[] value) throws SQLException {
    	String where = "";
    	for( int i = 0; i < column.length; i++){
    //Log.w(TAG,table + " getEntryByColumn("+column[i]+","+value[i]+")");
    		where += " AND " + column[i] + " = \"" + value[i] + "\"";
    	}
    														//AND in where
    	return detailQuery(table, null, where, null, null, null, COL_TIMEUPDATE+ " DESC", null);
    }
    public Cursor getEntry(String table, String[] column, int[] value) throws SQLException {
    	String where = "";
    	for( int i = 0; i < column.length; i++){
    //Log.w(TAG,table + " getEntryByColumn("+column[i]+","+value[i]+")");
    		where += " AND " + column[i] + " = " + value[i] + "";
    	}
    													//  AND in where
    	return detailQuery(table, null, where, null, null, null, COL_TIMEUPDATE + " DESC", null);
    }
    public Cursor getEntry(String table, String where) throws SQLException {
   //Log.w(TAG,table + " getEntry table("+table+") where("+where+")");
    	return detailQuery(table, null, where, null, null, null, COL_TIMEUPDATE + " DESC", null);
    }
    public Cursor getEntryS(String table, String column, String where) throws SQLException {
   //Log.w(TAG,table + " getEntry table("+table+") column("+column+") where("+where+")");
    	return detailQuery(table, new String[] {column}, where, null, null, null, COL_TIMEUPDATE + " DESC", null);
    }
    public Cursor getEntry(String table, String[] columns, String where) throws SQLException {
   //Log.w(TAG,table + " getEntry table("+table+") column("+columns.length+") where("+where+")");
    	return detailQuery(table, columns, where, null, null, null, COL_TIMEUPDATE + " DESC", null);
    }
    public Cursor getEntry(String table, String[] columns, String where, int limit) throws SQLException {
   //Log.w(TAG,table + " getEntry table("+table+") column("+columns.length+")() where("+where+") limit("+limit+")");
    	return detailQuery(table, columns, where, null, null, null, COL_TIMEUPDATE + " DESC", "" + limit);
    }

    private String whereCleanup(String where) {
    	String newWhere = "";
    	where.replaceAll(COL_STATUS+" =", COL_STATUS+"=");
    	where.replaceAll(COL_STATUS+"= ", COL_STATUS+"=");
    	if( where.trim().length() == 0 || !(where.contains(COL_STATUS+"=0") || where.contains(COL_STATUS+"=-") ) ){
    		newWhere = COL_STATUS + " > 0";
    	}
    	if( where.trim().length() > 0 && newWhere != "" ){
    //Log.i(TAG,"where("+where+") is > 0("+where.trim().length()+")");
			newWhere += " AND (" + where + ")";
    		//newWhere += " AND " + where + "";
		}
    	return newWhere;
    }
    
    public Boolean isOpen(){
    	Boolean returnBool = false;

    	if( mDb != null ){
	    	if ( mDb.isOpen() ){
	    		returnBool = true;
	    	}
    	}
    	
    	return returnBool;
    }
    
     
    public Cursor detailQuery(String table, String[] columns, String sqlWhere, String[] sqlWhereArgs, String groupBy, String having, String orderBy, String limit) throws SQLException {
    	
    	
    //Log.w(TAG,table + " detailQuery() columns()() selection("+whereCleanup(sqlWhere)+") selectionArgs()() groupBy("+groupBy+") having("+having+") orderBy("+orderBy+") limit("+limit+")");
    	
   //Log.w(TAG,table + " detailQuery()");
    	mDb = mDbHelper.getWritableDatabase();
    	/*try {
    	
    	} catch (NullPointerException e){
    	Log.e(TAG,"Caught Null Point Exception in arg output.");
    		return null;
    	}*/
    	mCursor = null;
    	try {
	        mCursor = mDb.query(true, table, columns, whereCleanup(sqlWhere), sqlWhereArgs, groupBy, having, orderBy, limit);
	        /**//*
	        if (mCursor != null) {
	        	Log.i(TAG,"db replied correctly");
	            if( !mCursor.requery() ){
	            Log.w(TAG,"detailQuery() replied with no data, returning null (First Checked Condition)."); // maybe masking usable condition
	            	mCursor.close();
	            	return null;
	            }
	        }
	        /**/
	    	if( mCursor == null ){ 
	        	Log.e(TAG,"detailQuery() replied with null, returning null."); 
	    		return null;
	    	}
	    	ROW_COUNT = mCursor.getCount();

    	} catch (SQLiteException e) {
    Log.e(TAG,"SQL Failure with this query. " + e.getLocalizedMessage() );
    	} catch (CursorIndexOutOfBoundsException e2) {
    Log.e(TAG,"Cursor Out of bounds, returning null " + e2.getLocalizedMessage() );
    		mDb.close();
    		return null;
    	} catch (ArrayIndexOutOfBoundsException e2) {
    	Log.e(TAG,"Array Out of bounds, returning null " + e2.getLocalizedMessage() );
    		mDb.close();
    		return null;
    	} catch (NullPointerException e4) {
    	Log.e(TAG,"NullPointerException, returning null " + e4.getLocalizedMessage() );
    		mDb.close();
    		return null;
    	} finally {
    		// May 2 2009, this is here to stop the error log entry: Invalid statement in fillWindow() 
  //  		if( mCursor.getCount() > 0 ){
    			//Log.w(TAG,"NO ERROR " + );
    //		}
    		//Log.i(TAG,"detailQuery() mCursor returning with " + mCursor.getCount() + " Records");
    		//String nothing = "";
        	if( mDb.isOpen() ) { mDb.close(); }
        	
    	
    	}

    	//Log.w(TAG,table + " detailQuery() return cursor");
        return mCursor; // I could provide count, and other services to this cursor, such as column(s) types
    }


	

}


















