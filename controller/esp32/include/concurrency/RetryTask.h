#pragma once

#include <functional>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

class RetryTask
{
public:
    struct Config
    {
        uint32_t initialDelayMs;
        uint32_t maxDelayMs;
        float multiplier;
    };

    using Attempt = std::function<bool()>;

    static bool start(
        Attempt attempt,
        Config config,
        const char *taskName = "RetryTask");

private:
    struct Context
    {
        Attempt attempt;
        Config config;
    };

    static void taskEntry(void *parameter);
};