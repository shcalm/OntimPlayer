package com.ontim.player;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;

import java.io.File;
import java.io.IOException;
import android.view.WindowManager;

import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
/**
 * This activity plays a video from a specified URI.
 */
public class MovieView extends NoSearchActivity  {
    private static final String TAG = "MovieView";

    private MovieViewControl mControl;
    private boolean mFinishOnCompletion;
    private boolean mResumed = false;  // Whether this activity has been resumed.
    private boolean mFocused = false;  // Whether this window has focus.
    private boolean mControlResumed = false;  // Whether the MovieViewControl is resumed.
    private boolean mIsFromList = false;
    private Handler mHander = new Handler();
	public static final int MENU_ITEM_GOTO = Menu.FIRST + 1;
	public static final int MENU_ITEM_SETTINGS = Menu.FIRST + 2;
	public static final int MENU_ITEM_CAPTURE = Menu.FIRST + 3;
    static {
        System.loadLibrary("capture_jni");
    }

	Runnable capture = new Runnable(){
		public void run() {
			// TODO Auto-generated method stub
			captureJPEG();
		}
		
	};
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		//String scheme = mUri.getScheme();
		//if ("http".equalsIgnoreCase(scheme)
		//		|| "rtsp".equalsIgnoreCase(scheme)) {
		//	menu.add(0,MENU_ITEM_GOTO,0,R.string.streaminggoto);
		//	menu.add(0, MENU_ITEM_SETTINGS, 1, R.string.streamingsettings);
			menu.add(0, MENU_ITEM_CAPTURE, 2, "capture");
		//}

		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
	
		case MENU_ITEM_CAPTURE:
			
			if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
						File dstFile = new File("/mnt/sdcard/playercapture/tmp");
					if(!dstFile.exists()){
							if(dstFile.getParentFile() != null && !dstFile.getParentFile().mkdir()){
								Log.i(TAG, "In mkdir fail");
								try {
									dstFile.createNewFile();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								return true;	
								}
					}
					Log.d("movieview","captureJPEG");
					mHander.postDelayed(capture, 1000);

					
			}
			
			else{
		
				Toast.makeText(getBaseContext(),"no sdcard", Toast.LENGTH_LONG).show();
		
			}
//			Display display = getWindowManager().getDefaultDisplay();
//	        int screenHeight = display.getHeight();
//	        int screenWidth = display.getWidth();
//			//RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(480, 320);
//			//findViewById(R.id.surface_view).setLayoutParams(lp);
//			LayoutParams lp = findViewById(R.id.surface_view).getLayoutParams();
//	    	lp.height = screenHeight;
//			lp.width = screenWidth;
//			findViewById(R.id.surface_view).setLayoutParams(lp);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	private void changeVideoSize(){
		
	}
	public  native void  captureJPEG();
	public boolean getIsFromList(){
		return mIsFromList;
	}
    public void onCreate(Bundle icicle) {
    	Log.d("##########","onCreate");
        super.onCreate(icicle);
        setContentView(R.layout.playerview);
        View rootView = findViewById(R.id.root);
        Intent intent = getIntent();
        mIsFromList = intent.getBooleanExtra("isFromList",false);
        mControl = new MovieViewControl(rootView, this, intent.getData()) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finish();
                }
            }
        };
        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        mFinishOnCompletion = intent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
    }

    @Override
    public void onPause() {
    	Log.d("###########","onPause");
        super.onPause();
        mResumed = false;
        if (mControlResumed) {
        	Log.d("###########","onPause2");
            mControl.onPause();
            mControlResumed = false;
        }
    }

    @Override
    public void onResume() {
    	Log.d("###########","onResume" + mFocused + " " + mControlResumed +" ");
        super.onResume();
        mResumed = true;
        if (mFocused && mResumed && !mControlResumed) {
        	Log.d("###########","onResume2" + mFocused + " " + mControlResumed +" ");
            mControl.onResume();
            mControlResumed = true;
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	Log.d("##########","onWindowFocusChanged" + " = " + hasFocus );
        mFocused = hasFocus;
        if (mFocused && mResumed && !mControlResumed) {
         	Log.d("##########","onWindowFocusChanged2" + " = " + hasFocus );
            mControl.onResume();
            mControlResumed = true;
        }
    }
}
