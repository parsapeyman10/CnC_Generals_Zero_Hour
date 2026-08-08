/*
 * AndroidTouch.cpp - تبدیل لمس به موس قدیمی Generals
 * 
 * هدف: GameClient فکر کند موس وصل است، در حالی که کاربر با انگشت کار می‌کند.
 * هیچ تغییری در InGameUI.cpp یا Mouse.cpp لازم نیست.
 */

#include "AndroidTouch.h"
#include "GameClient/Mouse.h"
#include "GameClient/InGameUI.h"
#include "GameClient/GameClient.h"

// برای A30: تاخیر نگه داشتن برای کلیک راست
#define LONG_PRESS_MS 400
#define DRAG_THRESHOLD_PX 15
#define PINCH_THRESHOLD 0.05f

struct FingerState
{
    bool down;
    float startX, startY;
    float lastX, lastY;
    unsigned int downTime;
    bool longPressFired;
};

static FingerState s_fingers[10];
static bool s_isPinching = false;
static float s_lastPinchDist = 0;

// ---------------------------------------------------------------------------
// این تابع از Java صدا زده می‌شود: GeneralsActivity.java -> onTouchEvent()
// ---------------------------------------------------------------------------
void AndroidTouch::onTouchEvent(const TouchEvent& ev)
{
    if (TheMouse == nullptr || TheInGameUI == nullptr)
        return;

    FingerState& f = s_fingers[ev.fingerId];

    switch (ev.type)
    {
    case TouchEvent::DOWN:
        f.down = true;
        f.startX = ev.x;
        f.startY = ev.y;
        f.lastX = ev.x;
        f.lastY = ev.y;
        f.downTime = ev.timeMs;
        f.longPressFired = false;

        // یک انگشت = Left Down
        if (countFingersDown() == 1)
        {
            // شبیه‌سازی موس قدیمی: TheWin32Mouse->setPosition(x,y)
            TheMouse->setPosition(ev.x * TheDisplay->getWidth(), ev.y * TheDisplay->getHeight());
            // InGameUI فکر می‌کند WM_LBUTTONDOWN آمده
            TheInGameUI->setMouseDown(true);
        }
        break;

    case TouchEvent::MOVE:
        f.lastX = ev.x;
        f.lastY = ev.y;

        if (countFingersDown() == 1 && !f.longPressFired)
        {
            // Drag نقشه: اگر انگشت را کشید، دوربین را حرکت بده
            float dx = ev.x - f.startX;
            float dy = ev.y - f.startY;
            float dist = sqrtf(dx*dx + dy*dy) * TheDisplay->getWidth();
            
            if (dist > DRAG_THRESHOLD_PX)
            {
                // تبدیل به Mouse Move
                TheMouse->setPosition(ev.x * TheDisplay->getWidth(), ev.y * TheDisplay->getHeight());
                // به GameClient بگو دوربین را جابجا کن (مثل نگه داشتن Middle Mouse)
                TheGameClient->scrollBy(-dx * 20, -dy * 20);
            }
        }
        else if (countFingersDown() == 2)
        {
            // Pinch قبلا در onPinchEvent هندل شده
        }
        break;

    case TouchEvent::UP:
        f.down = false;

        if (f.longPressFired)
        {
            // قبلا RightClick فرستادیم، الان Up بفرست
            TheInGameUI->setMouseDown(false);
        }
        else
        {
            float dx = ev.x - f.startX;
            float dy = ev.y - f.startY;
            float dist = sqrtf(dx*dx + dy*dy) * TheDisplay->getWidth();
            unsigned int holdTime = ev.timeMs - f.downTime;

            if (dist < DRAG_THRESHOLD_PX && holdTime < LONG_PRESS_MS)
            {
                // Tap کوتاه = Left Click
                TheMouse->setPosition(ev.x * TheDisplay->getWidth(), ev.y * TheDisplay->getHeight());
                TheInGameUI->clickAt(ev.x, ev.y);
            }
            // اگر Drag بود که در MOVE هندل شد
        }

        if (countFingersDown() == 0)
            s_isPinching = false;
        break;

    case TouchEvent::CANCEL:
        f.down = false;
        s_isPinching = false;
        break;
    }
}

void AndroidTouch::onPinchEvent(const PinchEvent& ev)
{
    // Pinch = زوم (مثل چرخ موس)
    // Generals: W3DView::setZoom() یا Mouse Wheel
    if (fabsf(ev.scale - 1.0f) < PINCH_THRESHOLD)
        return;

    float zoomDelta = (ev.scale - 1.0f) * 5.0f; // حساسیت
    TheGameClient->zoomBy(zoomDelta, ev.centerX, ev.centerY);
}

// هر فریم چک کن آیا LongPress شده؟
void AndroidTouch::update(unsigned int curTimeMs)
{
    for (int i = 0; i < 10; ++i)
    {
        FingerState& f = s_fingers[i];
        if (f.down && !f.longPressFired)
        {
            unsigned int hold = curTimeMs - f.downTime;
            float dx = f.lastX - f.startX;
            float dy = f.lastY - f.startY;
            float dist = sqrtf(dx*dx + dy*dy) * TheDisplay->getWidth();

            if (hold >= LONG_PRESS_MS && dist < DRAG_THRESHOLD_PX)
            {
                f.longPressFired = true;
                // Long Press = Right Click (دستور حرکت/حمله)
                TheMouse->setPosition(f.lastX * TheDisplay->getWidth(), f.lastY * TheDisplay->getHeight());
                TheInGameUI->rightClickAt(f.lastX, f.lastY);
                // ویبره کوتاه برای بازخورد (روی A30 خوب حس می‌شود)
                // AndroidVibrate(30);
            }
        }
    }
}

int AndroidTouch::countFingersDown()
{
    int c = 0;
    for (int i = 0; i < 10; ++i) if (s_fingers[i].down) ++c;
    return c;
}
