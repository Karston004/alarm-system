#pragma once

#include <pb.h>

enum class HttpMethod
{
    GET,
    POST,
    PUT,
    DELETE
};

enum class HttpResult
{
    SUCCESS,
    CONNECTION_FAILED,
    TIMEOUT,
    HTTP_ERROR,
    ENCODE_FAILED,
    DECODE_FAILED
};

class HttpClient
{
public:
    HttpClient(const char *serverURL, int port);
    bool initialise();

    HttpResult request(
        HttpMethod method,
        const char *endpoint,

        const pb_msgdesc_t *requestType = nullptr,
        const void *request = nullptr,

        const pb_msgdesc_t *responseType = nullptr,
        void *response = nullptr);

private:
    const char *serverURL;
    int port;
};