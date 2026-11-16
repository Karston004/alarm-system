#include "scheduling/Scheduler.h"
#include <algorithm>

// TODO: deal with non-recuring alarms

void Scheduler::setEvents(const std::vector<alarm_system_v1_Alarm> &alarms)
{
    events = unpackAlarms(alarms);
    time_t now = time(nullptr);
    refreshEvents(now);

    xTaskNotifyGive(schedulerTaskHandle); // Wake
}

std::vector<Scheduler::ScheduledEvent> Scheduler::unpackAlarms(const std::vector<alarm_system_v1_Alarm> &alarms)
{
    std::vector<ScheduledEvent> unpacked;

    for (const auto &alarm : alarms)
    {
        if (alarm.is_enabled)
        {
            for (pb_size_t i = 0; i < alarm.alarm_phases_count; i++)
            {
                unpacked.push_back({&alarm,
                                    &alarm.alarm_phases[i],
                                    -1});
            }
        }
    }

    return unpacked;
}

void Scheduler::refreshEvents(time_t referenceTime)
{
    // Recalculate nextTrigger for each event
    calculateNextTriggers(referenceTime);

    // Sort events
    sortEvents(events);
}

void Scheduler::calculateNextTriggers(time_t referenceTime)
{
    for (auto &event : events)
    {
        event.nextTrigger = calculateNextTrigger(
            referenceTime,
            event.alarm->days,
            event.alarm->days_count,
            event.phase);
    }
}

time_t calculateNextTrigger(time_t now, const alarm_system_v1_DayOfWeek *days, pb_size_t dayCount, const alarm_system_v1_AlarmPhase *phase)
{
    if (dayCount == 0 || days == nullptr || phase == nullptr)
        return -1;

    // Get current local date and weekday
    tm currentDate;
    if (localtime_r(&now, &currentDate) == nullptr)
        return -1;

    int currentDay = currentDate.tm_wday;

    // Construct today's trigger time
    tm todayTriggerDate = currentDate;

    todayTriggerDate.tm_hour = phase->trigger_time.hour;
    todayTriggerDate.tm_min = phase->trigger_time.min;
    todayTriggerDate.tm_sec = 0;
    todayTriggerDate.tm_isdst = -1;

    time_t todayTrigger = mktime(&todayTriggerDate);

    // Find closest valid day
    int closestOffset = 8;

    for (pb_size_t i = 0; i < dayCount; i++)
    {
        if (days[i] == alarm_system_v1_DayOfWeek_DAY_UNSPECIFIED)
            continue;

        int dayOffset = (days[i] - currentDay + 7) % 7;

        // If today's trigger has passed, use next week
        if (dayOffset == 0 && todayTrigger <= now)
            dayOffset += 7;

        if (dayOffset < closestOffset)
            closestOffset = dayOffset;
    }

    // No valid days
    if (closestOffset == 8)
        return -1;

    // Construct final trigger date
    tm nextDate = currentDate;

    nextDate.tm_mday += closestOffset;
    nextDate.tm_hour = phase->trigger_time.hour;
    nextDate.tm_min = phase->trigger_time.min;
    nextDate.tm_sec = 0;
    nextDate.tm_isdst = -1;

    return mktime(&nextDate);
}

void Scheduler::sortEvents(std::vector<ScheduledEvent> &events)
{
    std::sort(events.begin(), events.end(),
              [](const ScheduledEvent &a, const ScheduledEvent &b)
              {
                  return a.nextTrigger < b.nextTrigger;
              });
}

void Scheduler::setAlarmManger(AlarmManager &manager)
{
    alarmManager = &manager;
}

void Scheduler::start()
{
    if (schedulerTaskHandle != nullptr)
        return;

    xTaskCreate(
        taskEntry,
        "Scheduler",
        4096,    // Initial stack allocation in bytes
        this,    // Pass our Scheduler instance
        1,       // Task priority
        &schedulerTaskHandle
    );
}

void Scheduler::taskEntry(void* parameter)
{
    Scheduler* scheduler = static_cast<Scheduler*>(parameter);
    scheduler->run();
}

void Scheduler::wake()
{
    if (schedulerTaskHandle != nullptr)
        xTaskNotifyGive(schedulerTaskHandle);
}

void Scheduler::run()
{
    while (true)
    {

        TickType_t waitTicks = pdMS_TO_TICKS(portMAX_DELAY);


        // Read lock
        alarmManager->withAlarms([&](const auto& alarms)
        {
            time_t now = time(nullptr);
            int stepper = 0;
            while (stepper < events.size() && events.at(stepper).nextTrigger <= now){
                if (now - events.at(stepper).nextTrigger <= MISSED_ALARM_CUTOFF_SECONDS)
                    callback(*events.at(stepper).phase);
                stepper ++;
            }

            refreshEvents(now);

            // Calculate next wake-up duration
            if (!events.empty()) {
                int64_t seconds = events.front().nextTrigger - time(nullptr);

                waitTicks = seconds <= 0 ? 0 : static_cast<TickType_t>(std::min<uint64_t>(uint64_t(seconds) * configTICK_RATE_HZ, portMAX_DELAY - 1ULL));
            }

        });
        // Lock has been released


        ulTaskNotifyTake(pdTRUE, waitTicks);

    }
}
