# کارهای باقی‌مانده برای حفظ 100% ساختار PC روی اندروید

این لیست دقیق فایل‌هایی است که باید برای A30 تغییر کنند. بقیه 90% بدون تغییر می‌مانند.

## ✅ بدون تغییر (حفظ ساختار PC)

```
Generals/Code/GameEngine/Source/GameLogic/...          # هوش مصنوعی، سلاح، اقتصاد
Generals/Code/GameEngine/Source/GameClient/...         # منطق UI (نه رندر)
Generals/Code/GameEngine/Source/Common/INI/...         # پارسر INI - فقط strtok -> strtok_r
Generals/Code/GameEngine/Source/Common/System/AsciiString.cpp # فقط CriticalSection درست شود
Generals/Code/GameEngine/Include/...                   # همه هدرها
```

## 🔧 باید PAL شود (حدود 30 فایل)

| فایل PC | مشکل روی اندروید | جایگزین | اولویت |
|---|---|---|---|
| `Code/Main/WinMain.cpp` (1200 خط) | `windows.h / HWND / PeekMessage / WM_*` | `Android/app/src/main/cpp/main.cpp` (SDL_main) | P0 |
| `Code/GameEngineDevice/Source/Win32Device/*` | `Win32Mouse.cpp / Win32GameEngine.cpp` | `AndroidDevice/AndroidTouch.cpp` | P0 |
| `Code/GameEngineDevice/Source/W3DDevice/*` | `dx8wrapper.cpp / Direct3DCreate8` | `AndroidDevice/W3D_GLES.cpp` (GLES 3.0) | P0 |
| `Code/GameEngineDevice/Source/MilesAudioDevice/*` | `Miles 6 SDK` نایاب | `AndroidDevice/OpenALAudio.cpp` | P1 |
| `Code/GameEngineDevice/Source/VideoDevice/Bink/*` | `Bink SDK` پولی | `AndroidDevice/FFmpegVideo.cpp` یا حذف | P2 |
| `Code/Libraries/Source/WWVegas/WW3D2/*` | شیدرهای `vs_1_1` قدیمی | تبدیل به `GLSL 300 es` برای Mali-G71 | P0 |
| `Code/GameEngine/Source/Common/System/Debug.cpp` | `vsprintf / char theBuffer[8192]` غیر امن | `vsnprintf + thread_local` | P1 |
| `Code/GameEngine/Source/Common/System/File.cpp` | `fopen("C:\\...")` | `AAssetManager_open()` برای OBB | P0 |
| `Code/GameEngine/Source/Common/System/Registry.cpp` | `winreg.h` | `SharedPreferences` | P2 |
| `Code/Libraries/Source/WWVegas/WWLib/wwstring.cpp` | `lstrcat` | `strncat` | P1 |

## 📱 تنظیمات مخصوص A30

- `Generals/Code/GameEngine/Source/Common/GlobalData.cpp` : `m_resolution` پیش‌فرض را `1280x720` کن (نه 800x600 PC)
- `Generals/Code/GameEngine/Source/GameClient/Water/W3DWater.cpp:1628` : کیفیت آب را `LOW` برای Mali-G71
- `Generals/Code/GameEngine/Source/Common/System/MemoryInit.cpp:726` : `initialAllocationCount` را نصف کن (3GB RAM)
- `Generals/Code/GameEngine/Include/Common/GameMemory.h` : `LARGE_BUFFER 8192` به `16384` برای لاگ‌های طولانی اندروید

## تخمین زمان (حفظ ساختار PC)

- یک نفر فول‌تایم حرفه‌ای: 6-9 ماه تا نسخه قابل بازی (30 FPS روی A30)
- تیم 3 نفره: 3-4 ماه
- اگر فقط Winlator بخواهی: 0 ماه - همین امروز

## قدم بعدی پیشنهادی

1. `PAL/IRenderer.h` که ساختیم را کامل کن
2. `W3D_GLES.cpp` را با `bgfx` بساز (bgfx خودش DX8->GLES ترجمه می‌کند)
3. یک `Hello Triangle` با `SDL2 + GLES` روی A30 اجرا کن تا toolchain درست شود
4. بعد `GameLogic` را لینک کن
