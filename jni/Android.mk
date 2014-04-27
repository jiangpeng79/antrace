LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS := -fvisibility=hidden
LOCAL_LDLIBS:= -llog
LOCAL_LDFLAGS += -ljnigraphics
LOCAL_MODULE    := antrace
LOCAL_SRC_FILES := potrace/backend_dxf.c
LOCAL_SRC_FILES += potrace/backend_eps.c
LOCAL_SRC_FILES += potrace/backend_geojson.c
LOCAL_SRC_FILES += potrace/backend_pdf.c
LOCAL_SRC_FILES += potrace/backend_pgm.c
LOCAL_SRC_FILES += potrace/backend_svg.c
LOCAL_SRC_FILES += potrace/backend_xfig.c
LOCAL_SRC_FILES += potrace/curve.c
LOCAL_SRC_FILES += potrace/decompose.c
LOCAL_SRC_FILES += potrace/flate.c
LOCAL_SRC_FILES += potrace/getopt.c
LOCAL_SRC_FILES += potrace/getopt1.c
LOCAL_SRC_FILES += potrace/greymap.c
LOCAL_SRC_FILES += potrace/lzw.c
LOCAL_SRC_FILES += potrace/potracelib.c
LOCAL_SRC_FILES += potrace/progress_bar.c
LOCAL_SRC_FILES += potrace/render.c
LOCAL_SRC_FILES += potrace/trace.c
LOCAL_SRC_FILES += potrace/trans.c
LOCAL_SRC_FILES += potrace/bbox.c
LOCAL_SRC_FILES += antrace.cpp
include $(BUILD_SHARED_LIBRARY)
