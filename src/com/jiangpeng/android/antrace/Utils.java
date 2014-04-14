package com.jiangpeng.android.antrace;

import com.jiangpeng.android.antrace.Objects.path;

import android.graphics.Bitmap;

public class Utils {
	public static native void threshold(Bitmap input, int t, Bitmap output);
	public static native void grayScale(Bitmap input, Bitmap output);
	public static native path traceImage(Bitmap input);
	public static native void unsharpMask(Bitmap input);
	public static native boolean saveSVG(String filename, int w, int h);
	public static native boolean saveDXF(String filename, int w, int h);
	public static native boolean savePDF(String filename, int w, int h);
	public static native void clearState();
}
