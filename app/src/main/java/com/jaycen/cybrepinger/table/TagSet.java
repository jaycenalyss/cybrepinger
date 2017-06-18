package com.jaycen.cybrepinger.table;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import com.jaycen.cybrepinger.App1;
import com.jaycen.cybrepinger.util.LogCategory;

public class TagSet {
	
	private static final LogCategory log = new LogCategory( "TagSet" );
	
	private static final String table = "tag_set";
	private static final String COL_TIME_SAVE = "time_save";
	private static final String COL_TAG = "tag"; // タグ。先頭の#を含まない
	
	public static void onDBCreate( SQLiteDatabase db ){
		log.d( "onDBCreate!" );
		db.execSQL(
			"create table if not exists " + table
				+ "(_id INTEGER PRIMARY KEY"
				+ "," + COL_TIME_SAVE + " integer not null"
				+ "," + COL_TAG + " text not null"
				+ ")"
		);
		db.execSQL(
			"create unique index if not exists " + table + "_tag on " + table + "(" + COL_TAG + ")"
		);
		db.execSQL(
			"create index if not exists " + table + "_time on " + table + "(" + COL_TIME_SAVE + ")"
		);
	}
	
	public static void onDBUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ){
		if( oldVersion < 15 && newVersion >= 15 ){
			onDBCreate( db );
		}
	}
	
	public static void deleteOld( long now ){
		try{
			// 古いデータを掃除する
			long expire = now - 86400000L * 365;
			App1.getDB().delete( table, COL_TIME_SAVE + "<?", new String[]{ Long.toString( expire ) } );
			
		}catch( Throwable ex ){
			log.e( ex, "deleteOld failed." );
		}
	}
	
//	public static void save1( long now, String acct ){
//		try{
//
//			ContentValues cv = new ContentValues();
//			cv.put( COL_TIME_SAVE, now );
//			cv.put( COL_ACCT, acct );
//			App1.getDB().replace( table, null, cv );
//		}catch( Throwable ex ){
//			log.e( ex, "save failed." );
//		}
//	}
	
	public static void saveList( long now, String[] src_list, int offset, int length ){
		
		try{
			ContentValues cv = new ContentValues();
			cv.put( COL_TIME_SAVE, now );
			
			boolean bOK = false;
			SQLiteDatabase db = App1.getDB();
			db.execSQL( "BEGIN TRANSACTION" );
			try{
				for( int i = 0 ; i < length ; ++ i ){
					String acct = src_list[ i + offset ];
					cv.put( COL_TAG, acct );
					db.replace( table, null, cv );
				}
				bOK = true;
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "saveList failed." );
			}
			if( bOK ){
				db.execSQL( "COMMIT TRANSACTION" );
			}else{
				db.execSQL( "ROLLBACK TRANSACTION" );
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "saveList failed." );
		}
	}
	
	private static final String prefix_search_where = COL_TAG + " like ? escape '$'";

	private static final ThreadLocal< String[] > prefix_search_where_arg = new ThreadLocal< String[] >() {
		@Override protected String[] initialValue(){
			return new String[ 1 ];
		}
	};
	
	private static String makePattern( String src ){
		StringBuilder sb = new StringBuilder();
		for( int i = 0, ie = src.length() ; i < ie ; ++ i ){
			char c = src.charAt( i );
			if( c == '%' || c == '_' || c == '$' ){
				sb.append( '$' );
			}
			sb.append( c );
		}
		// 前方一致検索にするため、末尾に%をつける
		sb.append( '%' );
		return sb.toString();
	}
	
	@NonNull public static ArrayList< String > searchPrefix( @NonNull String prefix ,int limit){
		try{
			String[] where_arg = prefix_search_where_arg.get();
			where_arg[ 0 ] = makePattern( prefix );
			Cursor cursor = App1.getDB().query( table, null, prefix_search_where, where_arg, null, null, COL_TAG + " asc limit "+limit );
			if( cursor != null ){
				try{
					ArrayList< String > dst = new ArrayList<>( cursor.getCount() );
					int idx_acct = cursor.getColumnIndex( COL_TAG );
					while( cursor.moveToNext() ){
						dst.add( "#"+cursor.getString( idx_acct ) );
					}
					return dst;
				}finally{
					cursor.close();
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "searchPrefix failed." );
		}
		return new ArrayList<>();
	}
	
}
