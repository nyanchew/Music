/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nyanchew.android.music;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;
import android.graphics.Bitmap;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class MediaAppWidgetProvider2 extends AppWidgetProvider {
    static final String TAG = "MusicAppWidgetProvider2";
    
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate2";
    
    static final ComponentName THIS_APPWIDGET =
        new ComponentName("nyanchew.android.music",
                "nyanchew.android.music.MediaAppWidgetProvider2");
    
    private static MediaAppWidgetProvider2 sInstance;
    
    static synchronized MediaAppWidgetProvider2 getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider2();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(NycwMediaPlaybackService.SERVICECMD);
        updateIntent.putExtra(NycwMediaPlaybackService.CMDNAME,
                MediaAppWidgetProvider2.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }
    
    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.album_appwidget4x2);
        
        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist, res.getText(R.string.emptyplaylist));
	views.setImageViewResource(R.id.albumart, R.drawable.albumart_mp_unknown);

        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(THIS_APPWIDGET, views);
        }
    }
    
    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link MediaPlaybackService}
     */
    void notifyChange(NycwMediaPlaybackService service, String what) {
        if (hasInstances(service)) {
            if (NycwMediaPlaybackService.PLAYBACK_COMPLETE.equals(what) ||
                    NycwMediaPlaybackService.META_CHANGED.equals(what) ||
                    NycwMediaPlaybackService.PLAYSTATE_CHANGED.equals(what)) {
                performUpdate(service, null);
                
            } else if (NycwMediaPlaybackService.PROGRESSBAR_CHANGED.equals(what)) {
            	progRunner(service, null);
                    
            }
        }
    }
    
    /**
     * Update all active widget instances by pushing changes to progress bar only 
     */    
    void progRunner(NycwMediaPlaybackService service, int[] appWidgetIds) {

        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget4x2);
	long pos = service.position();
	long dur = service.duration();

            views.setProgressBar(R.id.progress, 1000, (int) (1000 * pos / dur), false);
            
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(NycwMediaPlaybackService service, int[] appWidgetIds) {
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget4x2);
        
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
	CharSequence albumName = service.getAlbumName();
	long albumId = service.getAlbumId();
	long songId = service.getAudioId();
	long pos = service.position();
	long dur = service.duration();
	Bitmap bm = MusicUtils.getArtwork(service, songId, albumId);
        CharSequence errorState = null;
        
        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
                status.equals(Environment.MEDIA_UNMOUNTED)) {
            errorState = res.getText(R.string.sdcard_busy_title);
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            errorState = res.getText(R.string.sdcard_missing_title);
        } else if (titleName == null) {
            errorState = res.getText(R.string.emptyplaylist);
        }
        
        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.title, View.GONE);
            views.setViewVisibility(R.id.albumname, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
	    views.setImageViewResource(R.id.albumart, R.drawable.albumart_mp_unknown);
            
        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.title, View.VISIBLE);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.albumname, albumName);
            views.setTextViewText(R.id.artist, artistName);
            views.setProgressBar(R.id.progress, 1000, (int) (1000 * pos / dur), false);
	    if (bm == null) {
		views.setImageViewResource(R.id.albumart, R.drawable.albumart_mp_unknown);
	} else {
		views.setImageViewBitmap(R.id.albumart, bm);
          	}
	}
        
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.widget_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.widget_play);
        }

        // Link actions buttons to intents
        linkButtons(service, views, playing);
        
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivityStarter},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        
        final ComponentName serviceName = new ComponentName(context, NycwMediaPlaybackService.class);
        
        if (playerActive) {
            intent = new Intent(context, MediaPlaybackActivity.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        } else {
            intent = new Intent(context, MusicBrowserActivity.class);
            pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.album_appwidget4x2, pendingIntent);
        }
        
        intent = new Intent(NycwMediaPlaybackService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
        
        intent = new Intent(NycwMediaPlaybackService.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);

        intent = new Intent(NycwMediaPlaybackService.PREVIOUS_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);
    }
}
