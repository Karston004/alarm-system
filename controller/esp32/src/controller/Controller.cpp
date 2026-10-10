#include "controller/Controller.h"
#include "utils/UUID.h"
#include <Arduino.h>

Controller::Controller(
    HttpClient &httpClient,
    MqttClient &mqttClient,
    LocalDeviceRegistry &localDeviceRegistry,
    std::string label)
    : httpClient(httpClient),
      mqttClient(mqttClient),
      localDeviceRegistry(localDeviceRegistry),
      label(std::move(label)),
      controllerApiClient(httpClient),
      controllerMqttClient(mqttClient),
      executionManager(localDeviceRegistry)
{
}

void Controller::initialise()
{
  ensureControllerId();
  localRepo.setAlarmsUpdatedCallback([this]()
                                     { onAlarmsUpdated(); });

  scheduler.setTriggerCallback([this](const alarm_system_v1_AlarmPhase &phase)
                               { onPhaseTrigger(phase); });

  // Active: search
  httpClient.initialise();
  trySync();

  // Passive: wait (once subscribed)
  // Auto connect and reconnect
  mqttClient.initialise();
  trySetupMqttClient();

  scheduler.setEvents(localRepo.getAlarms());
}

// ======================================================
// UUID confirmation
void Controller::ensureControllerId()
{
  if (!localRepo.getController().has_controller_id)
  {
    alarm_system_v1_Controller controller =
        alarm_system_v1_Controller_init_zero;

    snprintf(
        controller.label,
        sizeof(controller.label),
        "%s",
        label.c_str());

    std::string uuid = generateUuid();

    snprintf(
        controller.controller_id.id,
        sizeof(controller.controller_id.id),
        "%s",
        uuid.c_str());

    controller.has_controller_id = true;
    localRepo.saveController(
        controller);
  }
}

// ======================================================
// Sever requests

// (Re)register with sever
void Controller::trySync()
{
  if (sync())
  {
    tryGetAlarms();
    return;
  }

  RetryTask::start(
      [this]()
      {
        if (!sync())
        {
          return false;
        }
        else
        {
          tryGetAlarms();
          return true;
        }
      },
      {.initialDelayMs = 5000,
       .maxDelayMs = 30 * 60 * 1000,
       .multiplier = 2.0f},
      "SyncControllerRetry");
}

bool Controller::sync()
{
  auto result = controllerApiClient.syncController(
      localRepo.getController(),
      localDeviceRegistry.getDevices());

  if (result != ControllerApiClient::Result::SUCCESS)
    return false;
  else
  {
    return true;
  }
}

// Get Alarms
void Controller::tryGetAlarms()
{
  // Only one thread attempting to get alarms at a time
  bool expected = false;
  if (!getAlarmsRunning.compare_exchange_strong(expected, true))
    return;

  if (getAlarms())
  {
    getAlarmsRunning = false;
    return;
  }

  if (!RetryTask::start(
          [this]()
          {
            bool success = getAlarms();

            if (success)
              getAlarmsRunning = false;

            return success;
          },
          {.initialDelayMs = 5000,
           .maxDelayMs = 30 * 60 * 1000,
           .multiplier = 2.0f},
          "GetAlarmsRetry"))
  {
    Serial.print("ERR: - Failed to start getAlarms retry");
    getAlarmsRunning = false;
  }
}

bool Controller::getAlarms()
{
  std::vector<alarm_system_v1_Alarm> newAlarms;

  auto result = controllerApiClient.getAlarms(
      localRepo.getController().controller_id,
      newAlarms);

  if (result != ControllerApiClient::Result::SUCCESS)
    return false;
  else
  {
    localRepo.saveAlarms(std::move(newAlarms));
    return true;
  }
}

// ======================================================
// MQTT connection

// Lib allready implements retry behaviour
void Controller::trySetupMqttClient()
{
  controllerMqttClient.initialise([this]()
                                  { onMqttPing(); });
}

// ======================================================
// Event handlers

void Controller::onAlarmsUpdated()
{
  scheduler.setEvents(localRepo.getAlarms());
}
void Controller::onMqttPing()
{
  tryGetAlarms();
}
void Controller::onPhaseTrigger(
    const alarm_system_v1_AlarmPhase &phase)
{
  executionManager.executePhase(phase);
}