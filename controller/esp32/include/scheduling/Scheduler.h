#pragma once

#include <vector>
#include <functional>
#include <ctime>
#include <Arduino.h>

#include "alarm.pb.h"
#include "alarms/AlarmManager.h"



class Scheduler
{
public:
    void start();

    using TriggerCallback =
        std::function<void(const alarm_system_v1_AlarmPhase &)>;

    void setEvents(
        const std::vector<alarm_system_v1_Alarm> &alarms);

    void setTriggerCallback(TriggerCallback callback);
    void setAlarmManger(AlarmManager& alarmManager);
    void run();

private:

    static constexpr time_t MISSED_ALARM_CUTOFF_SECONDS = 5 * 60;

    struct ScheduledEvent
    {
        const alarm_system_v1_Alarm* alarm;
        const alarm_system_v1_AlarmPhase* phase;
        time_t nextTrigger;
    };

    std::vector<ScheduledEvent> events;
    std::vector<ScheduledEvent> unpackAlarms(const std::vector<alarm_system_v1_Alarm> &alarms);
    void refreshEvents(time_t referenceTime);
    void calculateNextTriggers(time_t referenceTime);
    time_t calculateNextTrigger(time_t now, const alarm_system_v1_DayOfWeek * days, pb_size_t dayCount, const alarm_system_v1_AlarmPhase * phase);
    void sortEvents(std::vector<ScheduledEvent>& events);

    TaskHandle_t schedulerTaskHandle = nullptr;
    static void taskEntry(void* parameter);
    void run();
    void wake();

    TriggerCallback callback;
    AlarmManager* alarmManager = nullptr;

};