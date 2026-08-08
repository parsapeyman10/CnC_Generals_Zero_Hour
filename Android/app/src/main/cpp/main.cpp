/*
 * main.cpp - نقطه ورود اندروید (جایگزین WinMain.cpp)
 * 
 * روی PC: WinMain() در Generals/Code/Main/WinMain.cpp
 * روی Android: SDL_main() که توسط SDLActivity.java صدا زده می‌شود
 * 
 * ساختار PC کاملا حفظ می‌شود: فقط WinMain -> SDL_main تغییر می‌کند
 * بقیه GameMain() بدون تغییر می‌ماند
 */

#include <SDL.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "Generals"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// این تابع از GameEngine است - همان که WinMain صدا می‌زد
extern void GameMain(int argc, char *argv[]);
extern void* CreateGameEngineAndroid();

// برای اینکه Generals فکر کند آرگومان دارد
static int g_argc = 1;
static char* g_argv[] = { (char*)"Generals", nullptr };

// SDL نقطه ورود اندروید را SDL_main می‌نامد نه main
extern "C" int SDL_main(int argc, char *argv[])
{
    LOGI("Generals Android starting on A30 - SDL_main called with %d args", argc);
    LOGI("Preserving PC structure: GameLogic/GameClient unchanged");

    // تنظیمات مخصوص A30
    // رزولوشن منطقی: 1280x720 نه 2340x1080 (برای Mali-G71)
    SDL_SetHint(SDL_HINT_ORIENTATIONS, "Landscape");
    
    // GameMain همان حلقه اصلی PC است:
    // while (!TheGameEngine->getQuitting()) { TheGameEngine->update(); }
    // فقط داخل AndroidGameEngine::serviceWindowsOS() به SDL_PollEvent تبدیل شده
    GameMain(g_argc, g_argv);

    LOGI("Generals Android shutting down");
    return 0;
}

// Lifecycle برای زمانی که کاربر Home را می‌زند (مثل ALT+TAB در PC)
extern "C" void Java_com_ea_generals_GeneralsActivity_onPause(JNIEnv* env, jobject obj)
{
    LOGI("onPause - pausing game (like losing focus on PC)");
    // TheGameEngine->setIsActive(false);
}

extern "C" void Java_com_ea_generals_GeneralsActivity_onResume(JNIEnv* env, jobject obj)
{
    LOGI("onResume - resuming game");
    // TheGameEngine->setIsActive(true);
}
