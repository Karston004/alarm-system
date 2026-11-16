#pragma once

#include <vector>
#include <functional>
#include <shared_mutex>
#include <mutex>

#include "alarm.pb.h"
#include "persistence/LocalRepo.h"

class AlarmManager {
    public:
    
    AlarmManager(LocalRepo& repo);
    
    using AlarmCallback = std::function<void(
        const std::vector<alarm_system_v1_Alarm>&
    )>;

    void withAlarms(AlarmCallback func);
    void setAlarms(std::vector<alarm_system_v1_Alarm> &&newAlarms);

    using AlarmsUpdatedCallback = std::function<void(
        const std::vector<alarm_system_v1_Alarm>&
    )>;
    
    void setSetAlarmsCallback(AlarmsUpdatedCallback callback);
    

    private:
    std::vector<alarm_system_v1_Alarm> alarms;
    std::shared_mutex alarmsMutex;
    LocalRepo* localRepo = nullptr;
    AlarmsUpdatedCallback callback;

};