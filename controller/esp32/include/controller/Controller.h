#pragma once

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <string>
#include <mutex>

#include "alarm.pb.h"

#include "network/MqttClient.h"
#include "network/HttpClient.h"

#include "scheduling/Scheduler.h"

#include "persistence/LocalRepo.h"

#include "devices/LocalDeviceRegistry.h"
#include "devices/DeviceDriver.h"
#include "devices/ExecutionManager.h"

#include "server/ControllerApiClient.h"
#include "server/ControllerMqttClient.h"
#include "concurrency/RetryTask.h"

class Controller
{
public:
    Controller(
        HttpClient &httpClient,
        MqttClient &mqttClient,
        LocalDeviceRegistry &localDeviceRegistry,
        std::string label);

    void initialise();

private:
    HttpClient &httpClient;
    MqttClient &mqttClient;
    LocalDeviceRegistry &localDeviceRegistry;
    std::string label;

    LocalRepo localRepo;
    ExecutionManager executionManager;

    ControllerApiClient controllerApiClient;
    ControllerMqttClient controllerMqttClient;
    Scheduler scheduler;

    std::atomic_bool getAlarmsRunning{false};

    void ensureControllerId();

    void trySync();
    bool sync();

    void tryGetAlarms();
    bool getAlarms();

    void trySetupMqttClient();

    void onAlarmsUpdated();
    void onMqttPing();
    void onPhaseTrigger(const alarm_system_v1_AlarmPhase &phase);
};