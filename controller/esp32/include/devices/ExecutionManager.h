#pragma once

#include "devices/LocalDeviceRegistry.h"
#include "alarm.pb.h"

class ExecutionManager
{
public:
    explicit ExecutionManager(
        LocalDeviceRegistry &deviceReg);

    void executePhase(
        const alarm_system_v1_AlarmPhase &phase);

private:
    LocalDeviceRegistry &localDeviceRegistry;

    static void actionTask(void *parameter);
};
