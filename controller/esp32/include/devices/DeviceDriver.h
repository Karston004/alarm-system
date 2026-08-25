#pragma once

#include <cstddef>
#include "alarm.pb.h"

struct DeviceCapabilityView {
    const alarm_system_v1_DeviceCapability* data;
    std::size_t count;
};

class DeviceDriver {
public:
    virtual bool execute(
        const alarm_system_v1_Action& action
    ) = 0;

    virtual DeviceCapabilityView getCapabilities() const = 0;

    virtual ~DeviceDriver() = default;
};