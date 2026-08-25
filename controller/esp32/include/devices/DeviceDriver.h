#pragma once

#include "alarm.pb.h"

class DeviceDriver {
public:
    virtual bool execute(
        const alarm_system_v1_Action& action
    ) = 0;

    virtual ~DeviceDriver() = default;
};