#pragma once

#include "devices\DeviceDriver.h"

#include <vector>

struct LocalDevice
{
    const char *id;
    DeviceDriver *driver;
};

class LocalDeviceRegistry
{
public:
    void addDevice(const char *id, DeviceDriver &driver);

    const std::vector<alarm_system_v1_Device> &getDevices() const;

    bool execute(const alarm_system_v1_Action &action);
    DeviceDriver *findDevice(const char *deviceId);

private:
    std::vector<LocalDevice> devices;
};