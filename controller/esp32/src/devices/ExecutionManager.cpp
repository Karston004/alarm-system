#include "devices/ExecutionManager.h"
#include "utils/protobuff/OwnedAction.h"

#include <Arduino.h>
#include <memory>

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

namespace
{
    constexpr uint32_t ACTION_TASK_STACK_SIZE = 8192;
    constexpr UBaseType_t ACTION_TASK_PRIORITY = 1;

    struct ActionTaskContext
    {
        LocalDeviceRegistry &registry;
        OwnedAction action;

        ActionTaskContext(
            LocalDeviceRegistry &registry,
            const alarm_system_v1_Action &source)
            : registry(registry),
              action(source)
        {
        }
    };

    // TODO: File contents are not deep-copied yet.
    bool containsFileParameter(
        const alarm_system_v1_Action &action)
    {
        for (pb_size_t i = 0; i < action.parameters_count; ++i)
        {
            const auto &param = action.parameters[i];

            if (param.has_value &&
                param.value.which_value ==
                    alarm_system_v1_ActionValue_file_tag)
            {
                return true;
            }
        }

        return false;
    }
}

ExecutionManager::ExecutionManager(
    LocalDeviceRegistry &deviceReg)
    : localDeviceRegistry(deviceReg)
{
}

void ExecutionManager::executePhase(
    const alarm_system_v1_AlarmPhase &phase)
{
    for (pb_size_t i = 0; i < phase.actions_count; ++i)
    {
        const alarm_system_v1_Action &source = phase.actions[i];

        // Allocate task context and independently copy action.
        auto context = std::make_unique<ActionTaskContext>(
            localDeviceRegistry, source);

        // Transfer ownership before creating the task.
        // The task may begin executing immediately.
        ActionTaskContext *raw = context.release();

        BaseType_t result = xTaskCreate(
            actionTask,
            "ActionExec",
            ACTION_TASK_STACK_SIZE,
            raw,
            ACTION_TASK_PRIORITY,
            nullptr);

        if (result != pdPASS){
            // if task not created, clean up context
            delete raw;

            Serial.println(
                "ExecutionManager: Failed to create task");
        }
    }
}

void ExecutionManager::actionTask(void *parameter)
{
    {
        // Task takes ownership of its context.
        std::unique_ptr<ActionTaskContext> context(static_cast<ActionTaskContext *>(parameter));

        // Execute using the independent action copy.
        if (context->registry.execute(context->action.get()) == false){
            Serial.printf("ExecutionManager: Action - %s - Failed\n", context->action.get().label);
        };
        // context is destroyed at the end of this scope.
    }
    vTaskDelete(nullptr);
}
