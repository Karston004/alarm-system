package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.BoolRequirement;
import com.karstonn.alarmsystem.proto.Device;
import com.karstonn.alarmsystem.proto.DeviceCapability;
import com.karstonn.alarmsystem.proto.DeviceCapabilityKey;
import com.karstonn.alarmsystem.proto.DeviceId;
import com.karstonn.alarmsystem.proto.DeviceListRequest;
import com.karstonn.alarmsystem.proto.DeviceListResponse;
import com.karstonn.alarmsystem.proto.DeviceRequestResponse;
import com.karstonn.alarmsystem.proto.DoubleRequirement;
import com.karstonn.alarmsystem.proto.INT32Requirement;
import com.karstonn.alarmsystem.proto.ParameterRequirement;
import com.karstonn.alarmsystem.proto.PercentageRequirement;
import com.karstonn.alarmsystem.proto.RGBARequirement;
import com.karstonn.alarmsystem.proto.StringRequirement;
import com.karstonn.alarmsystem.proto.UINT32Requirement;
import com.karstonn.alarmsystem.proto.UpdateDeviceRequest;

import java.util.HashMap;
import java.util.Map;

public class DebugDeviceRepo implements DeviceRepo{
    private final Map<String, Device> devices = new HashMap<>();
    public DebugDeviceRepo () {
        populateDebugDevices();
    }
    private void populateDebugDevices(){
        devices.put("Debug Device: Numbers", createDebugNumberDevice());
        devices.put("Debug Device: General",    createDebugTextDevice());
    }
    @Override
    public Device getDevice(DeviceId id) {
        return devices.get(id.getId());
    }

    @Override
    public DeviceRequestResponse addDevice(Device device) {
        devices.put(device.getDeviceId().getId(), device);
        return DeviceRequestResponse.newBuilder().setSuccess(true).build();
    }

    @Override
    public DeviceRequestResponse updateDevice(UpdateDeviceRequest updateRequest) {
        if (devices.containsKey(updateRequest.getDevice().getDeviceId().getId())){
            devices.put(updateRequest.getDevice().getDeviceId().getId(), updateRequest.getDevice());
            return DeviceRequestResponse.newBuilder().setSuccess(true).build();
        }
        return DeviceRequestResponse.newBuilder().setSuccess(false).build();
    }

    @Override
    public DeviceRequestResponse removeDevice(DeviceId id) {
        if (devices.containsKey(id.getId())){
            devices.remove(id.getId());
            return DeviceRequestResponse.newBuilder().setSuccess(true).build();
        }
        return DeviceRequestResponse.newBuilder().setSuccess(false).build();
    }

    @Override
    public DeviceListResponse listDevices(DeviceListRequest listRequest) {
        return DeviceListResponse.newBuilder().addAllDevices(devices.values()).build();
    }
    private Device createDebugNumberDevice(){
        return Device.newBuilder()
                .setDeviceId(
                        DeviceId.newBuilder()
                                .setId("Debug Device: Numbers")
                                .build()
                )
                .setLabel("Debug Device: Numbers")
                .addCapabilities(
                        DeviceCapability.newBuilder()
                                .setLabel("Debug Example")
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Constrained Int32")
                                                .setKey("0")
                                                .setInt32Requirement(
                                                        INT32Requirement.newBuilder()
                                                                .setMaxVal(500)
                                                                .setMinVal(-500)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("UnConstrained Int32")
                                                .setKey("1")
                                                .setInt32Requirement(
                                                        INT32Requirement.newBuilder()
                                                                .setMinVal(Integer.MIN_VALUE)
                                                                .setMaxVal(Integer.MAX_VALUE)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Constrained Uint32")
                                                .setKey("2")
                                                .setUint32Requirement(
                                                        UINT32Requirement.newBuilder()
                                                                .setMaxVal(500)
                                                                .setMinVal(10)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("UnConstrained Uint32")
                                                .setKey("3")
                                                .setUint32Requirement(
                                                        UINT32Requirement.newBuilder()
                                                                .setMaxVal(Integer.MAX_VALUE)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Constrained Double")
                                                .setKey("4")
                                                .setDoubleRequirement(
                                                        DoubleRequirement.newBuilder()
                                                                .setMaxVal(250.47)
                                                                .setMinVal(-100.47)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("UnConstrained Double")
                                                .setKey("5")
                                                .setDoubleRequirement(
                                                        DoubleRequirement.newBuilder()
                                                                .setMinVal(-Double.MAX_VALUE)
                                                                .setMaxVal(Double.MAX_VALUE)
                                                                .build())
                                                .build()
                                )
                                .build()
                ).build();
    }
    private Device createDebugTextDevice(){
        return Device.newBuilder()
                .setDeviceId(
                        DeviceId.newBuilder()
                                .setId("Debug Device: General")
                                .build()
                )
                .setLabel("Debug Device: General")
                .addCapabilities(
                        DeviceCapability.newBuilder()
                                .setLabel("Example Text")
                                .setKey(DeviceCapabilityKey.newBuilder().setKey("0"))
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Constrained Text")
                                                .setKey("0")
                                                .setStringRequirement(
                                                        StringRequirement.newBuilder()
                                                                .setMaxLength(12)
                                                                .setMinLength(5)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("UnConstrained Text")
                                                .setKey("1")
                                                .setStringRequirement(
                                                        StringRequirement.newBuilder()
                                                                .setMinLength(0)
                                                                .setMaxLength(Integer.MAX_VALUE)
                                                                .build())
                                                .build()
                                )
                                .build()
                )
                .addCapabilities(
                        DeviceCapability.newBuilder()
                                .setLabel("General")
                                .setKey(DeviceCapabilityKey.newBuilder().setKey("1"))
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Example 1.1")
                                                .setKey("2")
                                                .setStringRequirement(
                                                        StringRequirement.newBuilder()
                                                                .setMaxLength(500)
                                                                .setMinLength(5)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Example 2.1")
                                                .setKey("3")
                                                .setStringRequirement(
                                                        StringRequirement.newBuilder()
                                                                .setMinLength(0)
                                                                .setMaxLength(Integer.MAX_VALUE)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Bool Toggle")
                                                .setKey("0")
                                                .setBoolRequirement(
                                                        BoolRequirement.newBuilder()
                                                                .build()
                                                )
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Percentage")
                                                .setKey("1")
                                                .setPercentageRequirement(
                                                        PercentageRequirement.newBuilder()
                                                                .setMinVal(0)
                                                                .setMaxVal(100)
                                                                .setStep(1)
                                                                .build())
                                                .build()
                                )
                                .addParameterRequirements(
                                        ParameterRequirement.newBuilder()
                                                .setLabel("Colour Picker")
                                                .setKey("5")
                                                .setRgbaRequirement(RGBARequirement.newBuilder().build())
                                                .build()
                                )
                                .build()
                ).build();
    }
}
