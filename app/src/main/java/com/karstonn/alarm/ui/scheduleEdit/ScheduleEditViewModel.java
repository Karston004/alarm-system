package com.karstonn.alarm.ui.scheduleEdit;

import androidx.lifecycle.ViewModel;

import com.karstonn.alarmsystem.proto.Alarm;

import java.sql.Struct;
import java.util.function.Consumer;


public class ScheduleEditViewModel extends ViewModel {
    private Alarm.Builder draftAlarm;
    private boolean hasUnsavedChanges;

    public void loadAlarm(Alarm alarm) {
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
            throw new IllegalStateException("No phase has been loaded");
        }
    }
}