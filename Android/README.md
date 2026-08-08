# پورت Generals Zero Hour برای اندروید (حفظ ساختار PC) - مخصوص Galaxy A30

> هدف: **همان بازی کامپیوتر، بدون تغییر گیم‌پلی، داستان، هوش مصنوعی، INIها** فقط به شکل `APK` روی اندروید نصب شود.

این پوشه نقشه راه و اسکلت اولیه پورت را نگه می‌دارد. ساختار اصلی `Generals/` و `GeneralsMD/` **اصلا دست نمی‌خورد** - فقط یک لایه نازک `PAL` (Platform Abstraction Layer) اضافه می‌شود.

---

## 1. ایده اصلی: ساختار PC حفظ می‌شود

```
┌─────────────────────────────────────────────────┐
│  Generals / GeneralsMD                          │  <-- دست نمی‌خورد (800 هزار خط)
│  GameLogic / GameClient / Common / INI / AI     │      منطق بازی، سلاح‌ها، هوش مصنوعی
└──────────────────────┬──────────────────────────┘
                       │  فقط از طریق Interface صدا می‌زند
┌──────────────────────▼──────────────────────────┐
│  PAL - Platform Abstraction Layer (جدید)         │  <-- فقط اینجا را می‌نویسیم
│  IRenderer / IAudio / IInput / IFileSystem      │
└──────┬──────────────────┬───────────────┬────────┘
       │                  │               │
  W3D_DX8 (PC)      W3D_GLES (Android)  SDL2 Window
  Miles/Bink (PC)   OpenAL/Oboe (Android)
  Win32 Mouse       Touch (Android)
```

**نکته کلیدی:** `GameEngine.h` از قبل Factory دارد:
```cpp
virtual GameLogic *createGameLogic() = 0;
virtual GameClient *createGameClient() = 0;
virtual AudioManager *createAudioManager() = 0;
```
ما فقط برای اندروید یک `AndroidGameEngine : public GameEngine` می‌سازیم. منطق بازی نمی‌فهمد روی کجاست.

### چه چیزی حفظ می‌شود؟ 100%:
- همه `*.INI` - تعادل سلاح‌ها، قیمت‌ها
- همه `*.BIG` - مدل‌ها، تکسچرها، صداها، نقشه‌ها
- `GameLogic` - مسیر یابی، AI اسکیرمیش
- `GameClient/InGameUI` - منطق ساخت و ساز

### چه چیزی جایگزین می‌شود؟ فقط Device:
- `GameEngineDevice/W3DDevice` (Direct3D 8) -> `W3D_GLES` (OpenGL ES 3.0 برای Mali-G71 داخل A30)
- `GameEngineDevice/Win32Device` (`WinMain.cpp` + `WM_LBUTTONDOWN`) -> `AndroidDevice` (`SDLActivity.java` + Touch)
- `MilesAudioDevice` -> `OpenALDevice`
- `Win32Mouse / Win32GameEngine` -> `AndroidMouse / AndroidGameEngine`

برای همین می‌گوییم **ساختار PC حفظ می‌شود**.

---

## 2. مشخصات Galaxy A30 و تنظیمات پیشنهادی

- **CPU:** Exynos 7904 (2x Cortex-A73 1.8GHz + 6x A53 1.6GHz) - کافی است
- **GPU:** Mali-G71 MP2 - ضعیف‌ترین حلقه. باید رزولوشن را از `1080x2340` به `1280x720` بیاری و سایه‌های `W3DVolumetricShadow.cpp` را خاموش کنی
- **RAM:** 3GB / 4GB - بازی اصلی 512MB می‌خواست ولی `MemoryPool` فعلی 1GB رزرو می‌کند. باید `MemoryInit.cpp` را برای موبایل بهینه کنی
- **اندروید:** 9/10/11 - `armeabi-v7a + arm64-v8a` هر دو بساز
- **حافظه:** بازی با همه BIGها ~2GB. باید روی `APK (80MB) + OBB (1.8GB)` تقسیم شود

---

## 3. کنترل‌های لمسی - چطور حس PC حفظ شود؟

ایده: موس را شبیه‌سازی کن، نه اینکه UI را عوض کنی.

| حرکت لمسی | معادل PC | کجا پیاده می‌شود |
|---|---|---|
| `Tap` کوتاه | `Left Click` انتخاب/دستور | `AndroidTouch.cpp: onTouchDown()` |
| `Tap + Hold 400ms` | `Right Click` حرکت/حمله | `AndroidTouch.cpp` |
| `Drag یک انگشت` | `Mouse Move + Drag` جابجایی نقشه | `W3DView.cpp` |
| `Pinch` دو انگشت | `Mouse Wheel` زوم | `W3DView::setZoom()` |
| `Two Finger Drag` | `Middle Mouse Drag` چرخش دوربین | `W3DView::rotateCamera()` |
| `Double Tap` | `Double Click` انتخاب همه هم‌نوع | `InGameUI.cpp` |
| دکمه‌های شناور | `ControlBar` قدیمی ولی 1.5x بزرگ‌تر | `ControlBarScheme.h` با `DPI Scale` |

یک نوار کوچک هم برای `Ctrl + 1..9` (گروه‌بندی) و `Space` (پرش به آلارم) اضافه می‌شود.

---

## 4. چطور APK بسازیم؟ (خلاصه)

```bash
# پیش‌نیاز: Android Studio Hedgehog + NDK r26 + CMake 3.22
# 1. این ریپو را باز کن
# 2. Android/ را به عنوان پروژه باز کن
# 3. Gradle Sync -> Build -> Generate Signed APK
# 4. روی A30 نصب:
adb install app-arm64-v8a-debug.apk
adb push main.1.com.ea.generals.obb /sdcard/Android/obb/com.ea.generals/
```

جزئیات کامل در `BUILD_ANDROID.md`

---

## 5. وضعیت فعلی این پوشه

- `PAL/` - اینترفیس‌های جدید (IRenderer, IAudio...)
- `AndroidDevice/` - جایگزین Win32Device برای اندروید
- `CMakeLists.txt` - بیلد NDK که 90% کد موجود را بدون تغییر کامپایل می‌کند
- `docs/` - نقشه دقیق هر فایل که باید عوض شود

این اسکلت کامپایل می‌شود ولی هنوز رندر نمی‌کند - قدم بعدی پیاده‌سازی `W3D_GLES::draw()` است.
