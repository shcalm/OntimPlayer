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

package com.ontim.player;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class MovieViewControl implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    @SuppressWarnings("unused")
    private static final String TAG = "MovieViewControl";

    private static final int ONE_MINUTE = 60 * 1000;
    private static final int TWO_MINUTES = 2 * ONE_MINUTE;
    private static final int FIVE_MINUTES = 5 * ONE_MINUTE;

    // Copied from MediaPlaybackService in the Music Player app. Should be
    // public, but isn't.
    private static final String SERVICECMD =
            "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";

    private final VideoView mVideoView;
    private final View mProgressView;
    private final Uri mUri;
    private final ContentResolver mContentResolver;

    
    // State maintained for proper onPause/OnResume behaviour.
    private int mPositionWhenPaused = -1;
    private boolean mWasPlayingWhenPaused = false;
    private MediaController mMediaController;


  //  private SubTitle mSubTitle;
    Handler mHandler = new Handler();
    private TextView mSubtitleView;
    private Context mContext;
    ArrayList<subtitle> mSubTitle  =  new ArrayList<subtitle>();
    private boolean mIsLoadSub = false;
    private boolean mIsHaveSub = false;
    Runnable mPlayingChecker = new Runnable() {
        public void run() {
            if (mVideoView.isPlaying()) {
                mProgressView.setVisibility(View.GONE);
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };
    
    
	private static final int SUBTITLE_UPDATE = 2000;
	Handler mInfoHandler = new Handler();
	private BufferedReader mIn2 = null;
	private int mSubPos = 0;
	private int mLoadTimes = 1;
	private int mSubTitleReadPos = 0;
	private  int BinarySearch(ArrayList<subtitle> title,int pos){
		
		int low = 0;
		int high = title.size() - 1;
		while(low <= high){
			int mid = (low + high) / 2;
			subtitle midVal = (subtitle)title.get(mid);
			if(midVal.starttime < pos){
				low = mid + 1;
				int mid2 = (low +high)/2;
			}else if(midVal.starttime > pos){
				high = mid -1;
				
			}else{
				
			}
			
		}
		
		return 0;
	}
	Runnable mSubTitleUpdate = new Runnable(){
	
		public void run(){
			
			if(mIsLoadSub == false){
				mIsHaveSub = loadSubTitle();
				mIsLoadSub = true;
			}
			
			if(mIsHaveSub == true){
				if(mVideoView.isPlaying()){
					
					int currentpos = mVideoView.getCurrentPosition();
					Log.d("###########","currentpos "+ currentpos);

					while(currentpos > mSubTitle.get(mSubTitle.size() - 1).getStarttime()){
						ContinueLoadSubTitle();
					}
					
					//for(subtitle title : mSubTitle)
					for(int i = mSubTitleReadPos; i < mSubTitle.size() ; i++ ){
						subtitle title = mSubTitle.get(i);
						Log.d("##########","title.getStarttime() " + title.getStarttime()+ " title.getEndtime() "+ title.getEndtime());
						if(title.getStarttime() <= currentpos && title.getEndtime() >= currentpos){
							mSubtitleView.setText(title.getContent());
							mSubTitleReadPos = i;
							break;

						}
						if(title.getStarttime() > currentpos){
							mSubtitleView.setText("");
							break;
						}
					}

				}
				mInfoHandler.postDelayed(mSubTitleUpdate,SUBTITLE_UPDATE);
		}
		}
	
	
	};
	private void parseTime(String time,subtitle title){
		int index1 = time.lastIndexOf("-->");
		Log.d("moview","parseTime index1 = " + index1);
		if(index1 != -1){
			String formrpart = time.substring(0, index1);
			String lastpart  = time.substring(index1 + 4);
			String[] str = formrpart.split(":");
			if(str.length != 3){
				return ;
			}
			long starttime =0;
			String minsec = str[2].split(",")[1].trim();
			if(minsec.charAt(0) =='0'){
				minsec = minsec.substring(1);
			}
			starttime = Integer.parseInt(str[0])*60*60*1000
			+ Integer.parseInt(str[1])*60*1000
			+ Integer.parseInt(str[2].split(",")[0])*1000
			+ Integer.parseInt(minsec);
			System.out.print(str[2].split(",")[1]);
			title.setStarttime(starttime);
				
			str = lastpart.split(":");
			if(str.length != 3){
				return ;
			}
			
			long endtime =0;
			minsec = str[2].split(",")[1].trim();
			if(minsec.charAt(0) =='0'){
				minsec = minsec.substring(1);
			}
			endtime = Integer.parseInt(str[0])*60*60*1000
			+ Integer.parseInt(str[1])*60*1000
			+ Integer.parseInt(str[2].split(",")[0])*1000
			+ Integer.parseInt(minsec);
			title.setEndtime(endtime);
		//	Log.d("movieview####","parseTime formrpart = " + formrpart + " lastpart " + lastpart + " starttime " + starttime + " endtime " + endtime);

		}

	}
	private String getFileName(){
		String filename= "";
		String[] proj = new String[] {
                MediaStore.Video.Media.DATA         
        };
		Cursor cursor = mContentResolver.query(mUri,proj,null,null,null);
		Log.d("###########","cursor = " + cursor);
		if(cursor != null && cursor.moveToFirst()){
			int index =  cursor.getColumnIndex(MediaStore.Video.Media.DATA);
			filename = cursor.getString(index);
			Log.d("############","filename" + filename);
			
		}
		return filename;
		
		
	}
	private void ContinueLoadSubTitle(){
		String tmp;
		try{
			while((tmp =mIn2.readLine()) != null){
				subtitle title1 = new subtitle();
				int id = Integer.parseInt(tmp);
				title1.setId(id);

				int j = 0;
				StringBuilder content = new StringBuilder("");
				while(true){
					if((tmp =mIn2.readLine()) != null){
						//Log.d("#########","tmp = " + tmp);
						if(j == 0){
							parseTime(tmp,title1);
							j++;
							continue;
						}else{
							int length = tmp.length();
							if(length == 0){
								title1.setContent(new String(content));
								break;
							}else{
								content.append(tmp);
							}


						}

					}else{
						break;
					}
				}

				mSubTitle.add(title1);
				mSubPos ++;
				if(mSubPos >= 50){
					mSubPos = 0;
					break;
				}
			}		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public void setSubPos(int newpos){		
		for(int i = 0; i < mSubTitle.size() ; i++ ){
			subtitle title = mSubTitle.get(i);
			subtitle title2 = mSubTitle.get(i+1);
			if(title.getStarttime() <= newpos && title2.getStarttime() >= newpos){
				mSubTitleReadPos = i;
				break;

			}
		}
		
	}
	private boolean loadSubTitle(){
		//	mSubTitle = new SubTitle();
		String filename= "";
		String[] proj = new String[] {
                MediaStore.Video.Media.DATA         
        };
		Cursor cursor = mContentResolver.query(mUri,proj,null,null,null);
		Log.d("###########","cursor = " + cursor);
		if(cursor != null && cursor.moveToFirst()){
			int index =  cursor.getColumnIndex(MediaStore.Video.Media.DATA);
			String tmpname = cursor.getString(index);
		//	Log.d("############","tmpname" + tmpname);
			filename = tmpname.substring(0,tmpname.lastIndexOf('.')) + ".srt";
		//	Log.d("############","filename" + filename);
			
		}
 		String tmp;
		String decode = "GBK";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"UTF-16"));	
			int flag = in.read();
			byte flag1 = (byte)((flag >> 8) & 0x00FF);
			byte flag2 = (byte)(flag  & 0x00FF);
			flag = in.read();
			byte flag3 = (byte)((flag >> 8) & 0x00FF);
			Log.d("##########","flag1 = " + flag1 + "flag2 =" + flag2 + "flag3 = " + flag3 );
			if((flag1 ==(byte) 0xFF && flag2 == (byte)0xFE) || (flag2 == (byte)0xFF && flag1 == (byte)0xFE)){
				decode ="UTF-16";					
			}else if(flag1 == (byte)0xEF && flag2 ==(byte) 0xBB && flag3 == (byte)0xBF){
				decode = "UTF8";
				in.read();
				
				
				
			}
			mIn2 = new BufferedReader(new InputStreamReader(new FileInputStream(filename),decode));
			if(decode.compareTo("UTF8") == 0){//special edit for utf-8
				tmp = mIn2.readLine();
			//	Log.d("#########","tmp = " + tmp);
				subtitle title1 = new subtitle();
				title1.setId(1);
				
				int j = 0;
				StringBuilder content = new StringBuilder("");
				while(true){
					if((tmp =mIn2.readLine()) != null){
				//		Log.d("#########","tmp = " + tmp);
						if(j == 0){
							parseTime(tmp,title1);
							j++;
							continue;
						}else{
							int length = tmp.length();
							if(length == 0){
								title1.setContent(new String(content));
								break;
							}else{
								content.append(tmp);
							}
							
							
						}
						
					}
				}
				mSubTitle.add(title1);

			}
			while((tmp =mIn2.readLine()) != null){
				subtitle title1 = new subtitle();
				int id = Integer.parseInt(tmp);
				title1.setId(id);
				
				int j = 0;
				StringBuilder content = new StringBuilder("");
				while(true){
					if((tmp =mIn2.readLine()) != null){
						//Log.d("#########","tmp = " + tmp);
						if(j == 0){
							parseTime(tmp,title1);
							j++;
							continue;
						}else{
							int length = tmp.length();
							if(length == 0){
								title1.setContent(new String(content));
								break;
							}else{
								content.append(tmp);
							}
							
							
						}
						
					}else{
						break;
					}
				}
//				
//				for(int j = 0;j<3 ;j++){
//					if((tmp =in2.readLine()) != null){
//						Log.d("#########","tmp = " + tmp);
//						switch(j){
//						case 0://time
//							parseTime(tmp,title1);
//							break;
//						case 1://content
//							title1.setContent(tmp);
//							break;
//						case 2://ignore
//
//							break;			
//						}
//
//					}
//
//				}
				mSubTitle.add(title1);
				mSubPos ++;
				if(mSubPos >= 100){
					mSubPos = 0;
					break;
				}
			}		

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;

		//	String filename = mUri;
		//	mSubTitle.loadSubTitleForVideoFile(filename);

	}
	public MovieViewControl(View rootView, Context context, Uri videoUri) {
		mContentResolver = context.getContentResolver();
		mVideoView = (VideoView) rootView.findViewById(R.id.surface_view);
		mProgressView = rootView.findViewById(R.id.progress_indicator);
		mContext = context;
		mUri = videoUri;

		// For streams that we expect to be slow to start up, show a
		// progress spinner until playback starts.
		String scheme = mUri.getScheme();
		if ("http".equalsIgnoreCase(scheme)
				|| "rtsp".equalsIgnoreCase(scheme)) {
			mHandler.postDelayed(mPlayingChecker, 250);
		} else {
			mProgressView.setVisibility(View.GONE);
		}
		mSubtitleView =(TextView) rootView.findViewById(R.id.subtitle); 
		
		if ("http".equalsIgnoreCase(scheme)
				|| "rtsp".equalsIgnoreCase(scheme)) {
		} else {
			mInfoHandler.postDelayed(mSubTitleUpdate, 5000);	
		}
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setVideoURI(mUri);
		mMediaController = new MediaController(context,getFileName());
		mMediaController.setVideoView(mVideoView);
		mMediaController.setMovieViweController(this);
		if(((MovieView)mContext).getIsFromList()){
			mMediaController.setPrevNextListeners(new android.view.View.OnClickListener(){
				public void onClick(View v) {
					OntimPlayer pInstance = OntimPlayer.getInstance();
					if(pInstance != null){
						Uri uri = pInstance.getNextorPreUri(true);
						if(uri == null) return ;
						Intent i = new Intent(Intent.ACTION_VIEW,uri);
						i.setData(uri);
						i.setClassName(mContext, "com.ontim.player.MovieView");
						i.putExtra("isFromList",true);
						mContext.startActivity(i);
						((MovieView)mContext).finish();
					}

				}

			}, new android.view.View.OnClickListener(){
				public void onClick(View v) {
					OntimPlayer pInstance = OntimPlayer.getInstance();
					if(pInstance != null){
						Uri uri = pInstance.getNextorPreUri(false);
						if(uri == null) return ;
						Intent i = new Intent(Intent.ACTION_VIEW,uri);
						i.setData(uri);
						i.setClassName(mContext, "com.ontim.player.MovieView");
						i.putExtra("isFromList",true);
						mContext.startActivity(i);
						((MovieView)mContext).finish();
					}	
				}
			});
		}
		mVideoView.setMediaController(mMediaController);

		// make the video view handle keys for seeking and pausing
		mVideoView.requestFocus();

		Intent i = new Intent(SERVICECMD);
		i.putExtra(CMDNAME, CMDPAUSE);
		context.sendBroadcast(i);

		/*        final Integer bookmark = getBookmark();
        if (bookmark != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.resume_playing_title);
            builder.setMessage(String.format(
                    context.getString(R.string.resume_playing_message),
                    MenuHelper.formatDuration(context, bookmark)));
            builder.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    onCompletion();
                }});
            builder.setPositiveButton(R.string.resume_playing_resume,
                    new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mVideoView.seekTo(bookmark);
                    mVideoView.start();
                }});
            builder.setNegativeButton(R.string.resume_playing_restart,
                    new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mVideoView.start();
                }});
            builder.show();
        } else {*/
		mVideoView.start();
		//    }
	}

    private static boolean uriSupportsBookmarks(Uri uri) {
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        return ("content".equalsIgnoreCase(scheme)
                && MediaStore.AUTHORITY.equalsIgnoreCase(authority));
    }

    private Integer getBookmark() {
        if (!uriSupportsBookmarks(mUri)) {
            return null;
        }

        String[] projection = new String[] {
                Video.VideoColumns.DURATION,
                Video.VideoColumns.BOOKMARK};

        try {
            Cursor cursor = mContentResolver.query(
                    mUri, projection, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int duration = getCursorInteger(cursor, 0);
                        int bookmark = getCursorInteger(cursor, 1);
                        if ((bookmark < TWO_MINUTES)
                                || (duration < FIVE_MINUTES)
                                || (bookmark > (duration - ONE_MINUTE))) {
                            return null;
                        }
                        return Integer.valueOf(bookmark);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (SQLiteException e) {
            // ignore
        }

        return null;
    }

    private static int getCursorInteger(Cursor cursor, int index) {
        try {
            return cursor.getInt(index);
        } catch (SQLiteException e) {
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    private void setBookmark(int bookmark) {
        if (!uriSupportsBookmarks(mUri)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(Video.VideoColumns.BOOKMARK, Integer.toString(bookmark));
        try {
            mContentResolver.update(mUri, values, null, null);
        } catch (SecurityException ex) {
            // Ignore, can happen if we try to set the bookmark on a read-only
            // resource such as a video attached to GMail.
        } catch (SQLiteException e) {
            // ignore. can happen if the content doesn't support a bookmark
            // column.
        } catch (UnsupportedOperationException e) {
            // ignore. can happen if the external volume is already detached.
        }
    }

    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        setBookmark(mVideoView.getCurrentPosition());

        mPositionWhenPaused = mVideoView.getCurrentPosition();
        mWasPlayingWhenPaused = mVideoView.isPlaying();
        mVideoView.stopPlayback();
    }

    public void onResume() {
    	Log.d("#########","movieviewControl onResume");
        if (mPositionWhenPaused >= 0) {
            mVideoView.setVideoURI(mUri);
            mVideoView.seekTo(mPositionWhenPaused);
            mPositionWhenPaused = -1;
            if (mWasPlayingWhenPaused) {
                mMediaController.show(0);
            }
        }
    }

    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        mHandler.removeCallbacksAndMessages(null);
        mProgressView.setVisibility(View.GONE);
        return false;
    }

    public void onCompletion(MediaPlayer mp) {
        onCompletion();
    }

    public void onCompletion() {
    }
    class subtitle{
		public long getStarttime() {
			return starttime;
		}
		public void setStarttime(long starttime) {
			this.starttime = starttime;
		}
		public long getEndtime() {
			return endtime;
		}
		public void setEndtime(long endtime) {
			this.endtime = endtime;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		private long starttime;
		private long endtime;
		private String content;
		private long id;	
	}
}
