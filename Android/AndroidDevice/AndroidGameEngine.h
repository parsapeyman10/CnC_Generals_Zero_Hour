/*
 * AndroidGameEngine.h - جایگزین Win32GameEngine.h برای اندروید
 * 
 * این فایل دقیقا همان ساختار PC را حفظ می‌کند.
 * فقط به جای WinMain.cpp از SDL_main استفاده می‌کند.
 * 
 * PC:   WinMain.cpp -> Win32GameEngine::execute() -> MessageLoop (PeekMessage)
 * Android: GeneralsActivity.java -> SDLActivity -> AndroidGameEngine::execute() -> SDL_PollEvent
 */

#pragma once

#include "Common/GameEngine.h"

// Forward
class IRenderer;
class IAudio;
class IInput;

class AndroidGameEngine : public GameEngine
{
public:
    AndroidGameEngine();
    virtual ~AndroidGameEngine();

    // از GameEngine به ارث رسیده - اینها را برای اندروید پیاده می‌کنیم
    virtual void init(int argc, char *argv[]) override;
    virtual void execute() override;
    virtual void serviceWindowsOS() override; // روی اندروید می‌شود serviceAndroidOS()

protected:
    // Factory ها - اینجا مشخص می‌کنیم روی A30 از چی استفاده شود
    virtual LocalFileSystem* createLocalFileSystem() override;
    virtual ArchiveFileSystem* createArchiveFileSystem() override;
    virtual GameLogic* createGameLogic() override;          // همین GameLogic PC - بدون تغییر
    virtual GameClient* createGameClient() override;        // همین GameClient PC - بدون تغییر
    virtual ModuleFactory* createModuleFactory() override;  // بدون تغییر
    virtual ThingFactory* createThingFactory() override;
    virtual FunctionLexicon* createFunctionLexicon() override;
    virtual Radar* createRadar() override;
    virtual WebBrowser* createWebBrowser() override;        // روی موبایل null برمی‌گردانیم (اخبار GameSpy مرده)
    virtual ParticleSystemManager* createParticleSystemManager() override;
    virtual AudioManager* createAudioManager() override;    // اینجا OpenAL برمی‌گردانیم نه Miles

private:
    IRenderer* m_renderer;  // W3D_GLES برای Mali-G71
    IAudio* m_audio;
    IInput* m_input;

    bool m_isPaused;        // وقتی کاربر Home را می‌زند
    int m_width, m_height;  // برای A30: 1280x720 پیشنهاد می‌شود نه 2340x1080

    void handleAndroidLifecycle();
    void handleTouchToMouse(); // تبدیل TouchEvent به MouseEvent قدیمی
};

// این تابع جایگزین WinMain می‌شود
// در app/src/main/cpp/main.cpp صدا زده می‌شود:
// extern "C" int SDL_main(int argc, char *argv[]) { return GameMain(argc, argv); }
GameEngine* CreateGameEngineAndroid();
