package de.winterrettich.ninaradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.model.Station;
import de.winterrettich.ninaradio.service.BroadcastToEventAdapter;
import de.winterrettich.ninaradio.ui.MainActivity;

/**
 * Manages a single {@link Notification}
 */
public class RadioNotificationManager {
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_NOTIFICATION_DISMISS = BuildConfig.APPLICATION_ID + "ACTION_NOTIFICATION_DISMISS";
    public static final String ACTION_NOTIFICATION_PLAY = BuildConfig.APPLICATION_ID + "ACTION_NOTIFICATION_PLAY";
    public static final String ACTION_NOTIFICATION_PAUSE = BuildConfig.APPLICATION_ID + "ACTION_NOTIFICATION_PAUSE";

    private NotificationManager mNotificationManager;
    private PendingIntent mMainIntent;
    private PendingIntent mPlayIntent;
    private PendingIntent mPauseIntent;
    private PendingIntent mDismissIntent;

    private Context mContext;
    private MediaSessionCompat mMediaSession;
    private Station mStation = new Station("", "");
    private PlaybackEvent.Type mPlaybackState = PlaybackEvent.Type.PLAY;


    public RadioNotificationManager(Context context, MediaSessionCompat mediaSession) {
        mContext = context;
        mMediaSession = mediaSession;

        // init Intents
        mPlayIntent = createBroadcastIntent(ACTION_NOTIFICATION_PLAY);
        mPauseIntent = createBroadcastIntent(ACTION_NOTIFICATION_PAUSE);
        mDismissIntent = createBroadcastIntent(ACTION_NOTIFICATION_DISMISS);
        mMainIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        showNotification();
    }

    private PendingIntent createBroadcastIntent(String action) {
        Intent intent =  new Intent(mContext, BroadcastToEventAdapter.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(mContext,
                NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void showNotification() {
        // TODO Resources etc.
        Notification.Builder builder = new Notification.Builder(mContext)
                //.setCategory(Notification.CATEGORY_TRANSPORT);
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(mStation.name)
                .setContentText(mStation.url)
                .setContentIntent(mMainIntent)
                .setDeleteIntent(mDismissIntent);

        if(mPlaybackState == PlaybackEvent.Type.PLAY) {
            builder.addAction(R.drawable.ic_pause_black_36dp, "Pause", mPauseIntent);
        } else {
            builder.addAction(R.drawable.ic_play_arrow_black_36dp, "Play", mPlayIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Notification.MediaStyle style = new Notification.MediaStyle();
            MediaSession.Token token = (MediaSession.Token) mMediaSession.getSessionToken().getToken();
            style.setMediaSession(token);
            builder.setStyle(style);
        }

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void hideNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    public void setStation(Station station) {
        mStation = station;
        showNotification();
    }

    public void setPlaybackState(PlaybackEvent.Type playbackState) {
        mPlaybackState = playbackState;
        showNotification();
    }

}