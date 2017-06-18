package com.jaycen.cybrepinger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.jaycen.cybrepinger.api.TootApiClient;
import com.jaycen.cybrepinger.api.TootApiResult;
import com.jaycen.cybrepinger.api.entity.TootNotification;
import com.jaycen.cybrepinger.api.entity.TootStatus;
import com.jaycen.cybrepinger.table.SavedAccount;
import com.jaycen.cybrepinger.util.LogCategory;
import com.jaycen.cybrepinger.util.Utils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

class StreamReader {
	static final LogCategory log = new LogCategory( "StreamReader" );
	
	static final String EP_USER = "/api/v1/streaming/?stream=user";
	static final String EP_PUBLIC = "/api/v1/streaming/?stream=public";
	static final String EP_PUBLIC_LOCAL = "/api/v1/streaming/?stream=public:local";
	static final String EP_HASHTAG = "/api/v1/streaming/?stream=hashtag"; // + &tag=hashtag (先頭の＃を含まない)
	
	interface Callback {
		void onStreamingMessage( String event_type, Object o );
	}
	
	private class Reader extends WebSocketListener {
		final SavedAccount access_info;
		final String end_point;
		final LinkedList< Callback > callback_list = new LinkedList<>();
		
		Reader( SavedAccount access_info, String end_point ){
			this.access_info = access_info;
			this.end_point = end_point;
		}
		
		synchronized void addCallback( @NonNull Callback stream_callback ){
			for( Callback c : callback_list ){
				if( c == stream_callback ) return;
			}
			callback_list.add( stream_callback );
		}
		
		synchronized void removeCallback( Callback stream_callback ){
			Iterator< Callback > it = callback_list.iterator();
			while( it.hasNext() ){
				Callback c = it.next();
				if( c == stream_callback ) it.remove();
			}
		}
		
		final AtomicBoolean bDisposed = new AtomicBoolean();
		final AtomicBoolean bListening = new AtomicBoolean();
		final AtomicReference< WebSocket > socket = new AtomicReference<>( null );
		
		void dispose(){
			bDisposed.set( true );
			WebSocket ws = socket.get();
			if( ws != null ){
				ws.cancel();
			}
		}
		
		/**
		 * Invoked when a web socket has been accepted by the remote peer and may begin transmitting
		 * messages.
		 */
		@Override
		public void onOpen( WebSocket webSocket, Response response ){
			log.d( "WebSocket onOpen. url=%s .", webSocket.request().url() );
		}
		
		/**
		 * Invoked when a text (type {@code 0x1}) message has been received.
		 */
		@Override
		public void onMessage( WebSocket webSocket, String text ){
			// log.d( "WebSocket onMessage. url=%s, message=%s", webSocket.request().url(), text );
			try{
				final JSONObject obj = new JSONObject( text );
				final String event = obj.optString( "event" );
				final Object payload = parsePayload( event, obj );
				if( payload != null ){
					Utils.runOnMainThread( new Runnable() {
						@Override public void run(){
							if( bDisposed.get() ) return;
							synchronized( this ){
								for( Callback callback : callback_list ){
									try{
										callback.onStreamingMessage( event, payload );
									}catch( Throwable ex ){
										ex.printStackTrace();
									}
								}
							}
						}
					} );
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
			}
		}
		
		private Object parsePayload( String event, JSONObject obj ){
			try{
				if( "update".equals( event ) ){
					return TootStatus.parse( log, access_info, new JSONObject( obj.optString( "payload" ) ) );
				}else if( "notification".equals( event ) ){
					return TootNotification.parse( log, access_info, new JSONObject( obj.optString( "payload" ) ) );
				}else if( "delete".equals( event ) ){
					return obj.optLong( "payload", - 1L );
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Invoked when the peer has indicated that no more incoming messages will be transmitted.
		 */
		@Override
		public void onClosing( WebSocket webSocket, int code, String reason ){
			log.d( "WebSocket onClosing. code=%s,reason=%s,url=%s .", code, reason, webSocket.request().url() );
			webSocket.cancel();
			bListening.set( false );
			handler.removeCallbacks( proc_reconnect );
			handler.postDelayed( proc_reconnect, 10000L );
		}
		
		/**
		 * Invoked when both peers have indicated that no more messages will be transmitted and the
		 * connection has been successfully released. No further calls to this listener will be made.
		 */
		@Override
		public void onClosed( WebSocket webSocket, int code, String reason ){
			log.d( "WebSocket onClosed.  code=%s,reason=%s,url=%s .", code, reason, webSocket.request().url() );
			bListening.set( false );
			handler.removeCallbacks( proc_reconnect );
			handler.postDelayed( proc_reconnect, 10000L );
		}
		
		/**
		 * Invoked when a web socket has been closed due to an error reading from or writing to the
		 * network. Both outgoing and incoming messages may have been lost. No further calls to this
		 * listener will be made.
		 */
		@Override
		public void onFailure( WebSocket webSocket, Throwable ex, Response response ){
			log.e( ex, "WebSocket onFailure. url=%s .", webSocket.request().url() );
			bListening.set( false );
			handler.removeCallbacks( proc_reconnect );
			handler.postDelayed( proc_reconnect, 10000L );
		}
		
		final Runnable proc_reconnect = new Runnable() {
			@Override public void run(){
				if( bDisposed.get() ) return;
				startRead();
			}
		};
		
		void startRead(){
			if( bDisposed.get() ){
				log.d( "startRead: this reader is disposed." );
				return;
			}else if( bListening.get() ){
				log.d( "startRead: this reader is already listening." );
				return;
			}
			
			bListening.set( true );
			new AsyncTask< Void, Void, TootApiResult >() {
				@Override protected TootApiResult doInBackground( Void... params ){
					TootApiClient client = new TootApiClient( context, new TootApiClient.Callback() {
						@Override public boolean isApiCancelled(){
							return isCancelled();
						}
						
						@Override public void publishApiProgress( String s ){
						}
					} );
					
					client.setAccount( access_info );
					
					TootApiResult result = client.webSocket( end_point, new Request.Builder(), Reader.this );
					if( result == null ){
						log.d( "startRead: cancelled." );
						bListening.set( false );
					}else{
						socket.set( result.socket );
					}
					return result;
				}
			}.executeOnExecutor( App1.task_executor );
		}
		
	}
	
	private final LinkedList< Reader > reader_list = new LinkedList<>();
	
	final Context context;
	final SharedPreferences pref;
	private final Handler handler;
	
	StreamReader( Context context, SharedPreferences pref ){
		this.context = context;
		this.pref = pref;
		this.handler = new Handler();
	}
	
	private Reader prepareReader( @NonNull SavedAccount access_info, @NonNull String end_point ){
		synchronized( reader_list ){
			for( Reader reader : reader_list ){
				if( reader.access_info.db_id == access_info.db_id
					&& reader.end_point.equals( end_point )
					){
					return reader;
				}
			}
			Reader reader = new Reader( access_info, end_point );
			reader_list.add( reader );
			return reader;
		}
	}
	
	// onPauseのタイミングで全てのStreaming接続を破棄する
	void onPause(){
		synchronized( reader_list ){
			for( Reader reader : reader_list ){
				reader.dispose();
			}
			reader_list.clear();
		}
	}
	
	// カラム破棄やリロードのタイミングで呼ばれる
	void unregister( SavedAccount access_info, String end_point, Callback stream_callback ){
		synchronized( reader_list ){
			Iterator< Reader > it = reader_list.iterator();
			while( it.hasNext() ){
				Reader reader = it.next();
				if( reader.access_info.db_id == access_info.db_id
					&& reader.end_point.equals( end_point )
					){
					log.d("unregister: removeCallback %s",end_point);
					reader.removeCallback( stream_callback );
					if( reader.callback_list.isEmpty() ){
						log.d("unregister: dispose %s",end_point);
						reader.dispose();
						it.remove();
					}
				}
			}
		}
	}
	
	// onResume や ロード完了ののタイミングで登録される
	void register( @NonNull SavedAccount access_info, @NonNull String end_point, @NonNull Callback stream_callback ){
		
		final Reader reader = prepareReader( access_info, end_point );
		reader.addCallback( stream_callback );
		
		if( ! reader.bListening.get() ){
			reader.startRead();
		}
	}
	
}
