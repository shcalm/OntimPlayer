package com.ontim.player;
/*package com.ontim.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class SubTitle
{
  public Context context;
  private int currentIndex;
  private SubtitleItem[] listSubTitle;

  private List mArrayList = null;
  SharedPreferences sharep = null;

  public SubTitle(Context paramContext)
  {
    this.context = paramContext;
    SharedPreferences localSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    this.sharep = localSharedPreferences;
  }

  public static String getCharset(File paramFile)
  {
    String str1 = "GBK";
    byte[] arrayOfByte = new byte[3];
    int i = 0;
    try
    {
      FileInputStream localFileInputStream = new FileInputStream(paramFile);
      BufferedInputStream localBufferedInputStream = new BufferedInputStream(localFileInputStream);
      localBufferedInputStream.mark(0);
      String str2;
      if (localBufferedInputStream.read(arrayOfByte, 0, 3) == -1)
      {
        str2 = "GBK";
        return str2;
      }
      int k;
      if ((arrayOfByte[0] == -1) && (arrayOfByte[1] == -1))
      {
        str1 = "UTF-16LE";
        i = 1;
        localBufferedInputStream.reset();
        if (i == 0)
        {
          int j = 0;
          k = localBufferedInputStream.read();
          if (k != -1)
          {
            j += 1;
            if (k < 240)
          }
        }
      }
      while (true)
      {
        localBufferedInputStream.close();
        str2 = str1;
       // break;
        if ((arrayOfByte[0] == -1) && (arrayOfByte[1] == -1))
        {
          str1 = "UTF-16BE";
          i = 1;
        }
        if ((arrayOfByte[0] != 65519) || (arrayOfByte[1] != 65467) || (arrayOfByte[2] != 65471))
        str1 = "UTF-8";
        i = 1;
        label184: if ((128 <= k) && (k <= 191))
          continue;
        if ((192 <= k) && (k <= 223))
        {
          k = localBufferedInputStream.read();
          if ((128 > k) || (k > 191))
            continue;
        }
        if ((224 > k) || (k > 239))
        k = localBufferedInputStream.read();
        if ((128 > k) || (k > 191))
          continue;
        k = localBufferedInputStream.read();
        if ((128 > k) || (k > 191))
          continue;
        str1 = "UTF-8";
      }
    }
    catch (Exception localException)
    {
      while (true)
        localException.printStackTrace();
    }
  
  }
  private String getExtSubtitleFilePath(String paramString)
  {
	return paramString;
	}

  private boolean isMatchSubtitle(String paramString1, String paramString2){
	  String str1 = paramString2.toLowerCase();
	  String str2 = paramString1.toLowerCase();
	  if ((str1.startsWith(str2)) && (paramString2.toLowerCase().endsWith("srt")))
		  return true;
	  else
		  return false;
  }

  public String getSubtitleText(long paramLong){return null;}


  public boolean loadSubTitleForVideoFile(String paramString)
  {
    String str = getExtSubtitleFilePath(paramString);
    if ((str != null) && (str.length() > 0));
    for (boolean bool = loadSubTitle(str); ; bool = false)
      return bool;
  }

  private boolean loadSubTitle(String str) {
	// TODO Auto-generated method stub
	return false;
}

public class SubtitleItem
  {
    long beginTime;
    String content = "";
    long endTime;
    int titleIndex;
  }
}
*/