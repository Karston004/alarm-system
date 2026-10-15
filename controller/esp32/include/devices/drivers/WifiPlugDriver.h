#pragma once

#include <Arduino.h>
#include <WiFi.h>
#include <vector>

#include "devices/DeviceDriver.h"

class WifiPlugDriver final : public DeviceDriver
{
public:
    WifiPlugDriver(const IPAddress &ip, const String &pass);

    bool execute(const alarm_system_v1_Action &action) override;
    DeviceCapabilityView getCapabilities() const override;

private:
    IPAddress ipAddress;
    String password;
};