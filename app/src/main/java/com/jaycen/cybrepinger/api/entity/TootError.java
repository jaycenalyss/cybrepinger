package com.jaycen.cybrepinger.api.entity;

import org.json.JSONObject;

import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;

public class TootError {
	
	//	A textual description of the error
	public String error;
	
	public static TootError parse( LogCategory log, JSONObject src ){
		if( src==null ) return null;
		try{
			TootError dst = new TootError();
			dst.error = Utils.optStringX( src, "error" );
			return dst;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e(ex,"TootError.parse failed.");
			return null;
		}
	}
	
}
