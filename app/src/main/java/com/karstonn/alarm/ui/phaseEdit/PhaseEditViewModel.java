package com.karstonn.alarm.ui.phaseEdit;

import androidx.lifecycle.ViewModel;

import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.AlarmPhase;

import java.util.function.Consumer;

public class PhaseEditViewModel extends ViewModel {
    private boolean hasUnsavedChanges;
    private AlarmPhase.Builder draftPhase;

    public void loadPhase(AlarmPhase phase) {
        draftPhase = phase.toBuilder();
    }

    public void updatePhase(Consumer<AlarmPhase.Builder> update) {
        requireDraft();
        update.accept(draftPhase);
        hasUnsavedChanges = true;
    }
    public AlarmPhase getDraftPhase() {
        requireDraft();
        return draftPhase.build();
    }

    public boolean hasDraftPhase() {
        return draftPhase != null;
    }

    public void markSaved() {
        requireDraft();
        hasUnsavedChanges = false;
    }

    private void requireDraft(){
        if (draftPhase == null) {
            throw new IllegalStateException("No phase has been loaded");
        }
    }
}