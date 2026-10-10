#include "concurrency/RetryTask.h"
#include <esp_random.h>

int32_t addJitter(int32_t val);

bool RetryTask::start(
    Attempt attempt,
    Config config,
    const char *taskName)
{
    auto *context = new Context{
        std::move(attempt),
        config};

    BaseType_t result = xTaskCreate(
        taskEntry,
        taskName,
        4096,
        context,
        1,
        nullptr);

    if (result != pdPASS)
    {
        delete context;
        return false;
    }

    return true;
}

void RetryTask::taskEntry(void *parameter)
{
    auto *context = static_cast<Context *>(parameter);

    uint32_t delayMs = context->config.initialDelayMs;
    bool taskSuccess = false;

    while (taskSuccess == false)
    {
        vTaskDelay(pdMS_TO_TICKS(addJitter(delayMs)));

        // If task completed (reutrns true)
        if (context->attempt())
            taskSuccess = true;
        // Else, increase backoff
        else
        {
            delayMs = std::min(
                static_cast<uint32_t>(
                    delayMs * context->config.multiplier),
                context->config.maxDelayMs);
        }
    }

    delete context;

    vTaskDelete(nullptr);
}

uint32_t addJitter(uint32_t val)
{
    uint32_t jitter = val / 5; // 20%

    return (val - jitter + (esp_random() % (jitter * 2 + 1)));
}