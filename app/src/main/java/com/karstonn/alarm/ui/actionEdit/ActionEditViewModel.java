package com.karstonn.alarm.ui.actionEdit;

import androidx.lifecycle.ViewModel;

import com.karstonn.alarm.DeviceRepo;
import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.ActionParameter;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmPhase;
import com.karstonn.alarmsystem.proto.Device;
import com.karstonn.alarmsystem.proto.DeviceListRequest;
import com.karstonn.alarmsystem.proto.ParameterRequirement;

import java.util.List;
import java.util.function.Consumer;

public class ActionEditViewModel extends ViewModel {
    private Action.Builder draftAction;
    private DeviceRepo deviceRepo;
    private boolean hasUnsavedChanges;

    public void setDeviceRepo(DeviceRepo repo) {
        deviceRepo = repo;
    }
    public List<Device> fetchDevices(){
        requireRepo();
        return deviceRepo.listDevices(DeviceListRequest.newBuilder().build()).getDevicesList();
    }
    public void loadAction(Action action) {
        draftAction = action.toBuilder();
        hasUnsavedChanges = false;
    }

    public void updateAction(Consumer<Action.Builder> update) {
        requireDraft();
        update.accept(draftAction);
        hasUnsavedChanges = true;
    }

    public Action getDraftAction() {
        requireDraft();
        return draftAction.build();
    }

    public void markSaved() {
        requireDraft();
        hasUnsavedChanges = false;
    }

    public int findParameterIndexByKey(String parameterKey) {
        for (int i = 0; i< draftAction.getParametersCount(); i++) {
            ActionParameter parameter = draftAction.getParameters(i);

            if (parameter.getParameterKey().equals(parameterKey)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "No parameter found with key: " + parameterKey
        );
    }

    public boolean hasDraftAction() {
        return draftAction != null;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    private void requireDraft() {
        if (draftAction == null) {
            throw new IllegalStateException("No action has been loaded");
        }
    }

    private void requireRepo(){
        if (deviceRepo == null)
            throw new IllegalStateException("No device repo has been loaded");
    }
}