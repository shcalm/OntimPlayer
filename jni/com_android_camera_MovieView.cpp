/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <JNIHelp.h>
#include <jni.h>

#include <png.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

extern "C" {
    #include "jpeglib.h"
    #include "jerror.h"
}

#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <linux/fb.h>
#include <linux/kd.h>
#include <time.h>
#include <errno.h>

#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>

#include <binder/IMemory.h>
#include <surfaceflinger/ISurfaceComposer.h>

#include <SkImageEncoder.h>
#include <SkBitmap.h>

namespace android {

struct _FBInfo;
typedef struct _FBInfo FBInfo;
typedef int (*UnpackPixel)(FBInfo* fb, unsigned char* pixel, 
	unsigned char* r, unsigned char* g, unsigned char* b);

struct _FBInfo
{
	int fd;
	UnpackPixel unpack;
	unsigned char *bits;
	struct fb_fix_screeninfo fi;
	struct fb_var_screeninfo vi;
};

#define fb_width(fb)  ((fb)->vi.xres)
#define fb_height(fb) ((fb)->vi.yres)
#define fb_bpp(fb)    ((fb)->vi.bits_per_pixel>>3)
#define fb_size(fb)   ((fb)->vi.xres * (fb)->vi.yres * fb_bpp(fb))

static int fb_unpack_rgb565(FBInfo* fb, unsigned char* pixel, 
	unsigned char* r, unsigned char* g, unsigned char* b)
{
	unsigned short color = *(unsigned short*)pixel;

	*r = ((color >> 11) & 0xff) << 3;
	*g = ((color >> 5) & 0xff)  << 2;
	*b = (color & 0xff )<< 3;

	return 0;
}

static int fb_unpack_rgb24(FBInfo* fb, unsigned char* pixel, 
	unsigned char* r, unsigned char* g, unsigned char* b)
{
	*r = pixel[fb->vi.red.offset>>3];
	*g = pixel[fb->vi.green.offset>>3];
	*b = pixel[fb->vi.blue.offset>>3];

	return 0;
}

static int fb_unpack_argb32(FBInfo* fb, unsigned char* pixel, 
	unsigned char* r, unsigned char* g, unsigned char* b)
{
	*r = pixel[fb->vi.red.offset>>3];
	*g = pixel[fb->vi.green.offset>>3];
	*b = pixel[fb->vi.blue.offset>>3];

	return 0;
}

static int fb_unpack_none(FBInfo* fb, unsigned char* pixel, 
	unsigned char* r, unsigned char* g, unsigned char* b)
{
	*r = *g = *b = 0;

	return 0;
}

static void set_pixel_unpacker(FBInfo* fb)
{
	if(fb_bpp(fb) == 2)
	{
		fb->unpack = fb_unpack_rgb565;
	}
	else if(fb_bpp(fb) == 3)
	{
		fb->unpack = fb_unpack_rgb24;
	}
	else if(fb_bpp(fb) == 4)
	{
		fb->unpack = fb_unpack_argb32;
	}
	else
	{
		fb->unpack = fb_unpack_none;
		printf("%s: not supported format.\n", __func__);
	}

	return;
}

static int fb_open(FBInfo* fb, const char* fbfilename)
{
	fb->fd = open(fbfilename, O_RDWR);

	if (fb->fd < 0)
	{
		fprintf(stderr, "can't open %s\n", fbfilename);

		return -1;
	}

	if (ioctl(fb->fd, FBIOGET_FSCREENINFO, &fb->fi) < 0)
		goto fail;

	if (ioctl(fb->fd, FBIOGET_VSCREENINFO, &fb->vi) < 0)
		goto fail;

	fb->bits =(unsigned char*) mmap(0, fb_size(fb), PROT_READ | PROT_WRITE, MAP_SHARED, fb->fd, 0);

	if (fb->bits == MAP_FAILED)
		goto fail;

	printf("---------------framebuffer---------------\n");
	printf("%s: \n  width : %8d\n  height: %8d\n  bpp   : %8d\n  r(%2d, %2d)\n  g(%2d, %2d)\n  b(%2d, %2d)\n",
		fbfilename, fb_width(fb), fb_height(fb), fb_bpp(fb), 
		fb->vi.red.offset, fb->vi.red.length,
		fb->vi.green.offset, fb->vi.green.length,
		fb->vi.blue.offset, fb->vi.blue.length);
	printf("-----------------------------------------\n");

	set_pixel_unpacker(fb);

	return 0;

fail:
	printf("%s is not a framebuffer.\n", fbfilename);
	close(fb->fd);

	return -1;
}

static void fb_close(FBInfo* fb)
{
	munmap(fb->bits, fb_size(fb));
	close(fb->fd);

	return;
}

static int snap2jpg(const char * filename, int quality, FBInfo* fb)
{
	int row_stride = 0; 
	FILE * outfile = NULL;
	JSAMPROW row_pointer[1] = {0};
	struct jpeg_error_mgr jerr;
	struct jpeg_compress_struct cinfo;

	memset(&jerr, 0x00, sizeof(jerr));
	memset(&cinfo, 0x00, sizeof(cinfo));

	cinfo.err = jpeg_std_error(&jerr);
	jpeg_create_compress(&cinfo);
	LOGD("snap2jpg filename = %s ",filename);
//	filename = "/mnt/sdcard/playercapture/234.jpg";
	if ((outfile = fopen(filename, "wb+")) == NULL) 
	{
		fprintf(stderr, "can't open %s\n", filename);
		LOGD("snap2jpg filename = %s errno = %s",filename,strerror(errno));

		return -1;
	}

	jpeg_stdio_dest(&cinfo, outfile);
	cinfo.image_width = fb_width(fb);
	cinfo.image_height = fb_height(fb);
	LOGD("cinfo.image_width = %d,cinfo.image_height = %d",cinfo.image_width,cinfo.image_height);
	cinfo.input_components = 3;
	cinfo.in_color_space = JCS_RGB;
	jpeg_set_defaults(&cinfo);
	jpeg_set_quality(&cinfo, quality, TRUE);
	jpeg_start_compress(&cinfo, TRUE);

	row_stride = fb_width(fb) * 2;
	JSAMPLE* image_buffer = (JSAMPLE*)malloc(3 * fb_width(fb));

	while (cinfo.next_scanline < cinfo.image_height) 
	{
		int i = 0;
		int offset = 0;
		unsigned char* line = fb->bits + cinfo.next_scanline * fb_width(fb) * fb_bpp(fb);

		for(i = 0; i < fb_width(fb); i++, offset += 3, line += fb_bpp(fb))
		{
			fb->unpack(fb, line, image_buffer+offset, image_buffer + offset + 1, image_buffer + offset + 2);
		}

		row_pointer[0] = image_buffer;
		(void) jpeg_write_scanlines(&cinfo, row_pointer, 1);
	}

	jpeg_finish_compress(&cinfo);
	fclose(outfile);

	jpeg_destroy_compress(&cinfo);

	return 0;
}

//Ref: http://blog.chinaunix.net/space.php?uid=15059847&do=blog&cuid=2040565
static int snap2png(const char * filename, int quality, FBInfo* fb)
{
	FILE *outfile;
	if ((outfile = fopen(filename, "wb+")) == NULL)
	{
		fprintf(stderr, "can't open %s\n", filename);
		LOGD("can't open %s\n", filename);
		return -1;
	}
	LOGD("snap2png");
	/* prepare the standard PNG structures */
	png_structp png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING,0,0,0);
	
	png_infop info_ptr = png_create_info_struct(png_ptr);

	/* setjmp() must be called in every function that calls a PNG-reading libpng function */
	if (setjmp(png_jmpbuf(png_ptr)))
	{
		png_destroy_write_struct(&png_ptr, &info_ptr);
		fclose(outfile);
		return -1;
	}

	/* initialize the png structure */
	png_init_io(png_ptr, outfile);

	//
	int width = 0;
	int height = 0;
	int bit_depth = 8;
	int color_type = PNG_COLOR_TYPE_RGB;
	int interlace = 0;
	width = fb_width(fb);
	height = fb_height(fb);

	png_set_IHDR (png_ptr, info_ptr, width, height, bit_depth, color_type,
					(!interlace) ? PNG_INTERLACE_NONE : PNG_INTERLACE_ADAM7,
					PNG_COMPRESSION_TYPE_BASE, PNG_FILTER_TYPE_BASE);

	/* write the file header information */
	png_write_info(png_ptr, info_ptr);

	png_bytep row_pointers[height];
	png_byte* image_buffer =(png_byte*) malloc(3 * width);

	int i = 0;
	int j = 0;
	unsigned char* line = NULL;
	for( ; i < height; i++ )
	{
		line = (unsigned char*)fb->bits + i * width * fb_bpp(fb);
		for(j = 0; j < width; j++, line += fb_bpp(fb))
		{
			int offset = j * 3;
			fb->unpack(fb, line, image_buffer+offset, image_buffer+offset+1, image_buffer+offset+2);
		}
		row_pointers[i] = image_buffer;
		png_write_rows(png_ptr, &row_pointers[i], 1);
	}
	
	png_destroy_write_struct(&png_ptr, &info_ptr);

	fclose(outfile);

	return 0;

}
static void com_android_capture_captureJPEG(JNIEnv *e, jobject o){
#if 1	
		FBInfo fb;
    time_t t = time(0); 
	char tmp[64]; 
	strftime(tmp, sizeof(tmp), "/mnt/sdcard/playercapture/%Y_%m_%d_%H_%M_%S.jpg",localtime(&t));
	const char* filename   = tmp ;
	const char* fbfilename = "/dev/graphics/fb0";
	LOGD("filename %s,fbfilename %s",filename,fbfilename);
//	filename = "/mnt/sdcard/234.jpg";
	memset(&fb, 0x00, sizeof(fb));
	if (fb_open(&fb, fbfilename) == 0)
	{
		if(strstr(filename, ".png") != NULL)
		{
			snap2png(filename, 100, &fb);
		}
		else
		{
			snap2jpg(filename, 100, &fb);
		}
		fb_close(&fb);
	}
#endif
#if 0
    time_t t = time(0); 
	char tmp[64]; 
	strftime(tmp, sizeof(tmp), "/mnt/sdcard/playercapture/%Y_%m_%d_%H_%M_%S.png",localtime(&t));
	const char* filename   = tmp ;
	const String16 name("SurfaceFlinger");
    sp<ISurfaceComposer> composer;
    getService(name, &composer);

    sp<IMemoryHeap> heap;
    uint32_t w, h;
    PixelFormat f;
    status_t err = composer->captureScreen(0, &heap, &w, &h, &f, 0, 0);
    if (err != NO_ERROR) {
        fprintf(stderr, "screen capture failed: %s\n", strerror(-err));
        exit(0);
    }

    LOGD("screen capture success: w=%u, h=%u, pixels=%p\n",
            w, h, heap->getBase());

//    printf("saving file as PNG in %s ...\n", argv[1]);

    SkBitmap b;
    b.setConfig(SkBitmap::kARGB_8888_Config, w, h);
    b.setPixels(heap->getBase());
    SkImageEncoder::EncodeFile(filename, b,
            SkImageEncoder::kPNG_Type, SkImageEncoder::kDefaultQuality);
#endif

}

static JNINativeMethod gMethods[] = {
    {"captureJPEG", "()V",
      (void *)com_android_capture_captureJPEG},
};


int registerMethods(JNIEnv *e){
	   return jniRegisterNativeMethods(e,
      "com/ontim/player/MovieView",
      gMethods, NELEM(gMethods));
}
} // namespace android


jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
   JNIEnv *e;

   LOGD("NFC Service : loading JNI\n");

   // Check JNI version
   if(jvm->GetEnv((void **)&e, JNI_VERSION_1_6))
      return JNI_ERR;

	if(android::registerMethods(e) == -1){
	      return JNI_ERR;
	} 
//   jniRegisterNativeMethods(e,
//    "com/android/camera/MovieView",
//      android::gMethods, NELEM(android::gMethods));


   return JNI_VERSION_1_6;
}
