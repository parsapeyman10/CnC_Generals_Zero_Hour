# CMakeLists.txt - بیلد اندروید برای Generals (حفظ ساختار PC)
# این فایل فقط لایه PAL و AndroidDevice را می‌سازد و بقیه کد PC را بدون تغییر لینک می‌کند
# استفاده: در Android Studio به صورت خودکار صدا زده می‌شود
# دستی: cmake -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a ..

cmake_minimum_required(VERSION 3.22)
project(GeneralsAndroid)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# برای A30: هر دو ABI ولی arm64 اولویت
if(NOT ANDROID_ABI)
    set(ANDROID_ABI "arm64-v8a")
endif()

message(STATUS "Building for Android ABI: ${ANDROID_ABI}")

# ---------------------------------------------------------------------------
# 1. سورس‌های اصلی PC - بدون تغییر
#    این لیست دقیقا همان فایل‌هایی است که در GameEngine.dsp هستند
#    ما آنها را اینجا اضافه می‌کنیم تا NDK با clang کامپایل کند
# ---------------------------------------------------------------------------
file(GLOB_RECURSE GAMEENGINE_SOURCES
    ../Generals/Code/GameEngine/Source/Common/*.cpp
    ../Generals/Code/GameEngine/Source/GameLogic/*.cpp
    ../Generals/Code/GameEngine/Source/GameClient/*.cpp
)

# فایل‌های Win32 که باید حذف شوند (جایگزین اندرویدی دارند)
list(FILTER GAMEENGINE_SOURCES EXCLUDE REGEX ".*Win32.*")
list(FILTER GAMEENGINE_SOURCES EXCLUDE REGEX ".*StackDump\\.cpp") # imagehlp.h ندارد
list(FILTER GAMEENGINE_SOURCES EXCLUDE REGEX ".*CDManager\\.cpp") # SafeDisk ندارد

# ---------------------------------------------------------------------------
# 2. لایه PAL و AndroidDevice - جدید
# ---------------------------------------------------------------------------
set(ANDROID_SOURCES
    AndroidDevice/AndroidGameEngine.cpp
    AndroidDevice/AndroidTouch.cpp
    AndroidDevice/AndroidFileSystem.cpp
    AndroidDevice/W3D_GLES.cpp
    AndroidDevice/OpenALAudio.cpp
    PAL/IRenderer.cpp
)

# ---------------------------------------------------------------------------
# 3. کتابخانه‌های جایگزین
# ---------------------------------------------------------------------------
# SDL2 برای پنجره و ورودی (به جای windows.h)
# bgfx یا مستقیم GLES برای گرافیک (به جای DirectX 8)
# OpenAL-soft برای صدا (به جای Miles)

find_package(SDL2 REQUIRED) # از طریق gradle prefetched

# ---------------------------------------------------------------------------
# 4. تارگت اصلی - libGenerals.so که در APK لود می‌شود
# ---------------------------------------------------------------------------
add_library(Generals SHARED
    ${GAMEENGINE_SOURCES}
    ${ANDROID_SOURCES}
    app/src/main/cpp/main.cpp  # SDL_main
)

target_include_directories(Generals PRIVATE
    ../Generals/Code/GameEngine/Include
    ../Generals/Code/GameEngineDevice/Include
    ../Generals/Code/Libraries/Include
    ../Generals/Code/Libraries/Source/WWVegas
    PAL
    AndroidDevice
    ${SDL2_INCLUDE_DIRS}
)

target_compile_definitions(Generals PRIVATE
    -DANDROID
    -D_ANDROID
    -DNO_MSS          # Miles Sound System را خاموش کن
    -DNO_BINK         # Bink ویدیو خاموش (فعلا)
    -DNO_SAFEDISK     # SafeDisk خاموش
    -DUSE_GLES=1      # به جای USE_DX8
    -D_LARGE_HEAP=1   # برای A30 3GB
)

# برای Mali-G71: شیدرها را به 300 es تبدیل کن
target_compile_options(Generals PRIVATE
    -frtti
    -fexceptions
    -O2              # برای A30 O2 بهتر از O3 (گرمای کمتر)
    -Wno-invalid-offsetof
    -Wno-writable-strings
)

target_link_libraries(Generals
    SDL2::SDL2
    GLESv3
    EGL
    OpenSLES
    log
    android
)

# ---------------------------------------------------------------------------
# 5. نصب
# ---------------------------------------------------------------------------
install(TARGETS Generals DESTINATION lib/${ANDROID_ABI})
