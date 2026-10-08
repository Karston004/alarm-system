#pragma once

#include <vector>
#include <string>
#include <mutex>

#include "alarm.pb.h"

class LocalRepo
{
public:
    const std::vector<alarm_system_v1_Alarm> &getAlarms();
    void saveAlarms(std::vector<alarm_system_v1_Alarm> &&alarms);

    using AlarmsUpdatedCallback =
        std::function<void()>;

    void setAlarmsUpdatedCallback(
        AlarmsUpdatedCallback callback);

    alarm_system_v1_Controller getController();
    void saveController(alarm_system_v1_Controller);

private:
    std::mutex alarmsMutex;

    std::vector<alarm_system_v1_Alarm> alarms;
    bool alarmsLoaded = false;

    AlarmsUpdatedCallback alarmsUpdatedCallback;

    std::vector<alarm_system_v1_Alarm> loadAlarmsFromStorage();
    void saveAlarmsToStorage(const std::vector<alarm_system_v1_Alarm> &alarms);
};