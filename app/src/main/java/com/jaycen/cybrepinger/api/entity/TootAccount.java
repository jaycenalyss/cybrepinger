package com.jaycen.cybrepinger.api.entity;

import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.TextUtils;

import com.jaycen.cybrepinger.util.Emojione;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.jaycen.cybrepinger.util.HTMLDecoder;
import com.jaycen.cybrepinger.util.LinkClickContext;
import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;

public class TootAccount {
	
	
	public static class List extends ArrayList< TootAccount > {
		
	}
	
	//	The ID of the account
	public long id;
	
	// 	The username of the account
	public String username;
	
	//	Equals username for local users, includes @domain for remote ones
	public String acct;
	
	//	The account's display name
	public CharSequence display_name;
	
	//Boolean for when the account cannot be followed without waiting for approval first
	public boolean locked;
	
	//	The time the account was created
	// ex: "2017-04-13T11:06:08.289Z"
	public String created_at;
	
	//	The number of followers for the account
	public long followers_count;
	
	//The number of accounts the given account is following
	public long following_count;
	
	//	The number of statuses the account has made
	public long statuses_count;
	
	// Biography of user
	// 説明文。改行は\r\n。リンクなどはHTMLタグで書かれている
	public Spannable note;
	
	//URL of the user's profile page (can be remote)
	// https://mastodon.juggler.jp/@tateisu
	public String url;
	
	//	URL to the avatar image
	public String avatar;
	
	//	URL to the avatar static image (gif)
	public String avatar_static;
	
	//URL to the header image
	public String header;
	
	//	URL to the header static image (gif)
	public String header_static;
	
	public long time_created_at;
	
	public TootAccount(){
		
	}
	
	// 疑似アカウントの作成
	public TootAccount( String username ){
		this.acct = username;
		this.username = username;
	}
	
	public static TootAccount parse( LogCategory log, LinkClickContext account, JSONObject src, TootAccount dst ){
		if( src == null ) return null;
		try{
			dst.id = src.optLong( "id",-1L );
			dst.username = Utils.optStringX( src, "username" );
			dst.acct = Utils.optStringX( src, "acct" );
			if( dst.acct == null){
				dst.acct = "?@?";
			}
			
			String sv = Utils.optStringX( src, "display_name" );
			if( TextUtils.isEmpty( sv ) ){
				dst.display_name = dst.username;
			}else{
				dst.display_name = filterDisplayName(sv);
			}
			
			dst.locked = src.optBoolean( "locked" );
			dst.created_at = Utils.optStringX( src, "created_at" );
			dst.followers_count = src.optLong( "followers_count" );
			dst.following_count = src.optLong( "following_count" );
			dst.statuses_count = src.optLong( "statuses_count" );
			dst.note = HTMLDecoder.decodeHTML( account,Utils.optStringX( src, "note" ) ,true,null);
			dst.url = Utils.optStringX( src, "url" );
			dst.avatar = Utils.optStringX( src, "avatar" ); // "https:\/\/mastodon.juggler.jp\/system\/accounts\/avatars\/000\/000\/148\/original\/0a468974fac5a448.PNG?1492081886",
			dst.avatar_static = Utils.optStringX( src, "avatar_static" ); // "https:\/\/mastodon.juggler.jp\/system\/accounts\/avatars\/000\/000\/148\/original\/0a468974fac5a448.PNG?1492081886",
			dst.header = Utils.optStringX( src, "header" ); // "https:\/\/mastodon.juggler.jp\/headers\/original\/missing.png"
			dst.header_static = Utils.optStringX( src, "header_static" ); // "https:\/\/mastodon.juggler.jp\/headers\/original\/missing.png"}
			
			dst.time_created_at = TootStatus.parseTime( log, dst.created_at );
			
			return dst;
			
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "TootAccount.parse failed." );
			return null;
		}
	}
	
	
	public static TootAccount parse( LogCategory log, LinkClickContext account,JSONObject src ){
		return parse( log, account, src, new TootAccount() );
	}
	
	@NonNull public static List parseList( LogCategory log, LinkClickContext account, JSONArray array ){
		List result = new List();
		if( array != null ){
			int array_size = array.length();
			result.ensureCapacity( array_size );
			for( int i=0;i<array_size;++i){
				JSONObject src = array.optJSONObject( i );
				if( src == null ) continue;
				TootAccount item = parse( log, account,src );
				if( item != null ) result.add( item );
			}
		}
		return result;
	}
	
	private static CharSequence filterDisplayName( String sv ){

		// decode HTML entity
		sv = HTMLDecoder.decodeEntity(sv );

		// sanitize LRO,RLO
		sv = Utils.sanitizeBDI( sv);

		// decode emoji code
		return Emojione.decodeEmoji( sv ) ;
	}
	
}
