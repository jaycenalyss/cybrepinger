package com.jaycen.cybrepinger.api.entity;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;

public class TootMention {
	
	public static class List extends ArrayList< TootMention > {
		
	}
	
	//	URL of user's profile (can be remote)
	public String url;
	
	//	The username of the account
	public String username;
	
	//	Equals username for local users, includes @domain for remote ones
	public String acct;
	
	//	Account ID
	public long id;
	
	public static TootMention parse( LogCategory log, JSONObject src ){
		if( src == null ) return null;
		try{
			TootMention dst = new TootMention();
			dst.url = Utils.optStringX( src, "url" );
			dst.username = Utils.optStringX( src, "username" );
			dst.acct = Utils.optStringX( src, "acct" );
			dst.id = src.optLong( "id" );
			return dst;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "TootMention.parse failed." );
			return null;
		}
	}
	
	@NonNull
	public static List parseList( LogCategory log, JSONArray array ){
		List result = new List();
		if( array != null ){
			int array_size = array.length();
			result.ensureCapacity( array_size );
			for( int i = 0 ; i < array_size ; ++ i ){
				JSONObject src = array.optJSONObject( i );
				if( src == null ) continue;
				TootMention item = parse( log, src );
				if( item != null ) result.add( item );
			}
		}
		return result;
	}
}
