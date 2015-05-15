package de.winterrettich.ninaradio.event;

import android.util.Log;

import com.squareup.otto.Subscribe;

public class EventLogger {
    private static final String TAG = EventLogger.class.getSimpleName();
    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        Log.i(TAG, PlaybackEvent.class.getSimpleName() + ": " + event.type.name() );
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        Log.i(TAG, SelectStationEvent.class.getSimpleName() + ": " + event.station.name + " -> " + event.station.url );
    }

    @Subscribe
    public void handleHeadphoneDisconnectEvent(HeadphoneDisconnectEvent event) {
        Log.i(TAG, HeadphoneDisconnectEvent.class.getSimpleName());
    }

    @Subscribe
    public void handleDismissNotificationEvent(DismissNotificationEvent event) {
        Log.i(TAG, DismissNotificationEvent.class.getSimpleName());
    }

}