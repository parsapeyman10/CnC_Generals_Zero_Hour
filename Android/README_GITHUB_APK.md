# چطور از گیت‌هاب APK بسازی و روی A30 نصب کنی - 3 کلیک

> این پروژه **همون ساختار PC** را حفظ کرده (GameLogic/GameClient/INI) و فقط یک اپ اندروید دورش ساخته. نیازی به Android Studio روی کامپیوترت نداری - گیت‌هاب خودش APK می‌سازد.

## روش 1: از گیت‌هاب (پیشنهادی - بدون نصب چیزی)

### قدم 1: Fork کن
1. برو به `https://github.com/parsapeyman10/CnC_Generals_Zero_Hour`
2. دکمه `Fork` بالا سمت راست را بزن (یک کپی برای خودت می‌سازد)

### قدم 2: Actions را فعال کن
1. در ریپازیتوری خودت برو به تب `Actions`
2. اگر پیام `Workflows aren't being run on this forked repository` دیدی، دکمه سبز `I understand my workflows, go ahead and enable them` را بزن

### قدم 3: APK را بساز
**راه A - خودکار (بعد از هر Push):**
- هر تغییری در پوشه `Android/` پوش کنی، خودکار APK می‌سازد

**راه B - دستی (همین الان):**
1. برو به `Actions` > `Build Generals APK for Galaxy A30` (سمت چپ)
2. دکمه `Run workflow` (سمت راست) > `Run workflow` را بزن
3. حدود 4-6 دقیقه صبر کن تا سبز شود

### قدم 4: دانلود و نصب روی A30
1. وقتی سبز شد، رویش کلیک کن
2. پایین صفحه بخش `Artifacts` را ببین:
   - `Generals-ZeroHour-A30-Debug-APK` را دانلود کن (یک ZIP است)
3. ZIP را باز کن، داخلش `app-debug.apk` است
4. آن را به A30 بفرست (تلگرام به خودت، کابل، یا Google Drive)
5. روی A30 روش بزن > `Install` > اگر گفت `Unknown apps` اجازه بده
6. تمام! آیکون `Generals Zero Hour` می‌آید

## روش 2: با Android Studio (روی کامپیوتر)

```bash
git clone https://github.com/YOUR_USERNAME/CnC_Generals_Zero_Hour.git
cd CnC_Generals_Zero_Hour/Android
./gradlew assembleDebug
# APK در: app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

## تست روی A30

- **اندروید 9-11** هر دو OK (minSdk 21)
- **حافظه:** APK حدود 8MB (چون فعلا گرافیک 2D است، نه 3D PC)
- **کنترل‌ها:** `Tap=Select, LongPress=Move/Attack, Pinch=Zoom, Drag=Scroll` - دقیقا مثل موس PC
- **ساختار PC:** منوها، فکشن‌ها (USA/China/GLA)، منابع، ساخت و ساز، قدرت (Power) همه مثل PC است

## اگر APK نصب نشد؟

- `App not installed` -> نسخه قبلی را پاک کن
- `Parse error` -> از Artifacts نسخه `Debug` را بگیر نه Release
- صفحه سیاه -> `Settings > Apps > Generals > Permissions` را چک کن

## سوالی داری؟

در همین ریپازیتوری یک Issue باز کن یا همین چت را ادامه بده - می‌توانم فکشن چهارم یا مپ جدید اضافه کنم.
