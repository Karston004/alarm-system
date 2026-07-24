package com.karstonn.alarm.ui.scheduleEdit;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.function.Consumer;


public class ScheduleEditViewModel extends ViewModel {
    private Alarm.Builder draftAlarm;
    private AlarmRepo repo;
    private boolean hasUnsavedChanges;

    public void setAlarmRepo(
            @NonNull AlarmRepo repo
    ) {
        this.repo = repo;
    }
    public void loadAlarm(Alarm alarm) {
        if (alarm == null){
            throw new IllegalArgumentException("Alarm cannot be null");
        }
        draftAlarm = alarm.toBuilder();
        hasUnsavedChanges = false;
    }

    public void updateAlarm(Consumer<Alarm.Builder> update){
        requireDraft();
        update.accept(draftAlarm);
        hasUnsavedChanges = true;
    }

    public Alarm getDraftAlarm() {
        requireDraft();
        return draftAlarm.build();
    }

    public void saveAlarm(){
        requireDraft();
        requireRepo();
        if (hasUnsavedChanges){
          repo.updateAlarm(UpdateAlarmRequest.newBuilder()
                  .setId(draftAlarm.getId())
                  .setAlarm(draftAlarm).build());
          hasUnsavedChanges = false;
        }
    }

    public void deleteAlarm(){
        requireDraft();
        requireRepo();
        repo.removeAlarm(draftAlarm.getId());
    }

    public boolean hasDraftAlarm(){
        return draftAlarm != null;
    }

    public void markSaved() {
        hasUnsavedChanges = false;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    private void requireDraft (){
        if (draftAlarm == null) {
            throw new IllegalStateException("No alarm has been loaded");
        }
    }
    private void requireRepo (){
        if (repo == null) {
            throw new IllegalStateException("No repo has been loaded for ScheduleEditVm");
        }
    }
}