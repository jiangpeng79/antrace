package com.jiangpeng.android.antrace;

import java.io.File;
import java.io.IOException;
import android.media.ExifInterface;
import android.os.Environment;
import android.content.Context;

public class FileUtils
{
    public static String sep = "/";
    public static String png = ".png";
    public static String FILE_NAME = "FILE_NAME";
	public static boolean sdcardExists()
	{
	    String state = Environment.getExternalStorageState();
	    if (!Environment.MEDIA_MOUNTED.equals(state))
	    {
	    	return false;
	    }
		File file = Environment.getExternalStorageDirectory();
		return file.exists() && file.isAbsolute() && file.canRead();
	}
	
	public static String tempSvgFile(Context ctx)
	{
		return ctx.getExternalCacheDir().toString() + FileUtils.sep + "__temp_svg.svg";
	}

	public static boolean folderExists(String folder)
	{
		File file = new File(folder);
		return file.exists() && file.isAbsolute() && file.canRead();
	}
	
	public static boolean checkAndCreateFolder(String folder)
	{
		File file = new File(folder);
		if(file.exists() && file.isAbsolute() && file.canRead())
		{
			return true;
		}
		return file.mkdir();
	}
		
	public static boolean isPngFile(String filename)
	{
		String lower = filename.toLowerCase();
		return lower.endsWith(FileUtils.png);
	}
	
	public static boolean isJpgFile(String filename)
	{
		String lower = filename.toLowerCase();
		return lower.endsWith(".jpg");
	}
	
	public static boolean isBmpFile(String filename)
	{
		String lower = filename.toLowerCase();
		return lower.endsWith(".bmp");
	}

	public static String getCacheDir(Context ctx)
	{
		return ctx.getExternalCacheDir().toString();
	}

    public static String getRootFolder()
    {
    	return Environment.getExternalStorageDirectory().getPath();
    }
	
	public static boolean fileExists(String filename)
	{
	    File file = new File(filename);
	    return file.exists() && file.isFile();
	}
    
    public static float getPhotoAngle(String filename)
    {
    	ExifInterface exif;
		try {
			exif = new ExifInterface(filename);
		} catch (IOException e) {
			return 0;
		}
		int r = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
		if(r == ExifInterface.ORIENTATION_ROTATE_270)
   		{
   			return 270.0f;
   		}
   		else if(r == ExifInterface.ORIENTATION_ROTATE_180)
   		{
   			return 180.0f;
   		}
   		else if(r == ExifInterface.ORIENTATION_ROTATE_90)
   		{
   			return 90.0f;
   		}
		return 0;
    }
    
	public static String getFileStem(String filename)
	{
		int i = filename.lastIndexOf(".");
		if(i == -1)
		{
			return filename;
		}
		return filename.substring(0, i);
	}
	
	public static String getShortName(String filename)
	{
        String shortName = filename;
        int i = filename.lastIndexOf(FileUtils.sep);
        if (i >= 0)
        {
            shortName = filename.substring(i + 1, filename.length());
        }
        return shortName;
	}
	
	public static String getPath(String filename)
	{
        String path = filename;
        int i = filename.lastIndexOf(FileUtils.sep);
        if (i >= 0)
        {
            path = filename.substring(0, i);
        }
        return path;
	}
}
