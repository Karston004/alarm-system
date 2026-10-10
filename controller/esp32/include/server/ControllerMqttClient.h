#pragma once

#include <functional>

#include "network/MqttClient.h"

class ControllerMqttClient
{
public:
    ControllerMqttClient(MqttClient &mqttClient);

    using EventCallback = std::function<void()>;

    void initialise(EventCallback callback);

private:
    MqttClient &mqttClient;
    EventCallback eventCallback;

    void pingHandler(
        const std::string &topic,
        const std::string &payload);
};