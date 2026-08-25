#include "devices/drivers/DebugOledDriver.h"

#include <Arduino.h>
#include <Wire.h>

#include <cstdio>

DebugOledDriver::DebugOledDriver()
    : display(
        OLED_WIDTH,
        OLED_HEIGHT,
        &Wire,
        OLED_RESET_PIN
    ) {
}


// ======================================================
// Initialization
// ======================================================

bool DebugOledDriver::initialize() {

    if (initialized) {
        return true;
    }

    Wire.begin(
        OLED_SDA_PIN,
        OLED_SCL_PIN
    );

    /*
     * periphBegin = false because Wire.begin() has
     * already been called above with our chosen pins.
     */
    if (!display.begin(
        SSD1306_SWITCHCAPVCC,
        OLED_ADDRESS,
        true,
        false
    )) {

        Serial.println(
            "DebugOledDriver: SSD1306 initialization failed"
        );

        return false;
    }

    display.clearDisplay();

    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setTextWrap(false);

    display.setCursor(0, 0);
    display.println("Debug OLED ready");

    display.display();

    initialized = true;

    return true;
}


// ======================================================
// DeviceDriver
// ======================================================

bool DebugOledDriver::execute(
    const alarm_system_v1_Action& action
) {

    if (!initialize()) {
        return false;
    }

    /*
     * Keep a copy of the action so that update() can
     * continue displaying / scrolling it after execute()
     * has returned.
     */
    if (
    action.parameters_count > 0 &&
    action.parameters == nullptr
) {
    return false;
}

    currentParameters.clear();

    if (action.parameters_count > 0) {
        currentParameters.assign(
            action.parameters,
            action.parameters + action.parameters_count
        );
    }

    currentAction = action;

    currentAction.parameters_count =
        static_cast<pb_size_t>(currentParameters.size());

    currentAction.parameters =
        currentParameters.empty()
            ? nullptr
            : currentParameters.data();

    hasAction = true;

    firstVisibleParameter = 0;

    lastScrollTime = millis();

    render();

    return true;
}

/* Debug Device returns no capabilities
** Is to be called manually for debugging purposes
*/
DeviceCapabilityView DebugOledDriver::getCapabilities() const {
    return {
        nullptr,
        0
    };
}

// ======================================================
// Periodic update
// ======================================================

void DebugOledDriver::update() {

    if (!hasAction) {
        return;
    }

    /*
     * No scrolling needed if everything fits.
     */
    if (
        currentAction.parameters_count
        <= VISIBLE_PARAMETER_COUNT
    ) {
        return;
    }

    /*
     * Non-blocking timer.
     */
    if (
        millis() - lastScrollTime
        < SCROLL_INTERVAL_MS
    ) {
        return;
    }

    lastScrollTime = millis();


    /*
     * Example with 7 parameters and 5 visible rows:
     *
     * 0 -> 0 1 2 3 4
     * 1 -> 1 2 3 4 5
     * 2 -> 2 3 4 5 6
     * then back to 0.
     */

    const pb_size_t maxStart =
        currentAction.parameters_count
        - VISIBLE_PARAMETER_COUNT;

    firstVisibleParameter++;

    if (firstVisibleParameter > maxStart) {
        firstVisibleParameter = 0;
    }

    render();
}


// ======================================================
// Render current screen
// ======================================================

void DebugOledDriver::render() {

    if (!hasAction) {
        return;
    }

    display.clearDisplay();

    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setTextWrap(false);

    display.setCursor(0, 0);


    // --------------------------------------------------
    // Action label
    // --------------------------------------------------

    printLimited(
        currentAction.label,
        21
    );

    display.println();


    // --------------------------------------------------
    // Capability / action key
    // --------------------------------------------------

    display.print("> ");

    printLimited(
        currentAction.device_action_key.key,
        19
    );

    display.println();


    // --------------------------------------------------
    // Separator
    // --------------------------------------------------

    display.println("---------------------");


    // --------------------------------------------------
    // Parameters
    //
    // Top 3 lines stay fixed.
    // Bottom 5 lines form the scrolling region.
    // --------------------------------------------------

    for (
        pb_size_t row = 0;
        row < VISIBLE_PARAMETER_COUNT;
        row++
    ) {

        const pb_size_t parameterIndex =
            firstVisibleParameter + row;

        if (
            parameterIndex
            >= currentAction.parameters_count
        ) {
            break;
        }

        printParameter(
            currentAction.parameters[parameterIndex]
        );
    }


    // Push framebuffer to the actual OLED.
    display.display();
}


// ======================================================
// Parameter rendering
// ======================================================

void DebugOledDriver::printParameter(
    const alarm_system_v1_ActionParameter& parameter
) {

    /*
     * Roughly:
     *
     * Volume: 80 %
     * Enabled: true
     * Delay: 300 ms
     *
     * Around 21 characters fit on one line at
     * text size 1.
     */

    const char* parameterName;

    if (parameter.label[0] != '\0') {
        parameterName = parameter.label;
    } else {
        parameterName = parameter.parameter_key;
    }


    printLimited(
        parameterName,
        8
    );

    display.print(": ");


    char valueBuffer[32];

    formatValue(
        parameter,
        valueBuffer,
        sizeof(valueBuffer)
    );

    printLimited(
        valueBuffer,
        11
    );

    display.println();
}


// ======================================================
// ActionValue -> text
// ======================================================

void DebugOledDriver::formatValue(
    const alarm_system_v1_ActionParameter& parameter,
    char* buffer,
    size_t bufferSize
) {

    const alarm_system_v1_ActionValue& actionValue =
        parameter.value;


    const char* units =
        parameter.has_units
            ? parameter.units
            : "";


    switch (actionValue.which_value) {


        // --------------------------------------------------
        // String
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_string_val_tag:

            snprintf(
                buffer,
                bufferSize,
                "%s",
                actionValue.value.string_val
            );

            break;


        // --------------------------------------------------
        // uint32
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_uint32_val_tag:

            snprintf(
                buffer,
                bufferSize,
                "%lu%s%s",
                static_cast<unsigned long>(
                    actionValue.value.uint32_val
                ),
                units[0] != '\0' ? " " : "",
                units
            );

            break;


        // --------------------------------------------------
        // int32
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_int32_val_tag:

            snprintf(
                buffer,
                bufferSize,
                "%ld%s%s",
                static_cast<long>(
                    actionValue.value.int32_val
                ),
                units[0] != '\0' ? " " : "",
                units
            );

            break;


        // --------------------------------------------------
        // bool
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_bool_val_tag:

            snprintf(
                buffer,
                bufferSize,
                "%s",
                actionValue.value.bool_val
                    ? "true"
                    : "false"
            );

            break;


        // --------------------------------------------------
        // RGBA
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_rgba_val_tag:

            snprintf(
                buffer,
                bufferSize,
                "#%08lX",
                static_cast<unsigned long>(
                    actionValue
                        .value
                        .rgba_val
                        .rgba
                )
            );

            break;


        // --------------------------------------------------
        // Percentage
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_percentage_tag:

            snprintf(
                buffer,
                bufferSize,
                "%lu%%",
                static_cast<unsigned long>(
                    actionValue
                        .value
                        .percentage
                        .value
                )
            );

            break;


        // --------------------------------------------------
        // File
        //
        // Only display the filename.
        // Definitely don't try displaying file_content :)
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_file_tag:

            snprintf(
                buffer,
                bufferSize,
                "%s",
                actionValue
                    .value
                    .file
                    .filename
            );

            break;


        // --------------------------------------------------
        // Double
        // --------------------------------------------------

        case alarm_system_v1_ActionValue_double_val_tag:

            snprintf(
                buffer,
                bufferSize,
                "%.2f%s%s",
                actionValue.value.double_val,
                units[0] != '\0' ? " " : "",
                units
            );

            break;


        // --------------------------------------------------
        // Nothing set / unknown
        // --------------------------------------------------

        default:

            snprintf(
                buffer,
                bufferSize,
                "<unset>"
            );

            break;
    }
}


// ======================================================
// OLED utility
// ======================================================

void DebugOledDriver::printLimited(
    const char* text,
    size_t maxCharacters
) {

    if (text == nullptr) {
        return;
    }

    for (
        size_t i = 0;
        i < maxCharacters && text[i] != '\0';
        i++
    ) {

        display.print(
            text[i]
        );
    }
}