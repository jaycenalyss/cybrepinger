package com.jaycen.cybrepinger;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import com.jaycen.cybrepinger.util.LogCategory;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
	static final LogCategory log = new LogCategory("MyFirebaseMessagingService");
	
	@Override public void onMessageReceived( RemoteMessage remoteMessage ){
		super.onMessageReceived( remoteMessage );
		
		String tag = null;
		Map< String, String > data = remoteMessage.getData();
		if( data != null ){
			for( Map.Entry< String, String > entry : data.entrySet() ){
				log.d( "onMessageReceived: %s=%s", entry.getKey(), entry.getValue() );
				
				if( "notification_tag".equals( entry.getKey() ) ){
					tag = entry.getValue();
				}
			}
		}
		AlarmService.onFirebaseMessage( getApplicationContext() ,tag);
	}
}
