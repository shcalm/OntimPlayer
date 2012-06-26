package com.ontim.player;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;



public class OntimPlayer extends ListActivity {
	
	boolean mIsBitmapLoaded = false;
	private TextView mRightText;
	private int mCurPosition;
	private int mTotalNum;
	public static final int MENU_ITEM_GOTO = Menu.FIRST + 1;
	
	Handler mHandler = new Handler(){
	    public void handleMessage(Message msg){
	    	((FileListViewAdapter) getListAdapter()).notifyDataSetChanged();
	    }
	};
	private EditText mEt;
	private static OntimPlayer mInstance = null;
	public static OntimPlayer getInstance(){
		
		return mInstance;
	}
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
   //     requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    	setContentView(R.layout.main);
   //	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title_1);
    	FileListViewAdapter localFileListViewAdapter = new FileListViewAdapter(this);
    	setListAdapter(localFileListViewAdapter);
    	//     new Thread(new Runnable(){
    	//		public void run() {
    	// TODO Auto-generated method stub
    		loadMediaFile();
    	//		} 	
    	//   });
    //	mRightText = (TextView)findViewById(R.id.right_text);
    //	mTotalNum = localFileListViewAdapter.getCount();
   // 	mRightText.setText(String.valueOf(mTotalNum));
    	new Thread(new Runnable(){
    		public void run() {		
    			loadBitMap();
    		} 	
    	}).start();
        getListView().setEmptyView(findViewById(R.id.empty));
        mInstance = this;

    }
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0,MENU_ITEM_GOTO,0,R.string.gotouri);
		//menu.add(0, MENU_ITEM_SETTINGS, 1, R.string.streamingsettings);


		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_GOTO:
			showDialog(MENU_ITEM_GOTO);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	protected Dialog onCreateDialog(int id){
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.compose_url, null);
		mEt = (EditText)textEntryView.findViewById(R.id.urledit);
		return new AlertDialog.Builder(OntimPlayer.this)
		.setTitle(R.string.urltitle)
		.setView(textEntryView)
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				/* User clicked OK so do some stuff */
				String url = mEt.getText().toString();
				Log.d("songhua","edittext "+ url);
				Intent i = new Intent();
				i.setClass(OntimPlayer.this,MovieView.class);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
		})
		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				/* User clicked cancel so do some stuff */
			}
		})
		.create();


    }
    private void loadBitMap(){
    	List<FileData> items;
    	items = ((FileListViewAdapter) getListAdapter()).getItems();
    	for(FileData fd:items){
    		Bitmap bm = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), 
    				(long)Integer.parseInt(fd.id), MediaStore.Video.Thumbnails.MICRO_KIND, null);
    		if(bm == null){
    			fd.isTumbAvailable = false;
    		}else{
    			Log.d("============","bm height " + bm.getHeight() + " width = " + bm.getWidth());
    			fd.bitmap = bm;
    		}
    	}
    	mIsBitmapLoaded = true;
    	mHandler.sendEmptyMessage(1);
//    	((FileListViewAdapter) getListAdapter()).notifyDataSetChanged();
    	
    }

    private void loadMediaFile(){
    	String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA
                
        };
        ContentResolver resolver = getContentResolver();
        String mSortOrder = MediaStore.Video.Media.TITLE + " COLLATE UNICODE";
        String mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
        Cursor mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            cols, mWhereClause , null, mSortOrder);
        if(mCursor != null && mCursor.moveToFirst()){
        	do{
        		String title = mCursor.getString(mCursor.getColumnIndex(cols[1]));
        		String time = mCursor.getString(mCursor.getColumnIndex(cols[2]));
        		String size = mCursor.getString(mCursor.getColumnIndex(cols[3]));
        		String disname = mCursor.getString(mCursor.getColumnIndex(cols[4]));
        		String data = mCursor.getString(mCursor.getColumnIndex(cols[5]));
        		String id =  mCursor.getString(mCursor.getColumnIndex(cols[0]));
        		
        		Log.d("============","title = " + title + " time = "+ time + " size = "+ size + " disname ="+ disname
        				+ " data= "+ data);
        		((FileListViewAdapter) getListAdapter()).addItem(new FileData(1,title,time,size,id));
        	}while(mCursor.moveToNext());
        	
        }
       if(mCursor != null){mCursor.close();}
    	
    }
    /*
     * isnext true get next uri
     * false getpre uri
     */
    public Uri getNextorPreUri(boolean isnext){
    	Uri uri = null ;
    	Log.d("##########","mCurPosition = " + mCurPosition + "mTotalNum ="+mTotalNum);
    	if(isnext){
    		if(mCurPosition == mTotalNum-1){
    			return null;
    		}else{
    			mCurPosition++; 
    		}
    		
    	}else{
    		if(mCurPosition == 0){
    			return null;
    		}else{
    			mCurPosition--;
    		}    		
    	}
    	Log.d("##########","mCurPosition = " + mCurPosition );
		FileData fd = ((FileListViewAdapter) getListAdapter()).getItem(mCurPosition);
		uri =  ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Integer.parseInt(fd.id));
    	return uri;
    }
    protected void onListItemClick(ListView paramListView, View paramView, int paramInt, long paramLong){
    	mCurPosition = paramInt;
    	FileListViewAdapter adapter = (FileListViewAdapter)paramListView.getAdapter();
    	FileData fd = adapter.getItem(paramInt);
    	Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Integer.parseInt(fd.id));
    	Intent i = new Intent(Intent.ACTION_VIEW,uri);
    	i.setData(uri);
    	i.setClassName(OntimPlayer.this, "com.ontim.player.MovieView");
    	i.putExtra("isFromList",true);
    //	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	//i.setData(uri);
    	//i.setclass
    	startActivity(i);
    }
}




class FileListViewAdapter extends BaseAdapter
{
  private OntimPlayer _activity = null;

  private List<FileData> mItems = null;
  //private static final int HOUR_MILLISEC = 60*60*1000
  public FileListViewAdapter(OntimPlayer paramFileListActivity)
  {
    this._activity = paramFileListActivity;
    ArrayList<FileData> localArrayList = new ArrayList<FileData>();
    this.mItems = localArrayList;
  }

  private void _updateView(View paramView, FileData paramFileData)
  {
	  Log.d("============","_updateView");
	  ImageView lv1 = (ImageView)paramView.findViewById(R.id.image1);
	  if(_activity.mIsBitmapLoaded == true){
		  if(paramFileData.isTumbAvailable == false){
			  lv1.setImageResource(R.drawable.defaulttumb); 
		  }else{
			  lv1.setImageBitmap(paramFileData.bitmap);
		  }
	  }else{
		  lv1.setImageResource(R.drawable.defaulttumb);
	  }
	  // if(paramFileData.isTumbAvailable == false){
	  //		  lv1.setImageResource(R.drawable.defaulttumb); 
	  //	  }else{
	  //		  Bitmap bm = MediaStore.Video.Thumbnails.getThumbnail(this._activity.getContentResolver(), 
	  //				  (long)Integer.parseInt(paramFileData.id), MediaStore.Video.Thumbnails.MICRO_KIND, null);
	  //		  //lv1.setImageResource(R.drawable.movie_icon);
	  //		  if(bm == null){
	  //			  paramFileData.isTumbAvailable = false;
	  //			  lv1.setImageResource(R.drawable.defaulttumb); 
	  //		  }else{
	  //			  Log.d("============","bm height " + bm.getHeight() + " width = " + bm.getWidth());
	  //			  lv1.setImageBitmap(bm);
	  //		  }
	  //	  }
	  TextView txt1 = (TextView)paramView.findViewById(R.id.filename);
	  txt1.setText(paramFileData.title);
	  // the file size
	  TextView txt2 = (TextView)paramView.findViewById(R.id.filesize);
	  int filesize = Integer.parseInt(paramFileData.size);
	  Log.d("============= size",paramFileData.size);
	  double finalsize ;
	  String finaltxt;
	  java.text.DecimalFormat df =new java.text.DecimalFormat("#.##");
	  if(filesize < 1024){
		  finalsize = filesize;
		  finaltxt = df.format(finalsize) + "B";
	  }else  if(filesize >= 1024 && filesize < 1024*1024){
		  finalsize = ((double) filesize) /1024;
		  finaltxt = df.format(finalsize) + "KB";
	  }else if(filesize >= 1024*1024 && filesize < 1024*1024*1024){
		  finalsize = ((double) filesize) /(1024*1024);
		  finaltxt = df.format(finalsize) + "MB";
	  }else{
		  finalsize = ((double) filesize) /(1024*1024*1024);
		  finaltxt = df.format(finalsize) + "GB";
	  }
	  txt2.setText(finaltxt);
	  // file time length
	  TextView txt3 = (TextView)paramView.findViewById(R.id.filetime);
	  java.text.DecimalFormat df1 =new java.text.DecimalFormat("00");
	  int timetxt = Integer.parseInt(paramFileData.time);
	  Log.d("============= time ",paramFileData.time);
	  int hour = timetxt /(60 *60 *1000);
	  int min = (timetxt % (60*60*1000) )/(60 *1000);
	  int sec = (timetxt % (60*60*1000) ) % (60 * 1000) / 1000;
	  txt3.setText(""+df1.format(hour) + ":"+ df1.format(min) +":"+ df1.format(sec));


  }

  public void addItem(FileData paramFileData)
  {
    this.mItems.add(paramFileData);
  }

  public void clearItems()
  {
    this.mItems.clear();
  }

  public int getCount()
  {
    return this.mItems.size();
  }
  public List<FileData> getItems(){
	  return this.mItems; 
  }
  public FileData getItem(int paramInt)
  {
    int i = this.mItems.size();
    return this.mItems.get(paramInt);/*
    if (paramInt > i);
    for (FileData localFileData = null; ; localFileData = (FileData)this.mItems.get(paramInt))
      return localFileData;
  */
  }

  public long getItemId(int paramInt)
  {
    return paramInt;
  }

  public int getItemType(int paramInt)
  {
    return getItem(paramInt).type;
  }

  public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
  {
	Log.d("============","getView");
    View localView = paramView;
    if (localView == null)
      localView = this._activity.getLayoutInflater().inflate(R.layout.fileitem, null);
    FileData localFileData = getItem(paramInt);
    _updateView(localView, localFileData);
    return localView;
  }
}


class FileData{
	public int type;
	public String id ;
	public String title;
	public String size;
	public String time;
	public boolean isTumbAvailable;
	public Bitmap bitmap;
	public FileData(int type,String title,String time,String size,String id){
		this.type = type;
		this.title = title;
		this.size = size;
		this.time = time;
		this.id = id;
		this.isTumbAvailable = true;
	}
	public FileData(int type,String title,String time,String size,String id,Bitmap bitmap){
		this.type = type;
		this.title = title;
		this.size = size;
		this.time = time;
		this.id = id;
		this.isTumbAvailable = true;
		this.bitmap = bitmap;
	}
	void SetBitmap(Bitmap bitmap){
		this.bitmap = bitmap;
	}
	
	
}