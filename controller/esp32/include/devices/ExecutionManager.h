#pragma once

#include "devices/LocalDeviceRegistry.h"

class ExecutionManager
{
public:
    ExecutionManager(LocalDeviceRegistry &deviceReg);

    void executePhase(
        const alarm_system_v1_AlarmPhase &phase);

private:
    LocalDeviceRegistry &localDeviceRegistry;
};