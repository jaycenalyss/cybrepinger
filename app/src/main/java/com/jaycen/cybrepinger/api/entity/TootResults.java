package com.jaycen.cybrepinger.api.entity;

import org.json.JSONObject;

import java.util.ArrayList;

import com.jaycen.cybrepinger.util.LinkClickContext;
import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;

public class TootResults {
	//	An array of matched Accounts
	public TootAccount.List accounts;
	
	//	An array of matched Statuses
	public TootStatus.List statuses;
	
	//	An array of matched hashtags, as strings
	public ArrayList< String > hashtags;
	
	public static TootResults parse( LogCategory log, LinkClickContext account, JSONObject src ){
		try{
			if( src == null ) return null;
			TootResults dst = new TootResults();
			dst.accounts = TootAccount.parseList( log, account, src.optJSONArray( "accounts" ) );
			dst.statuses = TootStatus.parseList( log, account, src.optJSONArray( "statuses" ) );
			dst.hashtags = Utils.parseStringArray( log, src.optJSONArray( "hashtags" ) );
			return dst;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "TootResults.parse failed." );
			return null;
		}
	}
}
