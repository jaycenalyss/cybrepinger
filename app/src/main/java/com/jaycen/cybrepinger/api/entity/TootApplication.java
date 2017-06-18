package com.jaycen.cybrepinger.api.entity;

import org.json.JSONObject;

import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;

public class TootApplication {
	public String name;
	public String website;
	
	public static TootApplication parse( LogCategory log, JSONObject src ){
		if( src == null ) return null;
		try{
			TootApplication dst = new TootApplication();
			dst.name = Utils.optStringX( src, "name" );
			dst.website = Utils.optStringX( src, "website" );
			return dst;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "TootApplication.parse failed." );
			return null;
		}
	}
}
