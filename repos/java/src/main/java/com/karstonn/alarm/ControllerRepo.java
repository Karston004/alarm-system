package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.AddControllerRequest;
import com.karstonn.alarmsystem.proto.AddControllerResponse;
import com.karstonn.alarmsystem.proto.GetControllerRequest;
import com.karstonn.alarmsystem.proto.GetControllerResponse;
import com.karstonn.alarmsystem.proto.ListControllersRequest;
import com.karstonn.alarmsystem.proto.ListControllersResponse;
import com.karstonn.alarmsystem.proto.RemoveControllerRequest;
import com.karstonn.alarmsystem.proto.RemoveControllerResponse;
import com.karstonn.alarmsystem.proto.RequestIDRequest;
import com.karstonn.alarmsystem.proto.RequestIDResponse;
import com.karstonn.alarmsystem.proto.UpdateControllerResponse;

import java.util.concurrent.CompletableFuture;

public interface ControllerRepo {
    CompletableFuture<AddControllerResponse> addController(AddControllerRequest addControllerRequest);
    CompletableFuture<RemoveControllerResponse> removeController(RemoveControllerRequest removeControllerRequest);
    CompletableFuture<UpdateControllerResponse> updateController(UpdateControllerResponse updateControllerResponse);
    CompletableFuture<GetControllerResponse> getController(GetControllerRequest getControllerRequest);
    CompletableFuture<ListControllersResponse> listControllers(ListControllersRequest listControllersRequest);
    CompletableFuture<RequestIDResponse> requestID(RequestIDRequest requestIDRequest);
}
