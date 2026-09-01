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

    void pingHandler(const char *topic, const uint8_t *payload, size_t length);
};