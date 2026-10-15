#include "devices/drivers/WifiPlugDriver.h"

#include <Arduino.h>
#include <Wire.h>
#include <cstring>
#include <cstdio>
#include <HTTPClient.h>

WifiPlugDriver::WifiPlugDriver(
    const IPAddress &ip,
    const String &pass)
    : ipAddress(ip), password(pass)
{
}

// ======================================================
// DeviceDriver
// ======================================================

bool WifiPlugDriver::execute(
    const alarm_system_v1_Action &action)
{
    const char *key = action.device_action_key.key;

    if (strcmp(key, "set_switch") == 0)
    {
        bool stateV = action.parameters[0].value.value.bool_val;
        String state = stateV ? "On" : "Off";

        HTTPClient http;
        String url = "http://" + ipAddress.toString() + "/cm?user=admin&password=" + password + "&cmnd=Power%20" + state;

        http.begin(url);
        int code = http.GET();
        http.end();

        return code >= 200 && code < 300;
    }
    else if (strcmp(key, "toggle") == 0)
    {
        HTTPClient http;
        String url = "http://" + ipAddress.toString() + "/cm?user=admin&password=" + password + "&cmnd=Power%20TOGGLE";

        http.begin(url);
        int code = http.GET();
        http.end();

        return code >= 200 && code < 300;
    }

    return false;
}

// Abstract to a Capability builder?
DeviceCapabilityView WifiPlugDriver::getCapabilities() const
{
    static alarm_system_v1_ParameterRequirement stateRequirement =
        alarm_system_v1_ParameterRequirement_init_zero;

    static alarm_system_v1_DeviceCapability capabilities[2] = {
        alarm_system_v1_DeviceCapability_init_zero,
        alarm_system_v1_DeviceCapability_init_zero};

    static const bool initialised = []()
    {
        // Capability 0: Set Switch
        strcpy(capabilities[0].key.key, "set_switch");
        strcpy(capabilities[0].label, "Set Switch");

        strcpy(stateRequirement.key, "State");
        strcpy(stateRequirement.label, "State");

        stateRequirement.which_requirement =
            alarm_system_v1_ParameterRequirement_bool_requirement_tag;

        capabilities[0].parameter_requirements = &stateRequirement;
        capabilities[0].parameter_requirements_count = 1;

        // Capability 1: Toggle
        strcpy(capabilities[1].key.key, "toggle");
        strcpy(capabilities[1].label, "Toggle");

        capabilities[1].parameter_requirements = nullptr;
        capabilities[1].parameter_requirements_count = 0;

        return true;
    }();

    (void)initialised;

    return {capabilities, 2};
}