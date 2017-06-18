package com.jaycen.cybrepinger.api.entity;

import org.json.JSONObject;

import com.jaycen.cybrepinger.util.LinkClickContext;
import com.jaycen.cybrepinger.util.LogCategory;

public class TootContext {
	
	//	The ancestors of the status in the conversation, as a list of Statuses
	public TootStatus.List ancestors;
	
	// descendants	The descendants of the status in the conversation, as a list of Statuses
	public TootStatus.List descendants;
	
	public static TootContext parse( LogCategory log, LinkClickContext account,JSONObject src ){
		if( src==null) return null;
		try{
			TootContext dst = new TootContext();
			dst.ancestors = TootStatus.parseList( log, account,src.optJSONArray( "ancestors" ) );
			dst.descendants = TootStatus.parseList(log, account, src.optJSONArray( "descendants" ) );
			return dst;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e(ex,"TootContext.parse failed.");
			return null;
		}
	}
}
