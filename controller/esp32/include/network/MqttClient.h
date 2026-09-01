#pragma once

#include <functional>
#include <string>

#include <ESP32MQTTClient.h>

struct MqttConfig
{
    const char *broker;
    uint16_t port;

    const char *username;
    const char *password;

    const char *clientId;

    const char *rootCa;
};

class MqttClient
{
public:
    enum class MqttResult
    {
        SUCCESS,
        CONNECTION_FAILED,
        SUBSCRIBE_FAILED,
        PUBLISH_FAILED
    };

    using MessageCallback = std::function<void(
        const char *topic,
        const uint8_t *payload,
        size_t length)>;

    explicit MqttClient(const MqttConfig &config);

    void initialise();

    MqttResult subscribe(const char *topic);

    MqttResult publish(
        const char *topic,
        const char *message);

    void setMessageCallback(MessageCallback callback);

    bool isConnected();

private:
    MqttConfig config;

    ESP32MQTTClient mqttClient;

    MessageCallback messageCallback;
};