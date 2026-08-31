#pragma once

/*
<functional> is additional Overhead compared to normal embedded system options
But ESP32 is large and the function will be used rarely
And I like its simplification
*/
#include <functional>

class MqttClient
{
public:
    MqttClient(const char *broker, int port);
    bool initalise();

    enum class MqttResult
    {
        SUCCESS,
        CONNECTION_FAILED,
        SUBSCRIBE_FAILED,
        PUBLISH_FAILED
    };

    MqttResult subscribe(const char *topic);
    MqttResult publish(const char *topic, const char *message);

    using MessageCallback = std::function<void(
        const char *topic,
        const uint8_t *payload,
        size_t length)>;
    void setMessageCallback(MessageCallback callback);

private:
    MessageCallback messageCallback;

    const char *broker;
    int port;
};