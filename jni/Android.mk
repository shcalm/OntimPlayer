LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false
LOCAL_ARM_MODE := arm
LOCAL_SRC_FILES := \
				   com_android_camera_MovieView.cpp

LOCAL_C_INCLUDES +=  \
					external/jpeg \
					external/libpng \
					external/zlib \
					external/skia/include/core \
					external/skia/include/effects \
					external/skia/include/images \
					external/skia/src/ports \
					external/skia/include/utils

LOCAL_SHARED_LIBRARIES := \
	libjpeg \
    libnativehelper \
    libcutils \
    libutils \
    libz \
	libbinder \
	libskia \
    libui \
    libsurfaceflinger_client
    

LOCAL_STATIC_LIBRARIES:= libpng

LOCAL_MODULE:= libcapture_jni
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)
