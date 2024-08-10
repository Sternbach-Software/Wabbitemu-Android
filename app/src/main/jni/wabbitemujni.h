/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class io_github_angelsl_wabbitemu_CalcInterface */

#ifndef _Included_io_github_angelsl_wabbitemu_CalcInterface
#define _Included_io_github_angelsl_wabbitemu_CalcInterface
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SetCacheDir
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_Initialize
		(JNIEnv *env, jclass classObj, jstring filePath);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    LoadFile
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_LoadFile
  (JNIEnv *, jclass, jstring);
/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    LoadFile
 * Signature: (Landroid/net/Uri;)I
 */
/*JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_LoadFile
  (JNIEnv *, jobject, jstring);*/

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SaveCalcState
 * Signature: (Ljava/lang/String;)B
 */
JNIEXPORT jboolean JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_SaveCalcState
  (JNIEnv *, jclass, jstring);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    CreateRom
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_CreateRom
  (JNIEnv *, jclass, jstring, jstring, jstring, jint);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    ResetCalc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_ResetCalc
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    RunCalcs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_RunCalcs
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    PauseCalc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_PauseCalc
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    UnpauseCalc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_UnpauseCalc
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    GetModel
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_GetModel
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    Tstates
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_Tstates
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SetSpeedCalc
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_SetSpeedCalc
  (JNIEnv *, jclass, jint);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    PressKey
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_PressKey
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    ReleaseKey
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_ReleaseKey
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    SetAutoTurnOn
 * Signature: (B)V
 */
JNIEXPORT void JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_SetAutoTurnOn
  (JNIEnv *env, jclass classObj, jboolean turnOn);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    IsLCDActive
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_IsLCDActive
  (JNIEnv *, jclass);

/*
 * Class:     io_github_angelsl_wabbitemu_CalcInterface
 * Method:    GetLCD
 * Signature: ([I)I
 */
JNIEXPORT jint JNICALL Java_io_github_angelsl_wabbitemu_calc_CalcInterface_GetLCD
  (JNIEnv *, jclass, jobject);

#ifdef __cplusplus
}
#endif
#endif
