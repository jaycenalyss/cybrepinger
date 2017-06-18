package com.jaycen.cybrepinger.api.entity;

import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jaycen.cybrepinger.util.HTMLDecoder;
import com.jaycen.cybrepinger.util.LinkClickContext;
import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;
import com.jaycen.cybrepinger.util.WordTrieTree;

public class TootStatus extends TootId {
	
	public static class List extends ArrayList< TootStatus > {
		
		public List(){
			super();
		}
		
		public List( int capacity ){
			super( capacity );
		}
	}
	
	//	The ID of the status
	// TootId public long id;
	
	// A Fediverse-unique resource ID
	public String uri;
	
	//URL to the status page (can be remote)
	public String url;
	
	// The TootAccount which posted the status
	public TootAccount account;
	
	//	null or the ID of the status it replies to
	public String in_reply_to_id;
	
	//	null or the ID of the account it replies to
	public String in_reply_to_account_id;
	
	//	null or the reblogged Status
	public TootStatus reblog;
	
	//	Body of the status; this will contain HTML (remote HTML already sanitized)
	public String content;
	
	//	The time the status was created
	public String created_at;
	
	//The number of reblogs for the status
	public long reblogs_count;
	
	//The number of favourites for the status
	public long favourites_count;
	
	//	Whether the authenticated user has reblogged the status
	public boolean reblogged;
	
	//	Whether the authenticated user has favourited the status
	public boolean favourited;
	
	//Whether media attachments should be hidden by default
	public boolean sensitive;
	
	//If not empty, warning text that should be displayed before the actual content
	public String spoiler_text;
	
	//One of: public, unlisted, private, direct
	public String visibility;
	public static final String VISIBILITY_PUBLIC = "public";
	public static final String VISIBILITY_UNLISTED = "unlisted";
	public static final String VISIBILITY_PRIVATE = "private";
	public static final String VISIBILITY_DIRECT = "direct";
	
	//	An array of Attachments
	public TootAttachment.List media_attachments;
	
	//	An array of Mentions
	public TootMention.List mentions;
	
	//An array of Tags
	public TootTag.List tags;
	
	//Application from which the status was posted
	public TootApplication application;
	
	public long time_created_at;
	
	public Spannable decoded_content;
	public Spannable decoded_spoiler_text;
	// public Spannable decoded_tags;
	public Spannable decoded_mentions;
	
	public JSONObject json;
	
	public boolean conversation_main;
	
	public static TootStatus parse( LogCategory log, LinkClickContext account, JSONObject src ){
		
		if( src == null ) return null;
		
		try{
			TootStatus status = new TootStatus();
			status.json = src;
			//	log.d( "parse: %s", src.toString() );
			status.id = src.optLong( "id" );
			status.uri = Utils.optStringX( src, "uri" );
			status.url = Utils.optStringX( src, "url" );
			status.account = TootAccount.parse( log, account, src.optJSONObject( "account" ) );
			status.in_reply_to_id = Utils.optStringX( src, "in_reply_to_id" ); // null
			status.in_reply_to_account_id = Utils.optStringX( src, "in_reply_to_account_id" ); // null
			status.reblog = TootStatus.parse( log, account, src.optJSONObject( "reblog" ) );
			status.content = Utils.optStringX( src, "content" );
			status.created_at = Utils.optStringX( src, "created_at" ); // "2017-04-16T09:37:14.000Z"
			status.reblogs_count = src.optLong( "reblogs_count" );
			status.favourites_count = src.optLong( "favourites_count" );
			status.reblogged = src.optBoolean( "reblogged" );
			status.favourited = src.optBoolean( "favourited" );
			status.sensitive = src.optBoolean( "sensitive" ); // false
			status.spoiler_text = Utils.optStringX( src, "spoiler_text" ); // "",null, or CW text
			status.visibility = Utils.optStringX( src, "visibility" );
			status.media_attachments = TootAttachment.parseList( log, src.optJSONArray( "media_attachments" ) );
			status.mentions = TootMention.parseList( log, src.optJSONArray( "mentions" ) );
			status.tags = TootTag.parseList( log, src.optJSONArray( "tags" ) );
			status.application = TootApplication.parse( log, src.optJSONObject( "application" ) ); // null
			
			status.time_created_at = parseTime( log, status.created_at );
			status.decoded_content = HTMLDecoder.decodeHTML( account, status.content ,true,status.media_attachments );
			// status.decoded_tags = HTMLDecoder.decodeTags( account,status.tags );
			status.decoded_mentions = HTMLDecoder.decodeMentions( account, status.mentions );
			
			if( ! TextUtils.isEmpty( status.spoiler_text ) ){
				status.decoded_spoiler_text = HTMLDecoder.decodeHTML( account, status.spoiler_text ,true,status.media_attachments);
			}
			return status;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "TootStatus.parse failed." );
			return null;
		}
	}
	
	@NonNull
	public static List parseList( LogCategory log, LinkClickContext account, JSONArray array ){
		List result = new List();
		if( array != null ){
			int array_size = array.length();
			result.ensureCapacity( array_size );
			for( int i = 0 ; i < array_size ; ++ i ){
				JSONObject src = array.optJSONObject( i );
				if( src == null ) continue;
				TootStatus item = parse( log, account, src );
				if( item != null ) result.add( item );
			}
		}
		return result;
	}
	
	private static final Pattern reTime = Pattern.compile( "\\A(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)" );
	
	private static final TimeZone tz_utc = TimeZone.getTimeZone( "UTC" );
	
	static long parseTime( LogCategory log, String strTime ){
		if( ! TextUtils.isEmpty( strTime ) ){
			try{
				Matcher m = reTime.matcher( strTime );
				if( ! m.find() ){
					log.d( "!!invalid time format: %s", strTime );
				}else{
					GregorianCalendar g = new GregorianCalendar( tz_utc );
					g.set(
						Utils.parse_int( m.group( 1 ), 1 ),
						Utils.parse_int( m.group( 2 ), 1 ) - 1,
						Utils.parse_int( m.group( 3 ), 1 ),
						Utils.parse_int( m.group( 4 ), 0 ),
						Utils.parse_int( m.group( 5 ), 0 ),
						Utils.parse_int( m.group( 6 ), 0 )
					);
					g.set( Calendar.MILLISECOND, Utils.parse_int( m.group( 7 ), 0 ) );
					return g.getTimeInMillis();
				}
			}catch( Throwable ex ){// ParseException,  ArrayIndexOutOfBoundsException
				ex.printStackTrace();
				log.e( ex, "TootStatus.parseTime failed. src=%s", strTime );
			}
		}
		return 0L;
	}
	
	private static final SimpleDateFormat date_format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.getDefault() );
	
	public static String formatTime( long t ){
		date_format.setTimeZone( TimeZone.getDefault() );
		return date_format.format( new Date( t ) );
	}
	
	// 公開範囲を比較する
	// 公開範囲が広い => 大きい
	// aの方が小さい（狭い)ならマイナス
	// aの方が大きい（狭い)ならプラス
	// IndexOutOfBoundsException 公開範囲が想定外
	public static int compareVisibility( String a, String b ){
		int ia = compareVisibility_tmp( a );
		int ib = compareVisibility_tmp( b );
		if( ia < ib ) return - 1;
		if( ia > ib ) return 1;
		return 0;
	}
	
	private static int compareVisibility_tmp( String a ){
		if( TootStatus.VISIBILITY_DIRECT.equals( a ) ) return 0;
		if( TootStatus.VISIBILITY_PRIVATE.equals( a ) ) return 1;
		if( TootStatus.VISIBILITY_UNLISTED.equals( a ) ) return 2;
		if( TootStatus.VISIBILITY_PUBLIC.equals( a ) ) return 3;
		throw new IndexOutOfBoundsException( "visibility not in range" );
	}
	
//	public void updateNickname( SavedAccount access_info ){
//		decoded_content = HTMLDecoder.decodeHTML( access_info, content );
//		decoded_mentions = HTMLDecoder.decodeMentions( access_info, mentions );
//
//		if( ! TextUtils.isEmpty( spoiler_text ) ){
//			decoded_spoiler_text = HTMLDecoder.decodeHTML( access_info, spoiler_text );
//		}
//	}
	
	public boolean checkMuted( @NonNull HashSet< String > muted_app, @NonNull WordTrieTree muted_word ){
		
		// app mute
		if( application != null ){
			String name = application.name;
			if( name != null ){
				if( muted_app.contains( name ) ){
					return true;
				}
			}
		}
		
		// word mute
		if( decoded_content != null && muted_word.containsWord( decoded_content.toString() ) ){
			return true;
		}
		
		if( decoded_spoiler_text != null && muted_word.containsWord( decoded_spoiler_text.toString() ) ){
			return true;
		}
		
		// reblog
		return reblog != null && reblog.checkMuted( muted_app, muted_word );
		
	}
	
}
