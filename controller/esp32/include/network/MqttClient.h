#pragma once

#include <atomic>
#include <cstdint>
#include <functional>
#include <mutex>
#include <string>
#include <vector>

#include <mqtt_client.h>

struct MqttConfig
{
    std::string broker;
    uint16_t port = 8883;

    std::string username;
    std::string password;
    std::string clientId;
    std::string rootCa;
};

class MqttClient
{
public:
    using MessageCallback = std::function<void(
        const std::string &topic,
        const std::string &payload)>;

    explicit MqttClient(MqttConfig config);
    ~MqttClient();

    MqttClient(const MqttClient &) = delete;
    MqttClient &operator=(const MqttClient &) = delete;

    bool initialise();

    // Remember topics and restore them after reconnection.
    bool subscribe(const std::string &topic);

    bool publish(
        const std::string &topic,
        const std::string &message);

    void setMessageCallback(MessageCallback callback);

    bool isConnected() const;

private:
    static void eventHandler(
        void *args,
        esp_event_base_t base,
        int32_t eventId,
        void *eventData);

    void handleEvent(esp_mqtt_event_handle_t event);

    MqttConfig config;
    esp_mqtt_client_handle_t client = nullptr;

    std::atomic<bool> connected{false};

    std::mutex mutex;
    std::vector<std::string> subscriptions;
    MessageCallback messageCallback;
};