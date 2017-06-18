package com.jaycen.cybrepinger;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.jaycen.cybrepinger.api.entity.TootAccount;
import com.jaycen.cybrepinger.api.entity.TootStatus;
import com.jaycen.cybrepinger.table.SavedAccount;
import com.jaycen.cybrepinger.table.UserRelation;
import com.jaycen.cybrepinger.util.Emojione;
import com.jaycen.cybrepinger.util.Utils;
import com.jaycen.cybrepinger.view.MyLinkMovementMethod;
import com.jaycen.cybrepinger.view.MyNetworkImageView;

class HeaderViewHolder implements View.OnClickListener, View.OnLongClickListener {
	final Column column;
	private final ActMain activity;
	private final SavedAccount access_info;
	
	final View viewRoot;
	private final MyNetworkImageView ivBackground;
	private final TextView tvCreated;
	private final MyNetworkImageView ivAvatar;
	private final TextView tvDisplayName;
	private final TextView tvAcct;
	private final Button btnFollowing;
	private final Button btnFollowers;
	private final Button btnStatusCount;
	private final TextView tvNote;
	private final ImageButton btnFollow;
	private final ImageView ivFollowedBy;
	private final View llProfile;
	
	
	private TootAccount who;
	
	HeaderViewHolder( ActMain arg_activity,Column column, ListView parent ){
		this.activity = arg_activity;
		this.column = column;
		this.access_info = column.access_info;
		this.viewRoot = activity.getLayoutInflater().inflate( R.layout.lv_list_header, parent, false );
		viewRoot.setTag( this);
		
		if( activity.timeline_font != null ){
			Utils.scanView( viewRoot, new Utils.ScanViewCallback() {
				@Override public void onScanView( View v ){
					try{
						if( v instanceof Button ){
							// ボタンは太字なので触らない
						}else if( v instanceof TextView ){
							( (TextView) v ).setTypeface( activity.timeline_font );
						}
					}catch(Throwable ex){
						ex.printStackTrace();
					}
				}
			} );
		}
		
		
		ivBackground = (MyNetworkImageView) viewRoot.findViewById( R.id.ivBackground );
		llProfile = viewRoot.findViewById( R.id.llProfile );
		tvCreated = (TextView) viewRoot.findViewById( R.id.tvCreated );
		ivAvatar = (MyNetworkImageView) viewRoot.findViewById( R.id.ivAvatar );
		tvDisplayName = (TextView) viewRoot.findViewById( R.id.tvDisplayName );
		tvAcct = (TextView) viewRoot.findViewById( R.id.tvAcct );
		btnFollowing = (Button) viewRoot.findViewById( R.id.btnFollowing );
		btnFollowers = (Button) viewRoot.findViewById( R.id.btnFollowers );
		btnStatusCount = (Button) viewRoot.findViewById( R.id.btnStatusCount );
		tvNote = (TextView) viewRoot.findViewById( R.id.tvNote );
		View btnMore = viewRoot.findViewById( R.id.btnMore );
		btnFollow = (ImageButton) viewRoot.findViewById( R.id.btnFollow );
		ivFollowedBy = (ImageView) viewRoot.findViewById( R.id.ivFollowedBy );
		
		
		ivBackground.setOnClickListener( this );
		btnFollowing.setOnClickListener( this );
		btnFollowers.setOnClickListener( this );
		btnStatusCount.setOnClickListener( this );
		btnMore.setOnClickListener( this );
		btnFollow.setOnClickListener( this );
		
		btnFollow.setOnLongClickListener( this );
		
		tvNote.setMovementMethod( MyLinkMovementMethod.getInstance() );
	}
	
	void showColor(){
		int c = column.column_bg_color;
		if( c == 0 ){
			c = Styler.getAttributeColor( activity, R.attr. colorProfileBackgroundMask);
		}else{
			c = 0xc0000000 | (0x00ffffff & c);
		}
		llProfile.setBackgroundColor( c );
	}
	
	void bind( TootAccount who ){
		this.who = who;

		showColor();
		
		if( who == null ){
			tvCreated.setText( "" );
			ivBackground.setImageDrawable( null );
			ivAvatar.setImageDrawable( null );
			tvDisplayName.setText( "" );
			tvAcct.setText( "@" );
			tvNote.setText( "" );
			btnStatusCount.setText( activity.getString( R.string.statuses ) + "\n" + "?" );
			btnFollowing.setText( activity.getString( R.string.following ) + "\n" + "?" );
			btnFollowers.setText( activity.getString( R.string.followers ) + "\n" + "?" );
			
			btnFollow.setImageDrawable( null );
		}else{
			tvCreated.setText( TootStatus.formatTime( who.time_created_at ) );
			ivBackground.setImageUrl( access_info.supplyBaseUrl( who.header_static ) );
			ivAvatar.setCornerRadius( activity.pref,16f );
			ivAvatar.setImageUrl( access_info.supplyBaseUrl( who.avatar_static ) );
			tvDisplayName.setText( who.display_name );
			
			String s = "@" + access_info.getFullAcct( who );
			if( who.locked ){
				s += " " + Emojione.map_name2unicode.get( "lock" );
			}
			tvAcct.setText( Emojione.decodeEmoji( s ) );
			
			tvNote.setText( who.note );
			btnStatusCount.setText( activity.getString( R.string.statuses ) + "\n" + who.statuses_count );
			btnFollowing.setText( activity.getString( R.string.following ) + "\n" + who.following_count );
			btnFollowers.setText( activity.getString( R.string.followers ) + "\n" + who.followers_count );
			
			UserRelation relation = UserRelation.load( access_info.db_id, who.id );
			Styler.setFollowIcon( activity, btnFollow, ivFollowedBy, relation );
		}
	}
	
	@Override
	public void onClick( View v ){
		
		switch( v.getId() ){
		
		case R.id.ivBackground:
			if( who != null ){
				// 強制的にブラウザで開く
				activity.openChromeTab( activity.nextPosition( column ), access_info, who.url, true );
			}
			break;
		
		case R.id.btnFollowing:
			column.profile_tab = Column.TAB_FOLLOWING;
			activity.app_state.saveColumnList();
			column.startLoading();
			break;
		
		case R.id.btnFollowers:
			column.profile_tab = Column.TAB_FOLLOWERS;
			activity.app_state.saveColumnList();
			column.startLoading();
			break;
		
		case R.id.btnStatusCount:
			column.profile_tab = Column.TAB_STATUS;
			activity.app_state.saveColumnList();
			column.startLoading();
			break;
		
		case R.id.btnMore:
			if( who != null ){
				new DlgContextMenu( activity, column, who, null ).show();
			}
			break;
		
		case R.id.btnFollow:
			if( who != null ){
				new DlgContextMenu( activity, column, who, null ).show();
			}
			break;
		}
	}
	
	@Override public boolean onLongClick( View v ){
		switch( v.getId() ){
		case R.id.btnFollow:
			activity.openFollowFromAnotherAccount( access_info,who );
			return true;
		}
		
		return false;
	}
	
}