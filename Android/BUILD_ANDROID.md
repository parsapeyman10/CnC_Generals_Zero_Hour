# راهنمای ساخت APK برای Galaxy A30 - قدم به قدم

> این راهنما فرض می‌کند می‌خواهی **همان ساختار PC** را به APK تبدیل کنی، نه اینکه بازی را از صفر بنویسی.

## پیش‌نیازها

- Windows 10/11 یا Linux (برای بیلد)
- Android Studio Hedgehog یا جدیدتر
- NDK r26b (از SDK Manager نصب کن)
- CMake 3.22+
- یک A30 با `Developer Options > USB Debugging` روشن + کابل
- فایل‌های اصلی بازی: پوشه `Generals/Run/Data` و `GeneralsMD/Run/Data` از نسخه Steam (فایل‌های `.big` حدود 1.8GB)

---

## مرحله 1: آماده سازی پروژه

```bash
git clone https://github.com/parsapeyman10/CnC_Generals_Zero_Hour.git
cd CnC_Generals_Zero_Hour
git checkout arena/019fe382-cnc-generals-zero-hour
```

در Android Studio: `File > Open > انتخاب پوشه Android/`

ساختار مورد انتظار:
```
Android/
  app/
    src/main/
      AndroidManifest.xml
      java/com/ea/generals/GeneralsActivity.java
      cpp/CMakeLists.txt          <- اینجا GameEngine را صدا می‌زند
  PAL/
    IRenderer.h
    IAudio.h
  AndroidDevice/
    AndroidGameEngine.cpp
    AndroidTouch.cpp
```

## مرحله 2: تنظیم NDK

`Android/local.properties` را بساز:
```
sdk.dir=C\:\\Users\\YOU\\AppData\\Local\\Android\\Sdk
ndk.dir=C\:\\Users\\YOU\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
```

`app/build.gradle` از قبل برای A30 تنظیم شده:
```gradle
android {
  ndkVersion "26.1.10909125"
  defaultConfig {
    minSdk 21  // A30 اندروید 9
    targetSdk 34
    ndk { abiFilters 'arm64-v8a', 'armeabi-v7a' } // Exynos 7904 هر دو را می‌فهمد ولی arm64 سریع‌تره
  }
}
```

## مرحله 3: اضافه کردن Assets (مهم!)

APK نمی‌تواند 2GB باشد (لیمیت Google Play 100MB). باید OBB بسازی:

```bash
# روی PC
mkdir -p Android/app/src/main/assets/Data
cp -r Generals/Run/Data/*.big Android/app/src/main/assets/Data/
cp -r GeneralsMD/Run/Data/*.big Android/app/src/main/assets/Data/

# یا برای تست سریع فقط یک مپ:
mkdir -p Android/app/src/main/assets/Data/Maps
cp Generals/Run/Data/Maps/MapCache.ini Android/app/src/main/assets/Data/Maps/
```

برای نسخه نهایی:
```bash
# OBB سازی (روی A30 به /sdcard/Android/obb/com.ea.generals/ می‌رود)
jobb -d Android/app/src/main/assets -o main.1.com.ea.generals.obb -k 123456 -pn com.ea.generals -pv 1
```

## مرحله 4: بیلد

در Android Studio:
```
Build > Make Project  (Ctrl+F9)
# یا از ترمینال:
./gradlew assembleDebug
```

خروجی:
```
Android/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk  (~45 MB)
Android/app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk
```

اگر ارور `windows.h not found` گرفتی یعنی هنوز یک فایل `PAL` نشده - لیست فایل‌ها در `docs/REMAINING_WORK.md` هست.

## مرحله 5: نصب روی A30

```bash
adb devices  # باید A30 را ببینی
adb install -r Android/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

# اگر OBB ساختی:
adb shell mkdir -p /sdcard/Android/obb/com.ea.generals
adb push main.1.com.ea.generals.obb /sdcard/Android/obb/com.ea.generals/

# اجرا و لاگ:
adb shell am start -n com.ea.generals/com.ea.generals.GeneralsActivity
adb logcat -s Generals:D DEBUG:D
```

## عیب‌یابی A30

| مشکل | دلیل | راه حل |
|---|---|---|
| صفحه سیاه بعد از Splash | `Mali-G71` شیدر `W3D` را نمی‌فهمد | در `AndroidDevice/W3D_GLES.cpp` شیدر را به `#version 300 es` تبدیل کن |
| کرش بعد از لودینگ | `RAM` کم (3GB) | در `AndroidManifest.xml` `android:largeHeap="true"` + در `MemoryInit.cpp` `overflowAllocationCount` را نصف کن |
| تاچ کار نمی‌کند | `TheWin32Mouse` هنوز فعاله | در `AndroidGameEngine.cpp` `createMouse()` را به `AndroidMouse` تغییر بده |
| متن فارسی/عربی برعکس | `UnicodeString` قدیمی | فعلا `LanguageFilter.cpp` را غیرفعال کن |
| لگ شدید (10 FPS) | رزولوشن 1080p | در `AndroidGameEngine.cpp` `setResolution(1280,720)` + `W3DWater.cpp` کیفیت آب را `LOW` کن |

## تست سریع بدون بیلد (Winlator)

اگر فقط می‌خواهی ببینی بازی روی A30 با همین گرافیک PC چطور به نظر می‌رسد قبل از پورت:
1. از `play.google.com` `Winlator 7.1` نصب کن
2. `Generals/Run` را به `Download/Generals` در گوشی کپی کن
3. در Winlator یک Container بساز (Resolution 1280x720, DX Wrapper DXVK)
4. `generals.exe` را اجرا کن

این دقیقا همان حس PC را می‌دهد و بهت نشان می‌دهد UI روی 6.4 اینچ چقدر ریز است.
