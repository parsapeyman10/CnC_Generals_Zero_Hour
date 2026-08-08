/*
 * IRenderer.h - Platform Abstraction Layer for Generals on Android
 * 
 * این فایل جایگزین مستقیم Direct3D 8 نیست، فقط یک اینترفیس است
 * تا GameEngine نفهمد روی ویندوز است یا اندروید.
 * 
 * ساختار PC حفظ می‌شود: GameLogic و GameClient بدون تغییر می‌مانند.
 */

#pragma once

#include "Common/GameMemory.h"
#include "GameClient/Display.h"

// ============================================================================
// IRenderer - انتزاع گرافیک
// روی PC: W3DRenderer (dx8wrapper.cpp) این را پیاده می‌کند
// روی Android (A30): GLESRenderer (W3D_GLES.cpp) با OpenGL ES 3.0
// ============================================================================
class IRenderer : public MemoryPoolObject
{
public:
    virtual ~IRenderer() {}

    virtual bool init(void* windowHandle, int width, int height) = 0;
    virtual void shutdown() = 0;

    virtual void beginFrame() = 0;
    virtual void endFrame() = 0;

    // این همان چیزی است که W3DDevice الان صدا می‌زند
    virtual void drawMesh(void* meshData) = 0;
    virtual void drawTerrain(void* terrainData) = 0;
    virtual void drawWater(void* waterData) = 0;
    virtual void setCamera(const Coord3D& pos, const Coord3D& target) = 0;

    // برای A30: باید رزولوشن پایین بیاید
    virtual void setResolution(int w, int h) = 0;
    virtual void setShadowQuality(int quality) = 0; // 0=off برای Mali-G71

    // Factory
    static IRenderer* createRenderer();
};

// ============================================================================
// IAudio - انتزاع صدا
// روی PC: MilesAudioDevice (Miles 6)
// روی Android: OpenALDevice با Oboe (سازگار با A30)
// ============================================================================
class IAudio : public MemoryPoolObject
{
public:
    virtual ~IAudio() {}
    virtual bool init() = 0;
    virtual void shutdown() = 0;
    virtual void playSound(const char* eventName, const Coord3D& pos) = 0;
    virtual void setVolume(float masterVolume) = 0;
    static IAudio* createAudio();
};

// ============================================================================
// IInput - انتزاع ورودی
// روی PC: Win32Mouse + Keyboard (WM_LBUTTONDOWN)
// روی Android: AndroidTouch (MotionEvent)
//  Tap -> LeftClick, LongPress -> RightClick, Pinch -> Wheel
// ============================================================================
struct TouchEvent
{
    enum Type { DOWN, UP, MOVE, CANCEL };
    Type type;
    int fingerId;
    float x, y;           // 0..1
    float pressure;
    unsigned int timeMs;
};

struct PinchEvent
{
    float scale;          // 1.0 = بدون زوم
    float centerX, centerY;
};

class IInput : public MemoryPoolObject
{
public:
    virtual ~IInput() {}
    virtual void onTouchEvent(const TouchEvent& ev) = 0;
    virtual void onPinchEvent(const PinchEvent& ev) = 0;
    virtual void update() = 0; // هر فریم GameEngine::update() این را صدا می‌زند
    static IInput* createInput();
};

// ============================================================================
// IFileSystem - انتزاع فایل
// روی PC: LocalFileSystem با "C:\\Generals\\Data\\" و '\\'
// روی Android: AssetFileSystem با "/assets/Data/" و '/' و حساس به حروف + OBB
// ============================================================================
class IFileSystem : public MemoryPoolObject
{
public:
    virtual ~IFileSystem() {}
    virtual void* openFile(const char* path, const char* mode) = 0;
    virtual void closeFile(void* handle) = 0;
    virtual int readFile(void* handle, void* buffer, int bytes) = 0;
    virtual bool fileExists(const char* path) = 0;
    // روی اندروید این از داخل APK/OBB می‌خواند، نه از SD Card مستقیم
    static IFileSystem* createFileSystem();
};
