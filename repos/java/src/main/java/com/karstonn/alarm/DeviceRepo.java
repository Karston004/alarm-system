package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.Device;
import com.karstonn.alarmsystem.proto.DeviceId;
import com.karstonn.alarmsystem.proto.DeviceListRequest;
import com.karstonn.alarmsystem.proto.DeviceListResponse;
import com.karstonn.alarmsystem.proto.DeviceRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateDeviceRequest;

public interface DeviceRepo {
    Device getDevice(DeviceId id);
    DeviceRequestResponse addDevice(Device device);
    DeviceRequestResponse updateDevice(UpdateDeviceRequest updateRequest);
    DeviceRequestResponse removeDevice(DeviceId id);
    DeviceListResponse listDevices(DeviceListRequest listRequest);
}
