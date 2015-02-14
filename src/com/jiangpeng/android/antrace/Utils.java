package com.jiangpeng.android.antrace;

import com.jiangpeng.android.antrace.Objects.path;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class Utils {
	public static native void threshold(Bitmap input, int t, Bitmap output);
	public static native void grayScale(Bitmap input, Bitmap output);
	public static native path traceImage(Bitmap input);
	public static native void unsharpMask(Bitmap input, Bitmap output);
	public static native boolean saveSVG(String filename, int w, int h);
	public static native boolean saveDXF(String filename, int w, int h);
	public static native boolean savePDF(String filename, int w, int h);
	public static native void clearState();
	
	private static android.content.pm.PackageInfo getPackageInfo(Context context)
	{
        android.content.pm.PackageInfo pi = null;
        try
        {
            String name = context.getPackageName();
            PackageManager pm = context.getPackageManager();

            pi = pm.getPackageInfo(name, 0);
        } catch (Exception err)
        {
            return null;
        }
        return pi;
    }
	
	public static String getCurrentVersionName(Context context)
	{
        android.content.pm.PackageInfo pi = getPackageInfo(context);
        return pi == null ? "0.0" : pi.versionName;
	}

	public static DisplayMetrics getDisplayMetrics(Context context)
	{
	    DisplayMetrics metrics = new DisplayMetrics();
	    WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
	    wm.getDefaultDisplay().getMetrics(metrics); 
	    
	    return metrics;
	}
	
    public static int getDPI(Context context)
    {
	    DisplayMetrics metrics = getDisplayMetrics(context);
	    return metrics.densityDpi;
    }
   
}
