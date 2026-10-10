#include "network/MqttClient.h"

#include <algorithm>
#include <utility>

#include <esp_log.h>

namespace
{
    constexpr const char *TAG = "MqttClient";
}

MqttClient::MqttClient(MqttConfig config)
    : config(std::move(config))
{
}

MqttClient::~MqttClient()
{
    if (client)
    {
        esp_mqtt_client_stop(client);
        esp_mqtt_client_destroy(client);
    }
}

bool MqttClient::initialise()
{
    if (client)
        return true;

    if (config.broker.empty() ||
        config.port == 0 ||
        config.rootCa.empty())
        return false;

    esp_mqtt_client_config_t mqttConfig = {};

    mqttConfig.broker.address.hostname =
        config.broker.c_str();

    mqttConfig.broker.address.port = config.port;

    mqttConfig.broker.address.transport =
        MQTT_TRANSPORT_OVER_SSL;

    mqttConfig.broker.verification.certificate =
        config.rootCa.c_str();

    mqttConfig.credentials.client_id =
        config.clientId.empty()
            ? nullptr
            : config.clientId.c_str();

    mqttConfig.credentials.username =
        config.username.empty()
            ? nullptr
            : config.username.c_str();

    mqttConfig.credentials.authentication.password =
        config.password.empty()
            ? nullptr
            : config.password.c_str();

    // ESP-IDF reconnects automatically.
    mqttConfig.network.disable_auto_reconnect = false;

    client = esp_mqtt_client_init(&mqttConfig);

    if (!client)
        return false;

    esp_err_t result = esp_mqtt_client_register_event(
        client,
        MQTT_EVENT_ANY,
        &MqttClient::eventHandler,
        this);

    if (result == ESP_OK)
        result = esp_mqtt_client_start(client);

    if (result != ESP_OK)
    {
        esp_mqtt_client_destroy(client);
        client = nullptr;
        return false;
    }

    return true;
}

bool MqttClient::subscribe(const std::string &topic)
{
    if (topic.empty())
        return false;

    {
        std::lock_guard<std::mutex> lock(mutex);

        if (std::find(
                subscriptions.begin(),
                subscriptions.end(),
                topic) != subscriptions.end())
            return true;

        subscriptions.push_back(topic);
    }

    // If disconnected, the subscription is remembered
    // and will be requested on the next connection.
    if (!connected.load())
        return true;

    return client &&
           esp_mqtt_client_subscribe(
               client, topic.c_str(), 0) >= 0;
}

bool MqttClient::publish(
    const std::string &topic,
    const std::string &message)
{
    if (topic.empty() || !client || !connected.load())
        return false;

    // Queue message without waiting for network delivery.
    int result = esp_mqtt_client_enqueue(
        client,
        topic.c_str(),
        message.data(),
        static_cast<int>(message.size()),
        0,     // QoS 0
        0,     // Not retained
        true); // Queue QoS 0 message

    return result >= 0;
}

void MqttClient::setMessageCallback(
    MessageCallback callback)
{
    std::lock_guard<std::mutex> lock(mutex);
    messageCallback = std::move(callback);
}

bool MqttClient::isConnected() const
{
    return connected.load();
}

void MqttClient::eventHandler(
    void *args,
    esp_event_base_t base,
    int32_t eventId,
    void *eventData)
{
    (void)base;
    (void)eventId;

    auto *self = static_cast<MqttClient *>(args);
    auto *event =
        static_cast<esp_mqtt_event_handle_t>(eventData);

    if (self && event)
        self->handleEvent(event);
}

void MqttClient::handleEvent(
    esp_mqtt_event_handle_t event)
{
    switch (event->event_id)
    {
    case MQTT_EVENT_CONNECTED:
    {
        connected.store(true);
        ESP_LOGI(TAG, "Connected");

        std::vector<std::string> topics;

        {
            std::lock_guard<std::mutex> lock(mutex);
            topics = subscriptions;
        }

        // Restore subscriptions on every connection.
        for (const auto &topic : topics)
        {
            if (esp_mqtt_client_subscribe(
                    client, topic.c_str(), 0) < 0)
            {
                ESP_LOGW(TAG,
                         "Subscription request failed: %s",
                         topic.c_str());
            }
        }

        break;
    }

    case MQTT_EVENT_DISCONNECTED:
        connected.store(false);
        ESP_LOGW(TAG, "Disconnected");
        break;

    case MQTT_EVENT_DATA:
    {
        // This simple wrapper handles complete messages.
        // Fragmented messages are deliberately discarded.
        if (event->current_data_offset != 0 ||
            event->data_len != event->total_data_len)
        {
            ESP_LOGW(TAG, "Fragmented message ignored");
            break;
        }

        if (!event->topic || event->topic_len <= 0 ||
            event->data_len < 0 ||
            (event->data_len > 0 && !event->data))
            break;

        MessageCallback callback;

        {
            std::lock_guard<std::mutex> lock(mutex);
            callback = messageCallback;
        }

        if (callback)
        {
            std::string topic(
                event->topic, event->topic_len);

            std::string payload;

            if (event->data_len > 0)
                payload.assign(
                    event->data, event->data_len);

            callback(topic, payload);
        }

        break;
    }

    case MQTT_EVENT_ERROR:
        ESP_LOGE(TAG, "MQTT error");
        break;

    default:
        break;
    }
}