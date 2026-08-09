package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.Device;
import com.karstonn.alarmsystem.proto.DeviceId;
import com.karstonn.alarmsystem.proto.DeviceListRequest;
import com.karstonn.alarmsystem.proto.DeviceListResponse;
import com.karstonn.alarmsystem.proto.DeviceRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateDeviceRequest;

import java.util.concurrent.CompletableFuture;

public interface DeviceRepo {
    Device getDevice(DeviceId id);
    CompletableFuture<DeviceRequestResponse> addDevice(Device device);
    CompletableFuture<DeviceRequestResponse> updateDevice(UpdateDeviceRequest updateRequest);
    CompletableFuture<DeviceRequestResponse> removeDevice(DeviceId id);
    CompletableFuture<DeviceListResponse> listDevices(DeviceListRequest listRequest);
}
