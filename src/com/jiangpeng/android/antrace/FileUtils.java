package com.jiangpeng.android.antrace;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;

public class FileUtils
{
    public static String sep = "/";
    public static String png = ".png";

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
}
