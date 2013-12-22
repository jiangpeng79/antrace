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

JNIEXPORT void JNICALL
Java_com_jiangpeng_android_antrace_Utils_process( JNIEnv* env, jobject thiz, jobject bitmap, jobject input )
{
	AndroidBitmapInfo info;
	int ret = 0;
	void* src_pixels = 0;
	void* dst_pixels = 0;

	if ((ret = AndroidBitmap_getInfo(env, input, &info)) < 0) {
		return;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		return;
	}
	if ((ret = AndroidBitmap_lockPixels(env, input, &src_pixels)) < 0) {
	}

	potrace_param_t* param_t = potrace_param_default();
	potrace_bitmap_t* bmp_t = bm_new(info.width, info.height);
	bm_clear(bmp_t, 0);
	dst_pixels = bmp_t->map;

	const int kShiftBits = 20;
	const int32_t kRedRatio = static_cast<int32_t>((1 << kShiftBits) * 0.21f);
	const int32_t kGreenRatio = static_cast<int32_t>((1 << kShiftBits) * 0.71f);
	const int32_t kBlueRatio = static_cast<int32_t>((1 << kShiftBits) * 0.07f);
	for (uint32_t scan_line = 0; scan_line < info.height; scan_line++) {
	    uint32_t* dst = reinterpret_cast<uint32_t*>(dst_pixels);
	    pixel32_t* src = reinterpret_cast<pixel32_t*>(src_pixels);
	    pixel32_t* src_line_end = src + info.width;
	    while (src < src_line_end) {
	    	int32_t src_red = src->rgba8[0];
	    	int32_t src_green = src->rgba8[1];
	    	int32_t src_blue = src->rgba8[2];
	    	int32_t src_alpha = src->rgba8[3];

	    	int32_t dst_color = (kRedRatio * src_red + kGreenRatio * src_green +
	    			kBlueRatio * src_blue) >> kShiftBits;
	    	if (dst_color > 128) {
	    		dst_color = 255;
	    	}
	    	else
	    	{
	    		dst_color = 0;
	    	}
	    	*dst = (src_alpha << 24) | (dst_color << 16) | (dst_color << 8) | dst_color;
	    	dst++;
	    	src++;
	    }
	    dst_pixels = reinterpret_cast<char*>(dst_pixels) + bmp_t->dy;
	    src_pixels = reinterpret_cast<char*>(src_pixels) + info.stride;
	}
	potrace_state_t* state = potrace_trace(param_t, bmp_t);
	potrace_param_free(param_t);
	bm_free(bmp_t);

	AndroidBitmap_unlockPixels(env, input);
}
}
