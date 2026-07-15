package com.karstonn.alarm.ui.actionEdit;

import androidx.lifecycle.ViewModel;
import com.karstonn.alarmsystem.proto.Action;
import java.util.function.Consumer;

public class ActionEditViewModel extends ViewModel {
    private Action.Builder draftAction;
    private boolean hasUnsavedChanges;

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
}