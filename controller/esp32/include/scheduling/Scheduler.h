#pragma once

#include <vector>

class Scheduler
{
public:
    void setEvents(std::vector<alarm_system_v1_Alarm> alarms);

    void setTriggerCallback(
        std::function<void(const alarm_system_v1_AlarmPhase)> callback);

private:
    std::vector<alarm_system_v1_AlarmPhase> phases;
    std::function<void(const alarm_system_v1_Action &)> callback;
};