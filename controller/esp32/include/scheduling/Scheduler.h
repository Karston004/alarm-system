#pragma once

#include <vector>
#include <functional>

#include "alarm.pb.h"

class Scheduler
{
public:
    using TriggerCallback =
        std::function<void(const alarm_system_v1_AlarmPhase &)>;

    void setEvents(
        const std::vector<alarm_system_v1_Alarm> &alarms);

    void setTriggerCallback(TriggerCallback callback);

private:
    // Non-owning pointers to phases held by LocalRepo
    std::vector<const alarm_system_v1_AlarmPhase *> phases;

    TriggerCallback callback;
};