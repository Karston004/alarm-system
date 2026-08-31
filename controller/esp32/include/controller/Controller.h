#pragma once

#include "alarm.pb.h"

#include "network/MqttClient.h"
#include "network/HttpClient.h"

#include "scheduling/Scheduler.h"

#include "persistence/LocalRepo.h"

#include "devices/LocalDeviceRegistry.h"
#include "devices/DeviceDriver.h"

#include "server/ControllerApiClient.h"

class Controller
{
public:
    Controller(
        HttpClient &httpClient,
        MqttClient &mqttClient,
        LocalDeviceRegistry &localDeviceRegistry);

    bool initialise();

private:
    HttpClient &httpClient;
    MqttClient &mqttClient;
    LocalDeviceRegistry &localDeviceRegistry;
    std::vector<alarm_system_v1_Alarm> alarms;
    LocalRepo localRepo;
    ControllerApiClient controllerApiClient;
    Scheduler scheduler;
};