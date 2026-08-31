#pragma once

#include <vector>
#include <string>

#include "alarm.pb.h"

class LocalRepo
{
public:
    std::vector<alarm_system_v1_Alarm> loadAlarms();
    bool saveAlarms(std::vector<alarm_system_v1_Alarm> alarms);

    alarm_system_v1_Controller getController();
    bool saveController(alarm_system_v1_Controller);

private:
};