#pragma once

#include "devices/LocalDeviceRegistry.h"

class ExecutionManager
{
public:
    ExecutionManager(LocalDeviceRegistry &localDeviceRegistry);
    void executePhase(alarm_system_v1_AlarmPhase phase);

private:
    LocalDeviceRegistry &localDeviceRegistry;
};