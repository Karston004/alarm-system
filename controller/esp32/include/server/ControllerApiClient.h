#pragma once

#include <vector>

#include "alarm.pb.h"
#include "network/HttpClient.h"

class ControllerApiClient
{
public:
    ControllerApiClient(HttpClient &httpClient);

    enum class Result
    {
        SUCCESS,
        RETRYABLE_ERROR,
        NON_RETRYABLE_ERROR
    };

    Result syncController(
        const alarm_system_v1_Controller &controller,
        const std::vector<alarm_system_v1_Device> &devices);

    Result getAlarms(
        const alarm_system_v1_ControllerId &controllerId,
        std::vector<alarm_system_v1_Alarm> &alarms);

private:
    HttpClient &httpClient;
};