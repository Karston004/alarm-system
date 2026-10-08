#include <esp_system.h>
#include <string>
#include <cstdio>

std::string generateUuid()
{
    uint8_t bytes[16];

    for (int i = 0; i < 16; i += 4)
    {
        uint32_t randomValue = esp_random();

        bytes[i] = randomValue;
        bytes[i + 1] = randomValue >> 8;
        bytes[i + 2] = randomValue >> 16;
        bytes[i + 3] = randomValue >> 24;
    }

    // UUID v4
    bytes[6] = (bytes[6] & 0x0F) | 0x40;

    // RFC 4122 variant
    bytes[8] = (bytes[8] & 0x3F) | 0x80;

    char uuid[37];

    snprintf(
        uuid,
        sizeof(uuid),
        "%02x%02x%02x%02x-"
        "%02x%02x-"
        "%02x%02x-"
        "%02x%02x-"
        "%02x%02x%02x%02x%02x%02x",
        bytes[0], bytes[1], bytes[2], bytes[3],
        bytes[4], bytes[5],
        bytes[6], bytes[7],
        bytes[8], bytes[9],
        bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]);

    return std::string(uuid);
}