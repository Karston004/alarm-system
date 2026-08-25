#pragma once

#include <Adafruit_SSD1306.h>

#include "devices/DeviceDriver.h"

class DebugOledDriver final : public DeviceDriver {

public:
    DebugOledDriver();

    bool execute(
        const alarm_system_v1_Action& action
    ) override;

    void update();

private:
    static constexpr uint8_t OLED_ADDRESS = 0x3C;

    static constexpr int OLED_WIDTH = 128;
    static constexpr int OLED_HEIGHT = 64;

    static constexpr int OLED_SDA_PIN = 22;
    static constexpr int OLED_SCL_PIN = 21;

    static constexpr int OLED_RESET_PIN = -1;

    Adafruit_SSD1306 display;

    bool initialized = false;


    // ==================================================
    // Current displayed action
    // ==================================================

    alarm_system_v1_Action currentAction =
        alarm_system_v1_Action_init_zero;

    bool hasAction = false;

    pb_size_t firstVisibleParameter = 0;

    unsigned long lastScrollTime = 0;

    static constexpr unsigned long SCROLL_INTERVAL_MS =
        1000;

    static constexpr pb_size_t VISIBLE_PARAMETER_COUNT =
        5;


    // ==================================================
    // Internal methods
    // ==================================================

    bool initialize();

    void render();

    void printParameter(
        const alarm_system_v1_ActionParameter& parameter
    );

    void formatValue(
        const alarm_system_v1_ActionParameter& parameter,
        char* buffer,
        size_t bufferSize
    );

    void printLimited(
        const char* text,
        size_t maxCharacters
    );
};