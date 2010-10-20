/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabWidget;
import android.widget.TextView;

public class VideoBrowserActivity extends ListActivity implements MusicUtils.Defs
{
    public VideoBrowserActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        init();
    }

    public void init() {

        // Set the layout for this activity.  You can find it
        // in assets/res/any/layout/media_picker_activity.xml
        setContentView(R.layout.media_picker_activity);
        
        TabWidget buttonbar = (TabWidget) findViewById(R.id.buttonbar);
        buttonbar.setVisibility(View.GONE);

        MakeCursor();

        if (mCursor == null) {
            MusicUtils.displayDatabaseError(this);
            return;
        }

        if (mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        } else {
            setTitle(R.string.no_videos_title);
        }

        // Map Cursor columns to views defined in media_list_item.xml
        //SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        VideoListAdapter adapter = new VideoListAdapter(
                this,
                //android.R.layout.simple_list_item_1,
                R.layout.track_list_item,
                mCursor,
                //new String[] { MediaStore.Video.Media.TITLE},
                //new int[] { android.R.id.text1 });
                new String[] {},
                new int[] {});

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        mCursor.moveToPosition(position);
        String type = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        
        startActivity(intent);
    }

    private void MakeCursor() {
        String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST
        };
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            mSortOrder = MediaStore.Video.Media.TITLE + " COLLATE UNICODE";
            mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
            mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                cols, mWhereClause , null, mSortOrder);
        }
    }
    
    static class VideoListAdapter extends SimpleCursorAdapter {
        
        private final Bitmap mDefaultVideoIcon;
        private int mVideoIdx;
        private int mArtistIdx;
        //private final StringBuilder mStringBuilder = new StringBuilder();
        //private final String mUnknownAlbum;
        //private final String mUnknownArtist;
        //private final String mAlbumSongSeparator;
        //private final Object[] mFormatArgs = new Object[1];
        
        static class ViewHolder {
            TextView line1;
            TextView line2;
            //ImageView play_indicator;
            ImageView icon;
        }

        VideoListAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);

            Resources r = context.getResources();
            mDefaultVideoIcon = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown_list);
            getColumnIndices(cursor);
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mVideoIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                mArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST);
            }
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
           View v = super.newView(context, cursor, parent);
           ViewHolder vh = new ViewHolder();
           vh.line1 = (TextView) v.findViewById(R.id.line1);
           vh.line2 = (TextView) v.findViewById(R.id.line2);
           vh.icon = (ImageView) v.findViewById(R.id.icon);
           vh.icon.setImageBitmap(mDefaultVideoIcon);
           vh.icon.setPadding(1, 1, 1, 1);
           v.setTag(vh);
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mVideoIdx);
            vh.line1.setText(name);
            
            name = cursor.getString(mArtistIdx);
            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
            long aid = cursor.getLong(0);
            ContentResolver cr = context.getContentResolver();
            Bitmap b = MediaStore.Video.Thumbnails.getThumbnail(cr, aid, MediaStore.Video.Thumbnails.MICRO_KIND, null);
            iv.setImageBitmap(b);
        }
        
        
        public int getSectionForPosition(int position) {
            return 0;
        }
    }


    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
        super.onDestroy();
    }

    private Cursor mCursor;
    private String mWhereClause;
    private String mSortOrder;
}

