#include "wabbitemujni.h"
#include "calc.h"
#include "linksendvar.h"
#include "sendfile.h"
#include "exportvar.h"
#include <pthread.h>
#include <android/log.h>

#include <android/log.h>
#include <jni.h>
//#include <android/content/ContentProvider.h>
//#include <android/content/res/AssetManager.h>
//

static LPCALC lpCalc;
static int redPalette[256];
static int bluePalette[256];
static int greenPalette[256];
char cache_dir[MAX_PATH];
static long staticThreadId = -1;

void load_settings(LPCALC lpCalc, LPVOID lParam) {
	lpCalc->running = TRUE;
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SetCacheDir
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_Initialize
		(JNIEnv *env, jclass classObj, jstring filePath) {
	const char *path = (*env)->GetStringUTFChars(env, filePath, JNI_FALSE);
	strcpy(cache_dir, path);
	lpCalc = calc_slot_new();
	lpCalc->model = INVALID_MODEL;
	calc_register_event(lpCalc, ROM_LOAD_EVENT, &load_settings, NULL);

	for (int i = 0; i < 256; i++) {
		redPalette[i] = (0x9E * (256 - i)) / 255;
		bluePalette[i] = (0x88 * (256 - i)) / 255;
		greenPalette[i] = (0xAB * (256 - i)) / 255;
	}
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SaveCalcState
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jboolean JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_SaveCalcState
		(JNIEnv *env, jclass classObj, jstring filePath) {
	const char *path = (*env)->GetStringUTFChars(env, filePath, JNI_FALSE);

    SAVESTATE_t *save = SaveSlot(lpCalc, "Wabbitemu", "Automatic save state");
	BOOL wasSuccessful = FALSE;
	if (save != NULL) {
		wasSuccessful = WriteSave(path, save, ZLIB_CMP);
		FreeSave(save);
	}

	return (jboolean) (save != NULL && wasSuccessful? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    CreateRom
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_CreateRom
	(JNIEnv *env, jclass classObj, jstring jOsPath, jstring jBootPath,
			jstring jRomPath, jint model) {
	const char *osPath = (*env)->GetStringUTFChars(env, jOsPath, JNI_FALSE);
	const char *bootPath = (*env)->GetStringUTFChars(env, jBootPath, JNI_FALSE);
	const char *romPath = (*env)->GetStringUTFChars(env, jRomPath, JNI_FALSE);

	// Do not allow more than one calc currently
	if (lpCalc) {
		calc_slot_free(lpCalc);
	}

	lpCalc = calc_slot_new();
	calc_init_model(lpCalc, model, NULL);

	// slot stuff
	strcpy(lpCalc->rom_path, romPath);
	lpCalc->active = TRUE;
	lpCalc->model = (CalcModel) model;
	lpCalc->cpu.pio.model = model;
	FILE *file = fopen(bootPath, "rb");
	if (file == NULL) {
		return -1;
	}
	writeboot(file, &lpCalc->mem_c, -1);
	fclose(file);
	remove(bootPath);
	TIFILE_t *tifile = importvar(osPath, FALSE);
	if (tifile == NULL) {
		return -2;
	}
	int link_error = forceload_os(&lpCalc->cpu, tifile);
	if (link_error != LERR_SUCCESS) {
		return link_error;
	}

	calc_erase_certificate(lpCalc->mem_c.flash,lpCalc->mem_c.flash_size);
	calc_reset(lpCalc);
	//write the output from file
	MFILE *romfile = ExportRom((char *) romPath, lpCalc);
	if (romfile == NULL) {
		return -3;
	}

	calc_register_event(lpCalc, ROM_LOAD_EVENT, &load_settings, NULL);
	mclose(romfile);
	return 0;
}
/*JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_passUriToNative(JNIEnv *env, jobject thiz, jstring uri_string) {
    const char *uriStr = env->GetStringUTFChars(uri_string, 0);
    __android_log_print(ANDROID_LOG_DEBUG, "NativeCode", "Received URI: %s", uriStr);

    // Open the file using content resolver
    AAssetManager *assetManager = AAssetManager_fromJava(env, thiz);
    if (assetManager == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCode", "Failed to get AssetManager");
        return;
    }

    AAsset *asset = AAssetManager_open(assetManager, uriStr, AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCode", "Failed to open asset");
        return;
    }

    // Read file contents
    off_t fileSize = AAsset_getLength(asset);
    char *buffer = (char *) malloc(fileSize + 1);
    AAsset_read(asset, buffer, fileSize);
    buffer[fileSize] = '\0'; // Null-terminate the string

    // Use the file contents (buffer) as needed
    __android_log_print(ANDROID_LOG_DEBUG, "NativeCode", "File contents: %s", buffer);

    // Clean up
    free(buffer);
    AAsset_close(asset);
    env->ReleaseStringUTFChars(uri_string, uriStr);
}*/

JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_LoadFile
		(JNIEnv *env, jclass classObj, jstring filePath) {
    __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", "Trying to load file from JNI");
	const char *path = (*env)->GetStringUTFChars(env, filePath, JNI_FALSE);
    __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", "path: ");
    __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", path);
    TIFILE_t *tifile = importvar(path, TRUE);
	if (!tifile || !lpCalc) {
        __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", "Couldn't print tiff file, returning LERR_FILE (");
        __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", !lpCalc ? "lpCalc is NULL" : "lpCalc is not NULL");
        __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", !tifile ? "tifile is NULL" : "tifile is not NULL");
		return (jint) LERR_FILE;
	}

    __android_log_write(ANDROID_LOG_DEBUG, "Wabbitemu", "Reading SendFile");
	int result = SendFile(lpCalc, path, SEND_CUR);
	return result;
}

JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_ResetCalc
		(JNIEnv *env, jclass classObj) {
	if (!lpCalc) {
		return;
	}

	lpCalc->fake_running = TRUE;
	calc_reset(lpCalc);
	lpCalc->fake_running = FALSE;
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    RunCalc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_RunCalcs
  (JNIEnv *env, jclass classObj) {
	calc_run_all();
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    PauseCalc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_PauseCalc
  (JNIEnv *env, jclass classObj) {
	if (!lpCalc) {
		return;
	}

	lpCalc->running = FALSE;
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    UnpauseCalc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_UnpauseCalc
  (JNIEnv *env, jclass classObj) {
	if (!lpCalc) {
		return;
	}

	lpCalc->running = TRUE;
}

JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_GetModel
  (JNIEnv *env, jclass classObj) {
	if (!lpCalc) {
		return -1;
	}

	return lpCalc->model;
}

JNIEXPORT jlong JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_Tstates
  (JNIEnv *env, jclass classObj) {
	if (!lpCalc) {
		return -1;
	}
	return lpCalc->timer_c.tstates;
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SetSpeedCalc
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_SetSpeedCalc
  (JNIEnv *env, jclass classObj, jint speed) {
	lpCalc->speed = speed;
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    ClearKeys
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_ClearKeys
  (JNIEnv *env, jclass classObj) {
	for (int i = 0; i < 7; i++) {
		for (int j = 0; j < 8; j++) {

		}
	}
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    PressKey
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_PressKey
  (JNIEnv *env, jclass classObj, jint group, jint bit) {
	if (!lpCalc) {
		return;
	}

	keypad_press(&lpCalc->cpu, (int) group, (int) bit);
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SetAutoTurnOn
 * Signature: (B)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_SetAutoTurnOn
  (JNIEnv *env, jclass classObj, jboolean turnOn) {
	auto_turn_on = turnOn ? TRUE : FALSE;
}


/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    ReleaseKey
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_ReleaseKey
  (JNIEnv *env, jclass classObj, jint group, jint bit) {
	if (!lpCalc) {
		return;
	}

	keypad_release(&lpCalc->cpu, (int) group, (int) bit);
}

void CopyWideGrayscale(JNIEnv *env, int *screen, uint8_t *image) {
	for (int i = 0, j = 0; i < LCD_HEIGHT * 128; i++) {
		uint8_t val = image[i];
		screen[j++] = redPalette[val] + (greenPalette[val] << 8) +
				(bluePalette[val] << 16) + 0xFF000000;
	}
}

void CopyGrayscale(JNIEnv *env, int *screen, uint8_t *image) {
	for (int i = 0, j = 0; i < LCD_HEIGHT * 128;) {
		for (int k = 0; k < 96; i++, k++) {
			uint8_t val = image[i];
			screen[j++] = redPalette[val] + (greenPalette[val] << 8) +
					(bluePalette[val] << 16) + 0xFF000000;
		}
		i += 32;
	}
}

void CopyColor(JNIEnv *env, int *screen, uint8_t *image) {
	for (int i = 0, j = 0; i < COLOR_LCD_DISPLAY_SIZE; i+=3) {
		screen[j++] = image[i + 2] + (image[i+1] << 8) + (image[i] << 16) + 0xFF000000;
	}
}

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    GetLCD
 * Signature: ()[B
 */
JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_GetLCD
  (JNIEnv *env, jclass classObj, jobject intBuffer) {
	if (!lpCalc) {
		return FALSE;
	}

	LCDBase_t *lcd = lpCalc->cpu.pio.lcd;
	assert(intBuffer != NULL);

	int *bytes = (int *) (*env)->GetDirectBufferAddress(env, intBuffer);
	if (bytes == NULL) {
		return FALSE;
	}

	uint8_t *image;
	if (lcd != NULL && lcd->active) {
		image = lcd->image(lcd);
	} else {
		size_t size = (lpCalc->model == TI_84PCSE ? COLOR_LCD_DISPLAY_SIZE : GRAY_DISPLAY_SIZE);
		image = (uint8_t *) malloc(size);
		memset(image, 0, size);
	}

	switch (lpCalc->model) {
		case TI_85:
		case TI_86:
			CopyWideGrayscale(env, bytes, image);
			break;
		case TI_84PCSE:
			CopyColor(env, bytes, image);
			break;
		default:
			CopyGrayscale(env, bytes, image);
			break;
	}
	free(image);
	return TRUE;
}

void checkThread() {
	int threadId = pthread_self();
	if (staticThreadId == -1) {
		staticThreadId = threadId;
	} else {
		assert(threadId == staticThreadId);
	}
}
