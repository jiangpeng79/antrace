#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <getopt.h>
#include <math.h>

#include "potrace/main.h"
#include "potrace/potracelib.h"
#include "potrace/backend_pdf.h"
#include "potrace/backend_eps.h"
#include "potrace/backend_pgm.h"
#include "potrace/backend_svg.h"
#include "potrace/backend_xfig.h"
#include "potrace/backend_dxf.h"
#include "potrace/backend_geojson.h"
#include "potrace/potracelib.h"
#include "potrace/bitmap_io.h"
#include "potrace/bitmap.h"
#include "potrace/platform.h"
#include "potrace/auxiliary.h"
#include "potrace/progress_bar.h"
#include "potrace/trans.h"
#include "potrace/curve.h"
#include <android/bitmap.h>
#include <android/log.h>
#define LOG_TAG "libbitmaputils"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
extern "C"
{
union pixel32_t {
  uint32_t rgba32;
  uint8_t rgba8[4];  // 0: red 1:green 2:blue 3:alpha
};

typedef union pixel32_t pixel32_t;
potrace_state_t* s_state = NULL;

/*
jobject createPoint(JNIEnv* env, point_s* pt)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/point");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "L");
	env->SetLongField(ret, fid, pt->x);

	fid = env->GetFieldID(cls, "y", "L");
	env->SetLongField(ret, fid, pt->y);
	return ret;
}

jobject createiPoint(JNIEnv* env, ipoint_t* pt)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/ipoint");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "void(V)");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "I");
	env->SetIntField(ret, fid, pt->x);

	fid = env->GetFieldID(cls, "y", "I");
	env->SetIntField(ret, fid, pt->y);
	return ret;
}
*/

jobject createdPoint(JNIEnv* env, dpoint_t* pt)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "D");
	env->SetDoubleField(ret, fid, pt->x);

	fid = env->GetFieldID(cls, "y", "D");
	env->SetDoubleField(ret, fid, pt->y);
	env->DeleteLocalRef(cls);
	return ret;
}

/*
jobject createDummyDPoint(JNIEnv* env)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "D");
	env->SetDoubleField(ret, fid, 0.0);

	fid = env->GetFieldID(cls, "y", "D");
	env->SetDoubleField(ret, fid, 0.0);
	return ret;
}

jobject createSums(JNIEnv* env, sums_t* s)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/sums");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "x", "I");
	env->SetIntField(ret, fid, s->x);

	fid = env->GetFieldID(cls, "y", "I");
	env->SetIntField(ret, fid, s->y);

	fid = env->GetFieldID(cls, "xy", "I");
	env->SetIntField(ret, fid, s->xy);

	fid = env->GetFieldID(cls, "x2", "I");
	env->SetIntField(ret, fid, s->x2);

	fid = env->GetFieldID(cls, "y2", "I");
	env->SetIntField(ret, fid, s->y2);
	return ret;
}

jobject createprevCurve(JNIEnv* env, privcurve_t* c)
{
	jobject ret;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/privcurve");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	jfieldID fid = env->GetFieldID(cls, "n", "I");
	env->SetIntField(ret, fid, c->n);

	jintArray tags = env->NewIntArray(c->n);
	/*
	//jint* ia = new jint[c->n];
	//for(int i = 0; i < c->n; ++c->n)
	//{
	//	ia[i] = c->tag[i];
	//}
	env->SetIntArrayRegion(tags, 0, c->n, ia);
	delete[] ia;
	env->SetIntArrayRegion(tags, 0, c->n, c->tag);
	fid = env->GetFieldID(cls, "tag", "[I");
	env->SetObjectField(ret, fid, tags);

	cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
//	jobject newDPt = createDummyDPoint(env);
//	jobjectArray initarr = env->NewObjectArray(3, cls, newDPt);
	jobjectArray arr = env->NewObjectArray(c->n, env->FindClass("[Ljava/lang/Object;"), 0);
	for(int i = 0; i < c->n; ++i)
	{
		jobjectArray ptarr = env->NewObjectArray(3, cls, 0);
		for(int j = 0; j < 3; ++j)
		{
			jobject pt = createdPoint(env, &c->c[i][j]);
			env->SetObjectArrayElement(ptarr, j, pt);
		}
		env->SetObjectArrayElement(arr, i, ptarr);
	}
	fid = env->GetFieldID(cls, "c", "[[O");
	env->SetObjectField(ret, fid, arr);

	fid = env->GetFieldID(cls, "alphacurve", "I");
	env->SetIntField(ret, fid, c->alphacurve);
	int hasAlpha = c->alphacurve;
	if(hasAlpha)
	{
		cls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
		jobjectArray arr = env->NewObjectArray(c->n, env->FindClass("[Ljava/lang/Object;"), 0);
		for(int i = 0; i < c->n; ++i)
		{
			jobject pt = createdPoint(env, &c->vertex[i]);
			env->SetObjectArrayElement(arr, i, pt);
		}

		fid = env->GetFieldID(cls, "vertex", "[O");
		env->SetObjectField(ret, fid, arr);

		jdoubleArray alpha = env->NewDoubleArray(c->n);
		env->SetDoubleArrayRegion(alpha, 0, c->n, c->alpha);
		fid = env->GetFieldID(cls, "alpha", "[D");
		env->SetObjectField(ret, fid, alpha);

		jdoubleArray alpha0 = env->NewDoubleArray(c->n);
		env->SetDoubleArrayRegion(alpha0, 0, c->n, c->alpha0);
		fid = env->GetFieldID(cls, "alpha0", "[D");
		env->SetObjectField(ret, fid, alpha0);

		jdoubleArray beta = env->NewDoubleArray(c->n);
		env->SetDoubleArrayRegion(beta, 0, c->n, c->beta);
		fid = env->GetFieldID(cls, "beta", "[D");
		env->SetObjectField(ret, fid, beta);
	}
	return ret;
}
*/

jobject createCurve(JNIEnv* env, potrace_curve_t* c)
{
	jobject ret;
	jclass crvcls = env->FindClass("com/jiangpeng/android/antrace/Objects/curve");
	jmethodID constructor = env->GetMethodID(crvcls, "<init>", "()V");
	ret = env->NewObject(crvcls, constructor);

	jfieldID fid = env->GetFieldID(crvcls, "n", "I");
	env->SetIntField(ret, fid, c->n);

	jintArray tags = env->NewIntArray(c->n);
	jint* ia = new jint[c->n];
	for(int i = 0; i < c->n; ++i)
	{
		ia[i] = c->tag[i];
	}
	env->SetIntArrayRegion(tags, 0, c->n, ia);
	delete[] ia;

	fid = env->GetFieldID(crvcls, "tag", "[I");
	env->SetObjectField(ret, fid, tags);
	env->DeleteLocalRef(tags);
	
	jclass dptcls = env->FindClass("com/jiangpeng/android/antrace/Objects/dpoint");
//	jobject newDPt = createDummyDPoint(env);
//	jobjectArray initarr = env->NewObjectArray(3, cls, newDPt);
	jclass d1cls = env->FindClass("[Lcom/jiangpeng/android/antrace/Objects/dpoint;");
	jobjectArray arr = env->NewObjectArray(c->n, d1cls, 0);
	for(int i = 0; i < c->n; ++i)
	{
		jobjectArray ptarr = env->NewObjectArray(3, dptcls, 0);
		for(int j = 0; j < 3; ++j)
		{
			jobject pt = createdPoint(env, &c->c[i][j]);
			env->SetObjectArrayElement(ptarr, j, pt);
			env->DeleteLocalRef(pt);
		}
		env->SetObjectArrayElement(arr, i, ptarr);
		env->DeleteLocalRef(ptarr);
	}
	env->DeleteLocalRef(crvcls);
	env->DeleteLocalRef(dptcls);
	fid = env->GetFieldID(crvcls, "c", "[[Lcom/jiangpeng/android/antrace/Objects/dpoint;");
	env->SetObjectField(ret, fid, arr);
	env->DeleteLocalRef(arr);
	env->DeleteLocalRef(d1cls);
	return ret;
}

/*
jobject createPath(JNIEnv* env, potrace_path_t* path, jobject parent)
{
	jobject ret = 0;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/path");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	if(parent != 0)
	{
		jfieldID fid = env->GetFieldID(cls, "next", "Lcom/jiangpeng/android/antrace/Objects/path;");
		env->SetObjectField(parent, fid, ret);
	}
	env->DeleteLocalRef(cls);

	potrace_curve_t *curve = &path->curve;
	jobject jcurve = createCurve(env, curve);

	jfieldID fid = env->GetFieldID(cls, "curve", "Lcom/jiangpeng/android/antrace/Objects/curve;");
	env->SetObjectField(ret, fid, jcurve);
	env->DeleteLocalRef(jcurve);

	if(path->next != NULL)
	{
		jobject dummy = createPath(env, path->next, ret);
		env->DeleteLocalRef(dummy);
	}
	return ret;
}
*/

jobject createPath(JNIEnv* env, potrace_path_t* path)
{
	jobject ret = 0;
	jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/path");
	jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
	ret = env->NewObject(cls, constructor);

	env->DeleteLocalRef(cls);

	potrace_curve_t *curve = &path->curve;
	jobject jcurve = createCurve(env, curve);

	jfieldID fid = env->GetFieldID(cls, "curve", "Lcom/jiangpeng/android/antrace/Objects/curve;");
	env->SetObjectField(ret, fid, jcurve);
	env->DeleteLocalRef(jcurve);

	return ret;
}

JNIEXPORT jobject JNICALL
Java_com_jiangpeng_android_antrace_Utils_traceImage( JNIEnv* env, jobject thiz, jobject bitmap)
{
	AndroidBitmapInfo info;
	int ret = 0;
	void* src_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		return NULL;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return NULL;
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &src_pixels)) < 0) {
		return NULL;
	}

	potrace_param_t* param_t = potrace_param_default();
	potrace_bitmap_t* bmp_t = bm_new(info.width, info.height);
	//memcpy(bmp_t->map, src_pixels, bmp_t->dy * bmp_t->h * BM_WORDSIZE);

	const int kShiftBits = 20;
	const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
	const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
	const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);
	for (uint32_t scan_line = 0; scan_line < info.height; scan_line++) {
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + info.width;
	    int x = 0;
	    while (src < src_line_end) {
	    	int32_t src_red = src->rgba8[0];
	    	int32_t src_green = src->rgba8[1];
	    	int32_t src_blue = src->rgba8[2];
	    	int32_t src_alpha = src->rgba8[3];

	    	int32_t dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
	    			kBlueRatio * src_blue) >> kShiftBits;
	    	if (dst_color > 128) {
    	      BM_PUT(bmp_t, x, scan_line, 1);
	    	}
	    	else
	    	{
    	      BM_PUT(bmp_t, x, scan_line, 0);
	    	}
	    	src++;
	    	++x;
	    }
	    src_pixels = reinterpret_cast<char*>(src_pixels) + info.stride;
	}
	potrace_state_t* state = potrace_trace(param_t, bmp_t);
	potrace_param_free(param_t);
	bm_free(bmp_t);

	AndroidBitmap_unlockPixels(env, bitmap);

    if (!state || state->status != POTRACE_STATUS_OK) {
    	return NULL;
    }

    potrace_path_t* start = state->plist;
    jobject prev = 0;
    jclass cls = env->FindClass("com/jiangpeng/android/antrace/Objects/path");
    jobject retPath = 0;
    for(potrace_path_t* n = start; n != 0; n = n->next)
    {
    	jobject path = createPath(env, n);
    	if(retPath == 0)
    	{
    		retPath = path;
    	}
    	if(prev != 0)
    	{
    		jfieldID fid = env->GetFieldID(cls, "next", "Lcom/jiangpeng/android/antrace/Objects/path;");
    		env->SetObjectField(prev, fid, path);
    		env->DeleteLocalRef(prev);
    	}
    	prev = path;
    }
    env->DeleteLocalRef(cls);
    potrace_state_free(state);

	return retPath;
}

JNIEXPORT void JNICALL
Java_com_jiangpeng_android_antrace_Utils_threshold( JNIEnv* env, jobject thiz, jobject input, jint t, jobject output )
{
	AndroidBitmapInfo inputInfo;
	AndroidBitmapInfo outputInfo;
	int ret = 0;
	void* src_pixels = 0;
	void* dst_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, input, &inputInfo)) < 0) {
		return;
	}

	if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, input, &src_pixels)) < 0) {
		return;
	}

	if ((ret = AndroidBitmap_getInfo(env, output, &outputInfo)) < 0) {
		return;
	}

	if (outputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, output, &dst_pixels)) < 0) {
		return;
	}

	const int kShiftBits = 20;
	const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
	const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
	const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);
	for (uint32_t scan_line = 0; scan_line < outputInfo.height; scan_line++) {
	    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + inputInfo.width;
	    int y = 0;
	    while (src < src_line_end) {
	    	int32_t src_red = src->rgba8[0];
	    	int32_t src_green = src->rgba8[1];
	    	int32_t src_blue = src->rgba8[2];
	    	int32_t src_alpha = src->rgba8[3];

	    	int32_t dst_color = src_red;
	    	if(src_red != src_green || src_green != src_blue)
	    	{
	    		dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
	    				kBlueRatio * src_blue) >> kShiftBits;
	    	}
	    	if (dst_color > t) {
	    		dst_color = 255;
	    	}
	    	else
	    	{
	    		dst_color = 0;
	    	}
	    	*dst = (src_alpha << 24) | (dst_color << 16) | (dst_color << 8) | dst_color;
	    	dst++;
	    	src++;
	    	++y;
	    }
	    dst_pixels = reinterpret_cast<char*>(dst_pixels) + outputInfo.stride;
	    src_pixels = reinterpret_cast<char*>(src_pixels) + inputInfo.stride;
	}

	AndroidBitmap_unlockPixels(env, input);
	AndroidBitmap_unlockPixels(env, output);
}

JNIEXPORT void JNICALL
Java_com_jiangpeng_android_antrace_Utils_grayScale( JNIEnv* env, jobject thiz, jobject input, jobject output )
{
	AndroidBitmapInfo inputInfo;
	AndroidBitmapInfo outputInfo;
	int ret = 0;
	void* src_pixels = 0;
	void* dst_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, input, &inputInfo)) < 0) {
		return;
	}

	if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, input, &src_pixels)) < 0) {
		return;
	}

	if ((ret = AndroidBitmap_getInfo(env, output, &outputInfo)) < 0) {
		return;
	}

	if (outputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, output, &dst_pixels)) < 0) {
		return;
	}

	const int kShiftBits = 20;
	const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
	const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
	const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);
	for (uint32_t scan_line = 0; scan_line < outputInfo.height; scan_line++) {
	    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + inputInfo.width;
	    while (src < src_line_end) {
	    	int32_t src_red = src->rgba8[0];
	    	int32_t src_green = src->rgba8[1];
	    	int32_t src_blue = src->rgba8[2];
	    	int32_t src_alpha = src->rgba8[3];

	    	int32_t dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
	    			kBlueRatio * src_blue) >> kShiftBits;
	    	if (dst_color > 255) {
	    		dst_color = 255;
	    	}
	    	*dst = (src_alpha << 24) | (dst_color << 16) | (dst_color << 8) | dst_color;
	    	dst++;
	    	src++;
	    }
	    dst_pixels = reinterpret_cast<char*>(dst_pixels) + outputInfo.stride;
	    src_pixels = reinterpret_cast<char*>(src_pixels) + inputInfo.stride;
	}

	AndroidBitmap_unlockPixels(env, input);
	AndroidBitmap_unlockPixels(env, output);
}

JNIEXPORT void JNICALL Java_com_jiangpeng_android_antrace_Utils_saveSVG(JNIEnv * env, jobject jobj, jstring path)
{
    char const * filepath = env->GetStringUTFChars(path , NULL);
}

}
