#include "persistence/LocalRepo.h"

#include <LittleFS.h>
#include <pb_encode.h>
#include <pb_decode.h>

// TODO: Clean up mem management! - mem leak
// TODO: Reference Invalidation
// TODO: Ownership semantics

// TODO: Controller loading/Saving
const std::vector<alarm_system_v1_Alarm> &LocalRepo::getAlarms()
{
    // Scoped Lock
    std::lock_guard<std::mutex> lock(alarmsMutex);

    if (!alarmsLoaded)
    {
        alarms = loadAlarmsFromStorage();
        alarmsLoaded = true;
    }

    return alarms;
}

void LocalRepo::saveAlarms(
    std::vector<alarm_system_v1_Alarm> &newAlarms)
{
    // Scoped Lock
    {
        std::lock_guard<std::mutex> lock(alarmsMutex);

        alarms = std::move(newAlarms);
        alarmsLoaded = true;

        saveAlarmsToStorage(alarms);
    }

}

namespace
{
    constexpr const char *ALARMS_FILE = "/alarms.bin";

    static bool writeToFile(
        pb_ostream_t *stream,
        const pb_byte_t *buf,
        size_t count)
    {
        File *file =
            static_cast<File *>(stream->state);

        return file->write(buf, count) == count;
    }
}

std::vector<alarm_system_v1_Alarm> LocalRepo::loadAlarmsFromStorage()
{
    std::vector<alarm_system_v1_Alarm> loadedAlarms;

    File file = LittleFS.open(ALARMS_FILE, "r");

    if (!file)
    {
        // No saved alarms yet
        return loadedAlarms;
    }

    // --------------------------------------------------
    // Read alarm count
    // --------------------------------------------------

    uint32_t alarmCount = 0;

    if (file.read(
            reinterpret_cast<uint8_t *>(&alarmCount),
            sizeof(alarmCount)) != sizeof(alarmCount))
    {
        file.close();
        return {};
    }

    // --------------------------------------------------
    // Read alarms
    // --------------------------------------------------

    for (uint32_t i = 0; i < alarmCount; ++i)
    {
        uint32_t messageSize = 0;

        // Read protobuf message length
        if (file.read(
                reinterpret_cast<uint8_t *>(&messageSize),
                sizeof(messageSize)) != sizeof(messageSize))
        {
            file.close();
            return {};
        }

        // File is corrupt if it claims the message is
        // larger than the remaining data.
        if (messageSize > file.available())
        {
            file.close();
            return {};
        }

        std::vector<pb_byte_t> buffer(messageSize);

        if (file.read(
                buffer.data(),
                messageSize) != messageSize)
        {
            file.close();
            return {};
        }

        alarm_system_v1_Alarm alarm =
            alarm_system_v1_Alarm_init_zero;

        pb_istream_t stream =
            pb_istream_from_buffer(
                buffer.data(),
                buffer.size());

        if (!pb_decode(
                &stream,
                alarm_system_v1_Alarm_fields,
                &alarm))
        {
            file.close();
            return {};
        }

        loadedAlarms.push_back(std::move(alarm));
    }

    file.close();

    return loadedAlarms;
}

void LocalRepo::saveAlarmsToStorage(
    const std::vector<alarm_system_v1_Alarm> &alarms)
{
    File file = LittleFS.open(ALARMS_FILE, "w");

    if (!file)
    {
        // TODO: logging
        return;
    }

    // Store number of alarms first
    uint32_t alarmCount = static_cast<uint32_t>(alarms.size());

    if (file.write(
            reinterpret_cast<const uint8_t *>(&alarmCount),
            sizeof(alarmCount)) != sizeof(alarmCount))
    {
        file.close();
        return;
    }

    for (const auto &alarm : alarms)
    {
        // First determine encoded protobuf size
        size_t encodedSize = 0;

        if (!pb_get_encoded_size(
                &encodedSize,
                alarm_system_v1_Alarm_fields,
                &alarm))
        {
            file.close();
            return;
        }

        uint32_t messageSize =
            static_cast<uint32_t>(encodedSize);

        // Store message length
        if (file.write(
                reinterpret_cast<const uint8_t *>(&messageSize),
                sizeof(messageSize)) != sizeof(messageSize))
        {
            file.close();
            return;
        }

        // Encode directly into the file
        pb_ostream_t stream = {
            writeToFile,
            &file,
            encodedSize,
            0};

        if (!pb_encode(
                &stream,
                alarm_system_v1_Alarm_fields,
                &alarm))
        {
            file.close();
            return;
        }
    }

    file.close();
}