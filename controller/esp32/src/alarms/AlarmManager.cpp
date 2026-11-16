#include "alarms/AlarmManager.h"

    AlarmManager::AlarmManager(LocalRepo& repo){
        localRepo = &repo;
    }

    void AlarmManager::withAlarms(AlarmCallback func) {
        std::shared_lock lock(alarmsMutex);
        func(alarms);
    }

    void AlarmManager::setAlarms(
        std::vector<alarm_system_v1_Alarm>&& newAlarms)
    {
        std::unique_lock lock(alarmsMutex);

        // Transfer ownership to AlarmManager
        alarms = std::move(newAlarms);

        // Persist without transferring ownership
        localRepo->saveAlarms(alarms);

        // Run callbacks
        if (callback){
            callback(alarms);
        }
    }

    void AlarmManager::setSetAlarmsCallback(
    AlarmsUpdatedCallback newCallback)
    {
        callback = std::move(newCallback);
    }