package com.wordsaretoys.freebeard;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.wordsaretoys.freebeard.audio.Audio;
import com.wordsaretoys.freebeard.music.Music;

public class PlayerService extends Service {

	static String TAG = "PlayerService";

	static int NotifyId = 149;

	static String ActionPlay = "Play";
	static String ActionNext = "Next";
	static String ActionPause = "Pause";
	static String ActionClose = "Close";
	static String ActionSofter = "Softer";
	static String ActionLouder = "Louder";
	
	// manager service for notifications
	NotificationManager notifyManager;

	// music generator
	Music music;
	
	// notification UI views
	RemoteViews uiViews;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (intent != null) {

			String action = intent.getAction();
			if (action != null) {
				
				if (action.equals(ActionPlay)) {
					Audio.INSTANCE.play();
				} else if (action.equals(ActionNext)) {
					Audio.INSTANCE.reset();
				} else if (action.equals(ActionPause)) {
					Audio.INSTANCE.pause();
				} else if (action.equals(ActionClose)) {
					stopSelf();
				} else if (action.equals(ActionLouder)) {
					Audio.INSTANCE.louden();
				} else if (action.equals(ActionSofter)) {
					Audio.INSTANCE.soften();
				}
			}
		}

		createNotification(); 
		return START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		music = new Music();
		
		// construct intents
		Intent playIntent = 
				new Intent(ActionPlay, null, this, PlayerService.class);
		Intent nextIntent = 
				new Intent(ActionNext, null, this, PlayerService.class);
		Intent pauseIntent = 
				new Intent(ActionPause, null, this, PlayerService.class);
		Intent loudIntent = 
				new Intent(ActionLouder, null, this, PlayerService.class);
		Intent softIntent = 
				new Intent(ActionSofter, null, this, PlayerService.class);
		Intent closeIntent = 
				new Intent(ActionClose, null, this, PlayerService.class);
		
		// construct ui views
		uiViews = new RemoteViews("com.wordsaretoys.freebeard", R.layout.player);
		uiViews.setOnClickPendingIntent(R.id.play,
				PendingIntent.getService(this, 0, playIntent, 0));
		uiViews.setOnClickPendingIntent(R.id.pause,
				PendingIntent.getService(this, 0, pauseIntent, 0));
		uiViews.setOnClickPendingIntent(R.id.next,
				PendingIntent.getService(this, 0, nextIntent, 0));
		uiViews.setOnClickPendingIntent(R.id.softer,
				PendingIntent.getService(this, 0, softIntent, 0));
		uiViews.setOnClickPendingIntent(R.id.louder,
				PendingIntent.getService(this, 0, loudIntent, 0));
		uiViews.setOnClickPendingIntent(R.id.close,
				PendingIntent.getService(this, 0, closeIntent, 0));		
	}
	
	@Override
	public void onDestroy() {
		notifyManager.cancel(NotifyId);
		Audio.INSTANCE.pause();
		Audio.INSTANCE.flush();
	}
	
	/**
	 * generate the notification tray UI based on player state
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void createNotification() {
		Notification.Builder builder = new Notification.Builder(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			builder.setShowWhen(false);
		}
		
		builder.setSmallIcon(R.drawable.ic_notify);
		builder.setOngoing(true);
		
		builder.setContent(uiViews);

		int pv = Audio.INSTANCE.isPlaying() ? View.VISIBLE : View.GONE;
		int nv = Audio.INSTANCE.isPlaying() ? View.GONE : View.VISIBLE;
		
		uiViews.setViewVisibility(R.id.play, nv);
		uiViews.setViewVisibility(R.id.pause, pv);
		uiViews.setViewVisibility(R.id.next, pv);
		uiViews.setViewVisibility(R.id.louder, pv);
		uiViews.setViewVisibility(R.id.softer, pv);
		uiViews.setViewVisibility(R.id.close, nv);

		startForeground(NotifyId, builder.build());
	}

}
