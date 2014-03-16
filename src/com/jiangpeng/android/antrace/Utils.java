package com.jiangpeng.android.antrace;

import com.jiangpeng.android.antrace.Objects.path;

import android.graphics.Bitmap;

public class Utils {
	public static native void threshold(Bitmap input, Bitmap output);
	public static native path traceImage(Bitmap input);
}
